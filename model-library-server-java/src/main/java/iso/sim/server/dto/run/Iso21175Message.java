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
    private final String eventTime;
    private final String nextInternalTime;
    private final JsonNode payload;
    private final String wallClockTime;

    @JsonCreator
    public Iso21175Message(
        @JsonProperty("simulationRunId") String simulationRunId,
        @JsonProperty("messageId") String messageId,
        @JsonProperty("messageType") String messageType,
        @JsonProperty("senderId") String senderId,
        @JsonProperty("receiverId") String receiverId,
        @JsonProperty("eventTime") String eventTime,
        @JsonProperty("nextInternalTime") String nextInternalTime,
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
    public String getEventTime() { return eventTime; }
    public String getNextInternalTime() { return nextInternalTime; }
    public JsonNode getPayload() { return payload; }
    public String getWallClockTime() { return wallClockTime; }
}