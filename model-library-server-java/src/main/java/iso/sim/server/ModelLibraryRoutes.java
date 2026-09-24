/*
 * Sim Model Library Copyright (C) 2026 simlytics.cloud LLC and
 * Sim Model Library contributors.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License
 *  is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing permissions and limitations under
 *  the License.
 *
 */

package iso.sim.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import iso.sim.server.catalog.ModelCatalogService;
import iso.sim.server.catalog.ModelNotFoundException;
import iso.sim.server.dto.ErrorResponse;
import iso.sim.server.dto.run.KafkaDefaultsResponse;
import iso.sim.server.dto.run.StartModelRunRequest;
import iso.sim.server.service.DefaultRunReadinessProbeSelector;
import iso.sim.server.service.InvalidRunRequestException;
import iso.sim.server.service.RunAlreadyExistsException;
import iso.sim.server.service.RunNotFoundException;
import iso.sim.server.service.RunService;
import org.apache.pekko.http.javadsl.model.ContentTypes;
import org.apache.pekko.http.javadsl.model.HttpEntities;
import org.apache.pekko.http.javadsl.model.HttpHeader;
import org.apache.pekko.http.javadsl.model.HttpResponse;
import org.apache.pekko.http.javadsl.model.StatusCode;
import org.apache.pekko.http.javadsl.model.StatusCodes;
import org.apache.pekko.http.javadsl.model.Uri;
import org.apache.pekko.http.javadsl.server.AllDirectives;
import org.apache.pekko.http.javadsl.server.PathMatchers;
import org.apache.pekko.http.javadsl.server.Route;
import org.apache.pekko.http.javadsl.unmarshalling.Unmarshaller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ModelLibraryRoutes extends AllDirectives {
    private final ModelCatalogService service;
    private final RunService runService;
    private final KafkaDefaultsResponse kafkaDefaults;
    private final ObjectMapper objectMapper;

    public ModelLibraryRoutes(ModelCatalogService service) {
        this(
            service,
            new RunService(service, new DefaultRunReadinessProbeSelector()),
            new KafkaDefaultsResponse(
                "devs-sim",
                Map.of("bootstrap.servers", "localhost:9092", "security.protocol", "PLAINTEXT")
            ),
            new ObjectMapper()
        );
    }

    public ModelLibraryRoutes(ModelCatalogService service, RunService runService) {
        this(
            service,
            runService,
            new KafkaDefaultsResponse(
                "devs-sim",
                Map.of("bootstrap.servers", "localhost:9092", "security.protocol", "PLAINTEXT")
            ),
            new ObjectMapper()
        );
    }

    public ModelLibraryRoutes(ModelCatalogService service, RunService runService, KafkaDefaultsResponse kafkaDefaults) {
        this(service, runService, kafkaDefaults, new ObjectMapper());
    }

    public ModelLibraryRoutes(ModelCatalogService service, RunService runService, KafkaDefaultsResponse kafkaDefaults, ObjectMapper objectMapper) {
        this.service = service;
        this.runService = runService;
        this.kafkaDefaults = kafkaDefaults;
        this.objectMapper = objectMapper;
    }

    public Route routes() {
        return concat(
            path("openapi.yaml", () ->
                get(() -> complete(HttpEntities.create(ContentTypes.TEXT_PLAIN_UTF8, loadResource("open-api/model-libary-open-api-spec.yaml"))))
            ),
            path("swagger", () ->
                get(() -> complete(HttpEntities.create(ContentTypes.TEXT_HTML_UTF8, """
                    <!doctype html>
                    <html lang=\"en\">
                      <head>
                        <meta charset=\"utf-8\" />
                        <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />
                        <title>Model Library Swagger UI</title>
                        <link rel=\"stylesheet\" href=\"https://unpkg.com/swagger-ui-dist@5/swagger-ui.css\" />
                      </head>
                      <body>
                        <div id=\"swagger-ui\"></div>
                        <script src=\"https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js\"></script>
                        <script src=\"https://unpkg.com/swagger-ui-dist@5/swagger-ui-standalone-preset.js\"></script>
                        <script>
                          window.ui = SwaggerUIBundle({
                            url: '/openapi.yaml',
                            dom_id: '#swagger-ui',
                            deepLinking: true,
                            presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
                            layout: 'BaseLayout'
                          });
                        </script>
                      </body>
                    </html>
                    """)))
            ),
            pathPrefix("v1", () -> concat(
                path("models", () -> get(() -> completeJson(service.listModels()))),
                path(PathMatchers.segment("models").slash(PathMatchers.segment()), modelId ->
                    get(() -> {
                        try {
                            return completeJson(service.getModel(modelId));
                        } catch (ModelNotFoundException ignored) {
                            return complete(HttpResponse.create()
                                .withStatus(StatusCodes.NOT_FOUND)
                                .withEntity(ContentTypes.APPLICATION_JSON,
                                    toJson(new ErrorResponse("MODEL_NOT_FOUND", "Model '" + modelId + "' was not found"))));
                        }
                    })
                ),
                path(PathMatchers.segment("models").slash(PathMatchers.segment()).slash("run"), modelId ->
                    put(() -> entity(Unmarshaller.entityToString(), requestBody -> {
                        try {
                            StartModelRunRequest request = objectMapper.readValue(requestBody, StartModelRunRequest.class);
                            var response = runService.startRun(modelId, request);
                            return complete(HttpResponse.create()
                                .withStatus(StatusCodes.ACCEPTED)
                                .addHeader(HttpHeader.parse("Location", response.getStatusUrl()))
                                .withEntity(ContentTypes.APPLICATION_JSON, objectMapper.writeValueAsString(response)));
                        } catch (ModelNotFoundException ignored) {
                            return completeError(StatusCodes.NOT_FOUND, "MODEL_NOT_FOUND", "Model '" + modelId + "' was not found");
                        } catch (InvalidRunRequestException ex) {
                            return completeError(StatusCodes.BAD_REQUEST, "INVALID_RUN_REQUEST", ex.getMessage() == null ? "Invalid run request" : ex.getMessage());
                        } catch (RunAlreadyExistsException ex) {
                            return completeError(StatusCodes.CONFLICT, "RUN_ID_COLLISION", ex.getMessage());
                        } catch (IOException ex) {
                            return completeError(StatusCodes.BAD_REQUEST, "INVALID_RUN_REQUEST", "Invalid run request");
                        }
                    }))
                ),
                path(PathMatchers.segment("run-config").slash("defaults"), () ->
                    get(() -> completeJson(kafkaDefaults))
                ),
                path("runs", () -> get(() -> completeJson(runService.listRuns()))),
                path(PathMatchers.segment("runs").slash(PathMatchers.segment()), runId ->
                    concat(
                        get(() -> {
                            try {
                                return completeJson(runService.getRunStatus(runId));
                            } catch (RunNotFoundException ignored) {
                                return completeError(StatusCodes.NOT_FOUND, "RUN_NOT_FOUND", "Run '" + runId + "' was not found");
                            }
                        }),
                        delete(() -> {
                            try {
                                return completeJson(runService.cancelRun(runId));
                            } catch (RunNotFoundException ignored) {
                                return completeError(StatusCodes.NOT_FOUND, "RUN_NOT_FOUND", "Run '" + runId + "' was not found");
                            }
                        })
                    )
                )
            )),
            pathPrefix("ui", () -> concat(
                pathEndOrSingleSlash(() -> getFromResource("model-library-ui/index.html")),
                getFromResourceDirectory("model-library-ui")
            )),
            pathSingleSlash(() -> redirect(Uri.create("ui/"), StatusCodes.MOVED_PERMANENTLY))
        );
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
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize response", e);
        }
    }

    private String loadResource(String path) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalStateException("Required resource '" + path + "' was not found");
        }
        try (InputStream is = stream) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource '" + path + "'", e);
        }
    }
}
