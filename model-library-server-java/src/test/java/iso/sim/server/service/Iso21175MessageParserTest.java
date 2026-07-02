package iso.sim.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import iso.sim.server.dto.run.Iso21175Message;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Iso21175MessageParserTest {

    private final Iso21175MessageParser parser = new Iso21175MessageParser(new ObjectMapper());

    @Test
    void parseExtractsNextInternalTimeAndEventTime() {
        String raw = """
            {
              \"simulationRunId\": \"run-1\",
              \"messageId\": \"msg-1\",
              \"messageType\": \"NextInternalTimeReport\",
              \"eventTime\": \"120.0\",
              \"nextInternalTime\": \"125.0\"
            }
            """;

        Optional<Iso21175Message> parsed = parser.parse(raw);

        assertTrue(parsed.isPresent());
        assertEquals("120.0", parsed.get().getEventTime());
        assertEquals("125.0", parsed.get().getNextInternalTime());
    }
}