package iso.sim.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import iso.sim.server.dto.run.Iso21175Message;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Iso21175MessageParser {
    private static final Logger logger = Logger.getLogger(Iso21175MessageParser.class.getName());

    private final ObjectMapper objectMapper;

    public Iso21175MessageParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<Iso21175Message> parse(String rawMessage) {
        try {
            Iso21175Message message = objectMapper.readValue(rawMessage, Iso21175Message.class);
            if (message.getSimulationRunId() == null || message.getSimulationRunId().isBlank()) {
                logger.fine("Dropping ISO message because simulationRunId is missing");
                return Optional.empty();
            }
            if (message.getMessageType() == null || message.getMessageType().isBlank()) {
                logger.fine("Dropping ISO message because messageType is missing");
                return Optional.empty();
            }
            return Optional.of(message);
        } catch (JsonProcessingException ex) {
            logger.log(Level.FINE, "Dropping ISO message because JSON parsing failed: " + ex.getOriginalMessage(), ex);
            return Optional.empty();
        }
    }
}
