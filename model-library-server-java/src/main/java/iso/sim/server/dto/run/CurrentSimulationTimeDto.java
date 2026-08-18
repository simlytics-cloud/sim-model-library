package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrentSimulationTimeDto {
    private final String value;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final TimeSemanticsDto timeSemantics;
    private final String sourceMessageType;
    private final String sourceMessageId;
    private final String updatedAt;

    @JsonCreator
    public CurrentSimulationTimeDto(
        @JsonProperty("value") String value,
        @JsonProperty("timeSemantics") TimeSemanticsDto timeSemantics,
        @JsonProperty("sourceMessageType") String sourceMessageType,
        @JsonProperty("sourceMessageId") String sourceMessageId,
        @JsonProperty("updatedAt") String updatedAt
    ) {
        this.value = value;
        this.timeSemantics = timeSemantics;
        this.sourceMessageType = sourceMessageType;
        this.sourceMessageId = sourceMessageId;
        this.updatedAt = updatedAt;
    }

    public String getValue() { return value; }
    public TimeSemanticsDto getTimeSemantics() { return timeSemantics; }
    public String getSourceMessageType() { return sourceMessageType; }
    public String getSourceMessageId() { return sourceMessageId; }
    public String getUpdatedAt() { return updatedAt; }
}
