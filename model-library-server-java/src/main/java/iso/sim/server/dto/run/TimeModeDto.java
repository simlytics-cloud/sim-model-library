package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeModeDto {
    private final TimeMode mode;
    private final TimeType timeType;
    private final Double secondsPerSimulationTimeUnit;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Double realTimeFactor;

    @JsonCreator
    public TimeModeDto(
        @JsonProperty("mode") TimeMode mode,
        @JsonProperty("timeType") TimeType timeType,
        @JsonProperty("secondsPerSimulationTimeUnit") Double secondsPerSimulationTimeUnit,
        @JsonProperty("realTimeFactor") Double realTimeFactor
    ) {
        this.mode = mode;
        this.timeType = timeType;
        this.secondsPerSimulationTimeUnit = secondsPerSimulationTimeUnit;
        this.realTimeFactor = mode == TimeMode.SCALED_TIME ? realTimeFactor : null;
    }

    public TimeModeDto(TimeMode mode, TimeType timeType, Double secondsPerSimulationTimeUnit) {
        this(mode, timeType, secondsPerSimulationTimeUnit, null);
    }

    public TimeMode getMode() { return mode; }
    public TimeType getTimeType() { return timeType; }
    public Double getSecondsPerSimulationTimeUnit() { return secondsPerSimulationTimeUnit; }
    public Double getRealTimeFactor() { return realTimeFactor; }
}
