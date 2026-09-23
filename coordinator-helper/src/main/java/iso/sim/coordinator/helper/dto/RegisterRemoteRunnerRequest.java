package iso.sim.coordinator.helper.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record RegisterRemoteRunnerRequest(
    String modelId,
    String modelInstanceId,
    String modelLibraryUrl,
    JsonNode initializationParameters,
    KafkaConfigurationDto kafka,
    TimeModeConfigurationDto timeMode,
    String callbackToken
) {
}
