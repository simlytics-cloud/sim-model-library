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
