package iso.sim.coordinator.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import iso.sim.coordinator.helper.dto.KafkaConfigurationDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

class KafkaConfigurationDtoTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesNativeKafkaProperties() throws Exception {
        String json = objectMapper.writeValueAsString(new KafkaConfigurationDto(
            "topic", Map.of(
                "bootstrap.servers", "kafka:9092",
                "sasl.mechanism", "SCRAM-SHA-256"
            )
        ));

        assertEquals("kafka:9092", objectMapper.readTree(json).path("properties").path("bootstrap.servers").asText());
        assertEquals("SCRAM-SHA-256", objectMapper.readTree(json).path("properties").path("sasl.mechanism").asText());
    }
}
