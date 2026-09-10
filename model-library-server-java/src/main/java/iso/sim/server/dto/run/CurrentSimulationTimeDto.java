package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrentSimulationTimeDto {
    private final BigDecimal value;
    private final String sourceMessageType;
    private final String sourceMessageId;
    private final String updatedAt;

    @JsonCreator
    public CurrentSimulationTimeDto(
        @JsonProperty("value") BigDecimal value,
        @JsonProperty("sourceMessageType") String sourceMessageType,
        @JsonProperty("sourceMessageId") String sourceMessageId,
        @JsonProperty("updatedAt") String updatedAt
    ) {
        this.value = value;
        this.sourceMessageType = sourceMessageType;
        this.sourceMessageId = sourceMessageId;
        this.updatedAt = updatedAt;
    }

    public BigDecimal getValue() { return value; }
    public String getSourceMessageType() { return sourceMessageType; }
    public String getSourceMessageId() { return sourceMessageId; }
    public String getUpdatedAt() { return updatedAt; }
}
