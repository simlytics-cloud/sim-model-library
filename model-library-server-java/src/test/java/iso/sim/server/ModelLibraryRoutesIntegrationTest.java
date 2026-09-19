package iso.sim.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.ConfigFactory;
import iso.sim.server.catalog.CatalogRepository;
import iso.sim.server.catalog.ModelCatalogService;
import iso.sim.server.dto.ErrorResponse;
import iso.sim.server.dto.catalog.ModelListResponse;
import iso.sim.server.dto.catalog.ModelDto;
import iso.sim.server.dto.run.CurrentSimulationTimeDto;
import iso.sim.server.dto.run.KafkaDefaultsResponse;
import iso.sim.server.dto.run.RunStatusResponse;
import iso.sim.server.dto.run.StartModelRunResponse;
import iso.sim.server.dto.run.TimeMode;
import iso.sim.server.service.DefaultRunReadinessProbeSelector;
import iso.sim.server.executor.StubRunExecutor;
import iso.sim.server.runtime.RunResourceRegistry;
import iso.sim.server.service.RunService;
import iso.sim.server.store.InMemoryRunStatusStore;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.http.javadsl.Http;
import org.apache.pekko.http.javadsl.ServerBinding;
import org.apache.pekko.http.javadsl.server.Route;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelLibraryRoutesIntegrationTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ActorSystem actorSystem = ActorSystem.create("model-library-java-routes-test");
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @AfterEach
    void tearDown() {
        actorSystem.terminate();
    }

    @Test
    void routesPreserveRunAndErrorApiContract() throws Exception {
        ModelCatalogService service = new ModelCatalogService(
            new CatalogRepository(
                ConfigFactory.load().getStringList("model.library.catalog.resources"),
                objectMapper
            )
        );
        RunService runService = new RunService(
            service,
            new StubRunExecutor(),
            new InMemoryRunStatusStore(),
            new RunResourceRegistry(),
            new DefaultRunReadinessProbeSelector(),
            (context, handle) -> handle
        );
        Route route = new ModelLibraryRoutes(
            service,
            runService,
            new KafkaDefaultsResponse(
                "localhost:9092",
                "devs-sim",
                "PLAINTEXT",
                ""
            ),
            objectMapper
        ).routes();
        ServerBinding binding = bind(route);

        try {
            String baseUrl = "http://127.0.0.1:" + binding.localAddress().getPort();

            HttpResponse<String> listModelsResponse = send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/models"))
                .GET()
                .build());
            assertEquals(200, listModelsResponse.statusCode());
            ModelListResponse modelList = objectMapper.readValue(listModelsResponse.body(), ModelListResponse.class);
            assertTrue(modelList.getModels().stream().anyMatch(model -> "irpsystem.irpmodel.Vehicle".equals(model.getModelId())));

            HttpResponse<String> getModelResponse = send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/models/irpsystem.irpmodel.Vehicle"))
                .GET()
                .build());
            assertEquals(200, getModelResponse.statusCode());
            ModelDto model = objectMapper.readValue(getModelResponse.body(), ModelDto.class);
            assertEquals("Vehicle", model.getName());
            assertEquals(TimeMode.SCALED_REAL_TIME, model.getTimeMode().getMode());
            assertEquals(2.0, model.getTimeMode().getRealTimeFactor());

            String runRequestBody = loadResource("irpsystem.irpmodel.Vehicle.put-run-request.json");
            HttpResponse<String> startRunResponse = send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/models/irpsystem.irpmodel.Vehicle/run"))
                .PUT(HttpRequest.BodyPublishers.ofString(runRequestBody))
                .header("Content-Type", "application/json")
                .build());
            assertEquals(202, startRunResponse.statusCode());
            StartModelRunResponse runResponse = objectMapper.readValue(startRunResponse.body(), StartModelRunResponse.class);
            assertTrue(runResponse.getStatusUrl().startsWith("/v1/runs/"));
            assertEquals(runResponse.getStatusUrl(), startRunResponse.headers().firstValue("Location").orElseThrow());

            HttpResponse<String> statusResponse = send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + runResponse.getStatusUrl()))
                .GET()
                .build());
            assertEquals(200, statusResponse.statusCode());
            RunStatusResponse parsedStatus = objectMapper.readValue(statusResponse.body(), RunStatusResponse.class);
            assertEquals(runResponse.getRunId(), parsedStatus.getRunId());
            assertEquals("ready", parsedStatus.getStatus());

            runService.getRunStatusStore().save(new RunStatusResponse(
                parsedStatus.getRunId(),
                parsedStatus.getModelId(),
                parsedStatus.getStatus(),
                parsedStatus.getAcceptedAt(),
                parsedStatus.getReadyAt(),
                parsedStatus.getStartedAt(),
                parsedStatus.getCompletedAt(),
                parsedStatus.getMessage(),
                new CurrentSimulationTimeDto(
                    new BigDecimal("125.0"),
                    "NextInternalTimeReport",
                    "message-123",
                    "2026-07-01T12:34:56Z"
                )
            ));

            HttpResponse<String> statusWithTimeResponse = send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + runResponse.getStatusUrl()))
                .GET()
                .build());
            assertEquals(200, statusWithTimeResponse.statusCode());
            JsonNode statusWithTime = objectMapper.readTree(statusWithTimeResponse.body());
            assertTrue(statusWithTime.path("currentSimulationTime").path("value").isNumber());
            assertEquals(new BigDecimal("125.0"), statusWithTime.path("currentSimulationTime").path("value").decimalValue());

            HttpResponse<String> listRunsResponse = send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/runs"))
                .GET()
                .build());
            assertEquals(200, listRunsResponse.statusCode());
            JsonNode runs = objectMapper.readTree(listRunsResponse.body());
            assertTrue(runs.get(0).path("currentSimulationTime").path("value").isNumber());

            HttpResponse<String> cancelRunResponse = send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + runResponse.getStatusUrl()))
                .DELETE()
                .build());
            assertEquals(200, cancelRunResponse.statusCode());
            JsonNode canceledRun = objectMapper.readTree(cancelRunResponse.body());
            assertEquals("canceled", canceledRun.path("status").asText());

            HttpResponse<String> cancelRunAgainResponse = send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + runResponse.getStatusUrl()))
                .DELETE()
                .build());
            assertEquals(200, cancelRunAgainResponse.statusCode());
            JsonNode canceledRunAgain = objectMapper.readTree(cancelRunAgainResponse.body());
            assertEquals("canceled", canceledRunAgain.path("status").asText());

            HttpResponse<String> kafkaDefaultsResponse = send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/run-config/defaults"))
                .GET()
                .build());
            assertEquals(200, kafkaDefaultsResponse.statusCode());
            JsonNode kafkaDefaults = objectMapper.readTree(kafkaDefaultsResponse.body());
            assertEquals("localhost:9092", kafkaDefaults.path("bootstrapServers").asText());
            assertEquals("devs-sim", kafkaDefaults.path("topic").asText());
            assertTrue(kafkaDefaults.path("consumerGroup").isMissingNode());

            HttpResponse<String> missingModelResponse = send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/models/missing.model"))
                .GET()
                .build());
            assertEquals(404, missingModelResponse.statusCode());
            ErrorResponse missingModelError = objectMapper.readValue(missingModelResponse.body(), ErrorResponse.class);
            assertEquals("MODEL_NOT_FOUND", missingModelError.getCode());

            HttpResponse<String> missingRunResponse = send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/runs/missing-run"))
                .GET()
                .build());
            assertEquals(404, missingRunResponse.statusCode());
            ErrorResponse missingRunError = objectMapper.readValue(missingRunResponse.body(), ErrorResponse.class);
            assertEquals("RUN_NOT_FOUND", missingRunError.getCode());

            HttpResponse<String> missingRunDeleteResponse = send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/runs/missing-run"))
                .DELETE()
                .build());
            assertEquals(404, missingRunDeleteResponse.statusCode());
            ErrorResponse missingRunDeleteError = objectMapper.readValue(missingRunDeleteResponse.body(), ErrorResponse.class);
            assertEquals("RUN_NOT_FOUND", missingRunDeleteError.getCode());

            HttpResponse<String> invalidRunRequestResponse = send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/models/irpsystem.irpmodel.Vehicle/run"))
                .PUT(HttpRequest.BodyPublishers.ofString("{}"))
                .header("Content-Type", "application/json")
                .build());
            assertEquals(400, invalidRunRequestResponse.statusCode());
            ErrorResponse invalidRunError = objectMapper.readValue(invalidRunRequestResponse.body(), ErrorResponse.class);
            assertEquals("INVALID_RUN_REQUEST", invalidRunError.getCode());

            HttpResponse<String> openApiResponse = send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/openapi.yaml"))
                .GET()
                .build());
            assertEquals(200, openApiResponse.statusCode());
            assertTrue(openApiResponse.body().contains("openapi:"));

            HttpResponse<String> swaggerResponse = send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/swagger"))
                .GET()
                .build());
            assertEquals(200, swaggerResponse.statusCode());
            assertTrue(swaggerResponse.body().contains("SwaggerUIBundle"));
            assertTrue(swaggerResponse.body().contains("/openapi.yaml"));

            HttpResponse<String> rootResponse = send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/"))
                .GET()
                .build());
            assertEquals(301, rootResponse.statusCode());
            assertEquals("ui/", rootResponse.headers().firstValue("Location").orElseThrow());

            HttpResponse<String> uiResponse = send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/ui"))
                .GET()
                .build());
            assertEquals(200, uiResponse.statusCode());
            assertTrue(uiResponse.headers().firstValue("Content-Type").orElse("").contains("text/html"));

            HttpResponse<String> uiSlashResponse = send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/ui/"))
                .GET()
                .build());
            assertEquals(200, uiSlashResponse.statusCode());
            assertTrue(uiSlashResponse.headers().firstValue("Content-Type").orElse("").contains("text/html"));
        } finally {
            binding.unbind().toCompletableFuture().get(10, TimeUnit.SECONDS);
        }
    }

    private ServerBinding bind(Route route) throws Exception {
        return Http.get(actorSystem)
            .newServerAt("127.0.0.1", 0)
            .bind(route.function(actorSystem))
            .toCompletableFuture()
            .get(10, TimeUnit.SECONDS);
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String loadResource(String path) throws IOException {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalStateException("Fixture not found: " + path);
        }
        try (var is = stream) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
