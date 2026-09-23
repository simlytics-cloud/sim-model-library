package iso.sim.coordinator.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import iso.sim.coordinator.helper.dto.CoordinatorEventReport;
import iso.sim.coordinator.helper.dto.CreateCoordinatedRunRequest;
import iso.sim.coordinator.helper.dto.ErrorResponse;
import iso.sim.coordinator.helper.dto.RegisterRemoteRunnerRequest;
import iso.sim.coordinator.helper.dto.RemoteRunnerEventReport;
import org.apache.pekko.http.javadsl.model.ContentTypes;
import org.apache.pekko.http.javadsl.model.HttpEntities;
import org.apache.pekko.http.javadsl.model.HttpResponse;
import org.apache.pekko.http.javadsl.model.StatusCode;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.server.AllDirectives;
import org.apache.pekko.http.javadsl.server.PathMatchers;
import org.apache.pekko.http.javadsl.server.Route;
import org.apache.pekko.http.javadsl.unmarshalling.Unmarshaller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class CoordinatorHelperRoutes extends AllDirectives {
    public static final String CALLBACK_TOKEN_HEADER = "X-Coordinator-Helper-Token";

    private final CoordinatedRunService coordinatedRunService;
    private final ObjectMapper objectMapper;

    public CoordinatorHelperRoutes(CoordinatedRunService coordinatedRunService) {
        this(coordinatedRunService, new ObjectMapper());
    }

    public CoordinatorHelperRoutes(CoordinatedRunService coordinatedRunService, ObjectMapper objectMapper) {
        this.coordinatedRunService = coordinatedRunService;
        this.objectMapper = objectMapper;
    }

    public Route routes() {
        return concat(
            path("openapi.yaml", () -> get(() -> complete(HttpEntities.create(
                ContentTypes.TEXT_PLAIN_UTF8, loadResource("open-api/coordinator-helper-open-api-spec.yaml")
            )))),
            pathPrefix("v1", () -> pathPrefix("coordinated-runs", () -> concat(
            pathEndOrSingleSlash(() -> post(() -> entity(Unmarshaller.entityToString(), body -> {
                try {
                    return complete(HttpResponse.create()
                        .withStatus(StatusCodes.CREATED)
                        .withEntity(ContentTypes.APPLICATION_JSON, toJson(
                            coordinatedRunService.createRun(objectMapper.readValue(body, CreateCoordinatedRunRequest.class))
                        )));
                } catch (CoordinatedRunConflictException ex) {
                    return completeError(StatusCodes.CONFLICT, "RUN_ID_COLLISION", ex.getMessage());
                } catch (CoordinatedRunRequestException | IOException ex) {
                    return completeError(StatusCodes.BAD_REQUEST, "INVALID_COORDINATED_RUN_REQUEST", ex.getMessage());
                }
            }))),
            path(PathMatchers.segment(), runId -> concat(
                get(() -> {
                    try {
                        return completeJson(coordinatedRunService.getRun(runId));
                    } catch (CoordinatedRunNotFoundException ex) {
                        return completeError(StatusCodes.NOT_FOUND, "COORDINATED_RUN_NOT_FOUND", ex.getMessage());
                    }
                }),
                delete(() -> {
                    try {
                        return completeJson(coordinatedRunService.cancelRun(runId));
                    } catch (CoordinatedRunNotFoundException ex) {
                        return completeError(StatusCodes.NOT_FOUND, "COORDINATED_RUN_NOT_FOUND", ex.getMessage());
                    }
                })
            )),
            path(PathMatchers.segment().slash("remote-runners"), runId -> post(() ->
                entity(Unmarshaller.entityToString(), body -> {
                    try {
                        return completeJson(coordinatedRunService.registerRemoteRunner(
                            runId, objectMapper.readValue(body, RegisterRemoteRunnerRequest.class)
                        ));
                    } catch (CoordinatedRunNotFoundException ex) {
                        return completeError(StatusCodes.NOT_FOUND, "COORDINATED_RUN_NOT_FOUND", ex.getMessage());
                    } catch (CoordinatedRunConflictException ex) {
                        return completeError(StatusCodes.CONFLICT, "COORDINATED_RUN_CONFLICT", ex.getMessage());
                    } catch (CoordinatedRunRequestException | IOException ex) {
                        return completeError(StatusCodes.BAD_REQUEST, "INVALID_REMOTE_RUNNER_REQUEST", ex.getMessage());
                    }
                })
            )),
            path(PathMatchers.segment().slash("remote-runners").slash(PathMatchers.segment()).slash("events"),
                (runId, modelInstanceId) -> post(() -> headerValueByName(CALLBACK_TOKEN_HEADER, token ->
                    entity(Unmarshaller.entityToString(), body -> {
                        try {
                            return completeJson(coordinatedRunService.reportRemoteRunnerEvent(
                                runId, modelInstanceId, token, objectMapper.readValue(body, RemoteRunnerEventReport.class)
                            ));
                        } catch (CoordinatorAuthorizationException ex) {
                            return completeError(StatusCodes.FORBIDDEN, "COORDINATOR_HELPER_UNAUTHORIZED", ex.getMessage());
                        } catch (CoordinatedRunNotFoundException ex) {
                            return completeError(StatusCodes.NOT_FOUND, "COORDINATED_RUN_NOT_FOUND", ex.getMessage());
                        } catch (CoordinatedRunConflictException ex) {
                            return completeError(StatusCodes.CONFLICT, "COORDINATED_RUN_CONFLICT", ex.getMessage());
                        } catch (CoordinatedRunRequestException | IOException ex) {
                            return completeError(StatusCodes.BAD_REQUEST, "INVALID_REMOTE_RUNNER_EVENT", ex.getMessage());
                        }
                    })
                ))
            ),
            path(PathMatchers.segment().slash("start"), runId -> post(() -> {
                try {
                    return completeJson(coordinatedRunService.startRun(runId));
                } catch (CoordinatedRunNotFoundException ex) {
                    return completeError(StatusCodes.NOT_FOUND, "COORDINATED_RUN_NOT_FOUND", ex.getMessage());
                } catch (CoordinatedRunConflictException ex) {
                    return completeError(StatusCodes.CONFLICT, "COORDINATED_RUN_CONFLICT", ex.getMessage());
                }
            })),
            path(PathMatchers.segment().slash("coordinator").slash("events"), runId -> post(() ->
                headerValueByName(CALLBACK_TOKEN_HEADER, token -> entity(Unmarshaller.entityToString(), body -> {
                    try {
                        return completeJson(coordinatedRunService.reportCoordinatorEvent(
                            runId, token, objectMapper.readValue(body, CoordinatorEventReport.class)
                        ));
                    } catch (CoordinatorAuthorizationException ex) {
                        return completeError(StatusCodes.FORBIDDEN, "COORDINATOR_HELPER_UNAUTHORIZED", ex.getMessage());
                    } catch (CoordinatedRunNotFoundException ex) {
                        return completeError(StatusCodes.NOT_FOUND, "COORDINATED_RUN_NOT_FOUND", ex.getMessage());
                    } catch (CoordinatedRunConflictException ex) {
                        return completeError(StatusCodes.CONFLICT, "COORDINATED_RUN_CONFLICT", ex.getMessage());
                    } catch (CoordinatedRunRequestException | IOException ex) {
                        return completeError(StatusCodes.BAD_REQUEST, "INVALID_COORDINATOR_EVENT", ex.getMessage());
                    }
                }))
            ))
            ))));
    }

    private Route completeError(StatusCode statusCode, String code, String message) {
        return complete(HttpResponse.create()
            .withStatus(statusCode)
            .withEntity(ContentTypes.APPLICATION_JSON, toJson(new ErrorResponse(code, message))));
    }

    private Route completeJson(Object payload) {
        return complete(HttpEntities.create(ContentTypes.APPLICATION_JSON, toJson(payload)));
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to serialize response", ex);
        }
    }

    private String loadResource(String path) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalStateException("Required resource '" + path + "' was not found");
        }
        try (InputStream input = stream) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read resource '" + path + "'", ex);
        }
    }
}
