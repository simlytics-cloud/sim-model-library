package iso.sim.coordinator.helper.dto;

import java.util.Map;

public record KafkaConfigurationDto(
    String topic,
    Map<String, String> properties
) {
}
