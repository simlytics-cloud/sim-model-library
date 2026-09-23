package iso.sim.coordinator.helper.dto;

import java.util.Map;

public record KafkaConfigurationDto(
    String bootstrapServers,
    String topic,
    String securityProtocol,
    String saslMechanism,
    Map<String, String> properties
) {
}
