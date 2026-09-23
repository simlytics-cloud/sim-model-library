package iso.sim.coordinator.helper;

import com.fasterxml.jackson.databind.JsonNode;
import iso.sim.coordinator.helper.dto.KafkaConfigurationDto;
import iso.sim.coordinator.helper.dto.TimeModeConfigurationDto;

public interface RemoteRunnerClient {
    void start(
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
    );

    void stop(String modelLibraryUrl, String runId);
}
