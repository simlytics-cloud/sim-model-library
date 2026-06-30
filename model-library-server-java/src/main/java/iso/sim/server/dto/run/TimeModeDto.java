package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeModeDto {
    private final String mode;
    private final String timeType;
    private final Double secondsPerSimulationTimeUnit;

    @JsonCreator
    public TimeModeDto(
        @JsonProperty("mode") String mode,
        @JsonProperty("timeType") String timeType,
        @JsonProperty("secondsPerSimulationTimeUnit") Double secondsPerSimulationTimeUnit
    ) {
        this.mode = mode;
        this.timeType = timeType;
        this.secondsPerSimulationTimeUnit = secondsPerSimulationTimeUnit;
    }

    public String getMode() { return mode; }
    public String getTimeType() { return timeType; }
    public Double getSecondsPerSimulationTimeUnit() { return secondsPerSimulationTimeUnit; }
}
