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

package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Iso21175Message {
    private final String simulationRunId;
    private final String messageId;
    private final String messageType;
    private final String senderId;
    private final String receiverId;
    private final JsonNode eventTime;
    private final JsonNode nextInternalTime;
    private final JsonNode payload;
    private final String wallClockTime;

    @JsonCreator
    public Iso21175Message(
        @JsonProperty("simulationRunId") String simulationRunId,
        @JsonProperty("messageId") String messageId,
        @JsonProperty("messageType") String messageType,
        @JsonProperty("senderId") String senderId,
        @JsonProperty("receiverId") String receiverId,
        @JsonProperty("eventTime") JsonNode eventTime,
        @JsonProperty("nextInternalTime") JsonNode nextInternalTime,
        @JsonProperty("payload") JsonNode payload,
        @JsonProperty("wallClockTime") String wallClockTime
    ) {
        this.simulationRunId = simulationRunId;
        this.messageId = messageId;
        this.messageType = messageType;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.eventTime = eventTime;
        this.nextInternalTime = nextInternalTime;
        this.payload = payload;
        this.wallClockTime = wallClockTime;
    }

    public String getSimulationRunId() { return simulationRunId; }
    public String getMessageId() { return messageId; }
    public String getMessageType() { return messageType; }
    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public JsonNode getEventTime() { return eventTime; }
    public JsonNode getNextInternalTime() { return nextInternalTime; }
    public JsonNode getPayload() { return payload; }
    public String getWallClockTime() { return wallClockTime; }
}