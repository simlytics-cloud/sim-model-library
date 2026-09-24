package iso.sim.coordinator.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import iso.sim.coordinator.helper.dto.KafkaConfigurationDto;
import iso.sim.coordinator.helper.dto.TimeModeConfigurationDto;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.http.javadsl.Http;
import org.apache.pekko.http.javadsl.ServerBinding;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoordinatorHelperRoutesIntegrationTest {
    private final ActorSystem actorSystem = ActorSystem.create("coordinator-helper-routes-test");
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @AfterEach
    void tearDown() {
        actorSystem.terminate();
    }

    @Test
    void exposesCoordinatedRunRegistrationAndAuthenticatedRemoteRunnerCallbacks() throws Exception {
        CoordinatedRunService service = new CoordinatedRunService(
            new NoopCoordinatorController(),
            new NoopRemoteRunnerClient(),
            "http://127.0.0.1:8091"
        );
        ServerBinding binding = Http.get(actorSystem)
            .newServerAt("127.0.0.1", 0)
            .bind(new CoordinatorHelperRoutes(service).routes().function(actorSystem))
            .toCompletableFuture()
            .get(10, TimeUnit.SECONDS);
        try {
            String baseUrl = "http://127.0.0.1:" + binding.localAddress().getPort();
            HttpResponse<String> openApi = send(HttpRequest.newBuilder(URI.create(baseUrl + "/openapi.yaml")).GET().build());
            assertEquals(200, openApi.statusCode());
            assertTrue(openApi.body().contains("Optional Coordinator Helper API"));

            assertEquals(201, send(HttpRequest.newBuilder(URI.create(baseUrl + "/v1/coordinated-runs"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                    {"runId":"run-route-1","simulationId":"simulation-route-1",
                     "coordinatorId":"coordinator-route-1","coordinatorToken":"coordinator-token"}
                    """))
                .build()).statusCode());

            assertEquals(200, send(HttpRequest.newBuilder(URI.create(baseUrl + "/v1/coordinated-runs/run-route-1/remote-runners"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                    {"modelId":"model-route-1","modelInstanceId":"instance-route-1",
                     "modelLibraryUrl":"http://model-library.example","initializationParameters":{},
                     "kafka":{"topic":"simulation-topic","properties":{"bootstrap.servers":"kafka:9092"}},
                     "timeMode":{"mode":"virtual-time"},"callbackToken":"callback-token"}
                    """))
                .build()).statusCode());

            HttpResponse<String> callback = send(HttpRequest.newBuilder(URI.create(
                baseUrl + "/v1/coordinated-runs/run-route-1/remote-runners/instance-route-1/events"
            ))
                .header("Content-Type", "application/json")
                .header(CoordinatorHelperRoutes.CALLBACK_TOKEN_HEADER, "callback-token")
                .POST(HttpRequest.BodyPublishers.ofString("""
                    {"runId":"run-route-1","modelInstanceId":"instance-route-1","coordinatorId":"coordinator-route-1",
                     "eventId":"accepted-route-1","eventType":"remote-runner-accepted","timestamp":"2026-01-01T00:00:00Z"}
                    """))
                .build());
            assertEquals(200, callback.statusCode());

            assertEquals(403, send(HttpRequest.newBuilder(URI.create(
                baseUrl + "/v1/coordinated-runs/run-route-1/remote-runners/instance-route-1/events"
            ))
                .header("Content-Type", "application/json")
                .header(CoordinatorHelperRoutes.CALLBACK_TOKEN_HEADER, "wrong")
                .POST(HttpRequest.BodyPublishers.ofString("""
                    {"runId":"run-route-1","modelInstanceId":"instance-route-1","coordinatorId":"coordinator-route-1",
                     "eventId":"rejected-route-1","eventType":"remote-runner-ready","timestamp":"2026-01-01T00:00:00Z"}
                    """))
                .build()).statusCode());
        } finally {
            binding.unbind().toCompletableFuture().get(10, TimeUnit.SECONDS);
        }
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static final class NoopRemoteRunnerClient implements RemoteRunnerClient {
        @Override
        public void start(
            String modelLibraryUrl, String modelId, String runId, String simulationId, String modelInstanceId,
            String coordinatorId, com.fasterxml.jackson.databind.JsonNode initializationParameters,
            KafkaConfigurationDto kafka, TimeModeConfigurationDto timeMode, String callbackEndpoint, String callbackToken
        ) {
        }

        @Override
        public void stop(String modelLibraryUrl, String runId) {
        }
    }
}
