package iso.sim.server.service;

import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KafkaDefaultsConfigTest {

    @Test
    void rejectsInvalidEnumValuesInKafkaDefaults() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            KafkaDefaultsConfig.from(ConfigFactory.parseString("""
                model.library.run-config.kafka-defaults {
                  bootstrap-servers = "kafka:9092"
                  topic = "topic"
                  security-protocol = "NOT_A_PROTOCOL"
                  sasl-mechanism = null
                }
                """))
        );

        assertTrue(exception.getMessage().contains("security-protocol"));
        assertTrue(exception.getMessage().contains("NOT_A_PROTOCOL"));
    }

    @Test
    void rejectsSaslDefaultsWithoutAMechanism() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            KafkaDefaultsConfig.from(ConfigFactory.parseString("""
                model.library.run-config.kafka-defaults {
                  bootstrap-servers = "kafka:9092"
                  topic = "topic"
                  security-protocol = "SASL_SSL"
                  sasl-mechanism = null
                }
                """))
        );

        assertTrue(exception.getMessage().contains("saslMechanism"));
    }
}
