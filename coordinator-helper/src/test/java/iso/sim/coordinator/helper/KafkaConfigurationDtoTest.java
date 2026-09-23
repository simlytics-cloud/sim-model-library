package iso.sim.coordinator.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import iso.sim.coordinator.helper.dto.KafkaConfigurationDto;
import iso.sim.coordinator.helper.dto.KafkaSaslMechanism;
import iso.sim.coordinator.helper.dto.KafkaSecurityProtocol;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KafkaConfigurationDtoTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesHyphenatedSaslMechanismsUsingTheirKafkaWireValue() throws Exception {
        String json = objectMapper.writeValueAsString(new KafkaConfigurationDto(
            "kafka:9092", "topic", KafkaSecurityProtocol.SASL_SSL, KafkaSaslMechanism.SCRAM_SHA_256, null
        ));

        assertEquals("SCRAM-SHA-256", objectMapper.readTree(json).path("saslMechanism").asText());
    }

    @Test
    void rejectsSaslMechanismsForNonSaslProtocols() {
        assertThrows(IllegalArgumentException.class, () -> new KafkaConfigurationDto(
            "kafka:9092", "topic", KafkaSecurityProtocol.PLAINTEXT, KafkaSaslMechanism.PLAIN, null
        ));
    }
}
