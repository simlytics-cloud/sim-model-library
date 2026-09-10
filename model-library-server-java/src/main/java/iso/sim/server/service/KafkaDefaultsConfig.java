package iso.sim.server.service;

import com.typesafe.config.Config;
import iso.sim.server.dto.run.KafkaDefaultsResponse;

public class KafkaDefaultsConfig {
    private final String bootstrapServers;
    private final String topic;
    private final String securityProtocol;
    private final String saslMechanism;

    public KafkaDefaultsConfig(
        String bootstrapServers,
        String topic,
        String securityProtocol,
        String saslMechanism
    ) {
        this.bootstrapServers = bootstrapServers;
        this.topic = topic;
        this.securityProtocol = securityProtocol;
        this.saslMechanism = saslMechanism;
    }

    public static KafkaDefaultsConfig from(Config config) {
        return new KafkaDefaultsConfig(
            config.getString("model.library.run-config.kafka-defaults.bootstrap-servers"),
            config.getString("model.library.run-config.kafka-defaults.topic"),
            config.getString("model.library.run-config.kafka-defaults.security-protocol"),
            config.getString("model.library.run-config.kafka-defaults.sasl-mechanism")
        );
    }

    public KafkaDefaultsResponse toResponse() {
        return new KafkaDefaultsResponse(bootstrapServers, topic, securityProtocol, saslMechanism);
    }
}