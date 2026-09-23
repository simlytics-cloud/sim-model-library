package iso.sim.coordinator.helper.dto;

import java.util.Map;

public record KafkaConfigurationDto(
    String bootstrapServers,
    String topic,
    KafkaSecurityProtocol securityProtocol,
    KafkaSaslMechanism saslMechanism,
    Map<String, String> properties
) {
    public KafkaConfigurationDto {
        if (securityProtocol != null && securityProtocol.usesSasl() && saslMechanism == null) {
            throw new IllegalArgumentException("'kafka.saslMechanism' is required when 'kafka.securityProtocol' uses SASL");
        }
        if ((securityProtocol == null || !securityProtocol.usesSasl()) && saslMechanism != null) {
            throw new IllegalArgumentException("'kafka.saslMechanism' must be omitted or null unless 'kafka.securityProtocol' uses SASL");
        }
    }
}
