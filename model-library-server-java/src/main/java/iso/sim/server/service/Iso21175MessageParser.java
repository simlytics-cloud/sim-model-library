package iso.sim.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import iso.sim.server.dto.run.Iso21175Message;

import java.util.Optional;

public class Iso21175MessageParser {
    private final ObjectMapper objectMapper;

    public Iso21175MessageParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<Iso21175Message> parse(String rawMessage) {
        try {
            Iso21175Message message = objectMapper.readValue(rawMessage, Iso21175Message.class);
            if (message.getSimulationRunId() == null || message.getSimulationRunId().isBlank()) {
                return Optional.empty();
            }
            if (message.getMessageType() == null || message.getMessageType().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(message);
        } catch (JsonProcessingException ex) {
            return Optional.empty();
        }
    }
}
