package iso.sim.server.service;

import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkaDefaultsConfigTest {

    @Test
    void readsNativeKafkaPropertiesFromDefaults() {
        var defaults = KafkaDefaultsConfig.from(ConfigFactory.parseString("""
            model.library.run-config.kafka-defaults {
              topic = "topic"
              properties {
                "bootstrap.servers" = "kafka:9092"
                "security.protocol" = "SASL_SSL"
                "sasl.mechanism" = "SCRAM-SHA-512"
              }
            }
            """)).toResponse();

        assertEquals("topic", defaults.getTopic());
        assertEquals("kafka:9092", defaults.getProperties().get("bootstrap.servers"));
        assertEquals("SASL_SSL", defaults.getProperties().get("security.protocol"));
        assertEquals("SCRAM-SHA-512", defaults.getProperties().get("sasl.mechanism"));
    }
}
