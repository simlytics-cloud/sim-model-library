package iso.sim.coordinator.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import iso.sim.coordinator.helper.dto.KafkaConfigurationDto;
import iso.sim.coordinator.helper.dto.TimeModeConfigurationDto;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP-only orchestration client. It does not publish Kafka control records.
 */
public class HttpRemoteRunnerClient implements RemoteRunnerClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpRemoteRunnerClient(ObjectMapper objectMapper) {
        this(HttpClient.newHttpClient(), objectMapper);
    }

    HttpRemoteRunnerClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void start(
        String modelLibraryUrl,
        String modelId,
        String runId,
        String simulationId,
        String modelInstanceId,
        String coordinatorId,
        JsonNode initializationParameters,
        KafkaConfigurationDto kafka,
        TimeModeConfigurationDto timeMode,
        String coordinatorHelperEndpoint,
        String callbackToken
    ) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("runId", runId);
        requestBody.put("initializationParameters", initializationParameters);
        Map<String, Object> simulation = new LinkedHashMap<>();
        simulation.put("simulationId", simulationId);
        simulation.put("modelInstanceId", modelInstanceId);
        simulation.put("coordinatorId", coordinatorId);
        simulation.put("timeMode", timeMode);
        requestBody.put("simulation", simulation);
        requestBody.put("kafka", kafka);
        requestBody.put("coordinatorHelper", Map.of(
            "endpoint", coordinatorHelperEndpoint,
            "token", callbackToken
        ));
        URI endpoint = URI.create(trimTrailingSlash(modelLibraryUrl) + "/v1/models/" + modelId + "/run");
        send(HttpRequest.newBuilder(endpoint)
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(toJson(requestBody))), "start");
    }

    @Override
    public void stop(String modelLibraryUrl, String runId) {
        URI endpoint = URI.create(trimTrailingSlash(modelLibraryUrl) + "/v1/runs/" + runId);
        send(HttpRequest.newBuilder(endpoint).DELETE(), "stop");
    }

    private void send(HttpRequest.Builder request, String operation) {
        try {
            HttpResponse<Void> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RemoteRunnerControlException("Model-library " + operation + " request returned HTTP " + response.statusCode());
            }
        } catch (IOException ex) {
            throw new RemoteRunnerControlException("Could not send model-library " + operation + " request", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RemoteRunnerControlException("Model-library " + operation + " request was interrupted", ex);
        }
    }

    private String toJson(Map<String, Object> requestBody) {
        try {
            return objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException ex) {
            throw new RemoteRunnerControlException("Could not serialize remote-runner start request", ex);
        }
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
