/*
 * Sim Model Library Copyright (C) 2026 simlytics.cloud LLC and
 * Sim Model Library contributors.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License
 *  is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing permissions and limitations under
 *  the License.
 *
 */

package iso.sim.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import iso.sim.server.dto.run.Iso21175Message;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
              \"eventTime\": 120.0,
              \"nextInternalTime\": 125.0
            }
            """;

        Optional<Iso21175Message> parsed = parser.parse(raw);

        assertTrue(parsed.isPresent());
        assertEquals(new BigDecimal("120.0"), parsed.get().getEventTime().decimalValue());
        assertEquals(new BigDecimal("125.0"), parsed.get().getNextInternalTime().decimalValue());
    }
}