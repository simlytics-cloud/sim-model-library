package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrentSimulationTimeDto {
    private final Double value;
    private final String timeType;
    private final Double secondsPerSimulationTimeUnit;
    private final String sourceMessageType;
    private final String sourceMessageId;
    private final String updatedAt;

    @JsonCreator
    public CurrentSimulationTimeDto(
        @JsonProperty("value") Double value,
        @JsonProperty("timeType") String timeType,
        @JsonProperty("secondsPerSimulationTimeUnit") Double secondsPerSimulationTimeUnit,
        @JsonProperty("sourceMessageType") String sourceMessageType,
        @JsonProperty("sourceMessageId") String sourceMessageId,
        @JsonProperty("updatedAt") String updatedAt
    ) {
        this.value = value;
        this.timeType = timeType;
        this.secondsPerSimulationTimeUnit = secondsPerSimulationTimeUnit;
        this.sourceMessageType = sourceMessageType;
        this.sourceMessageId = sourceMessageId;
        this.updatedAt = updatedAt;
    }

    public Double getValue() { return value; }
    public String getTimeType() { return timeType; }
    public Double getSecondsPerSimulationTimeUnit() { return secondsPerSimulationTimeUnit; }
    public String getSourceMessageType() { return sourceMessageType; }
    public String getSourceMessageId() { return sourceMessageId; }
    public String getUpdatedAt() { return updatedAt; }
}