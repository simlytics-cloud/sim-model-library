package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeModeDto {
    private final TimeMode mode;
    private final TimeSemanticsDto timeSemantics;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Double realTimeFactor;

    @JsonCreator
    public TimeModeDto(
        @JsonProperty("mode") TimeMode mode,
        @JsonProperty("timeSemantics") TimeSemanticsDto timeSemantics,
        @JsonProperty("realTimeFactor") Double realTimeFactor
    ) {
        if (mode == null) {
            throw new IllegalArgumentException("mode is required");
        }
        if (timeSemantics == null) {
            throw new IllegalArgumentException("timeSemantics is required");
        }
        if (mode == TimeMode.SCALED_REAL_TIME && realTimeFactor == null) {
            throw new IllegalArgumentException("realTimeFactor is required when mode is scaled-real-time");
        }
        this.mode = mode;
        this.timeSemantics = timeSemantics;
        this.realTimeFactor = mode == TimeMode.SCALED_REAL_TIME ? realTimeFactor : null;
    }

    public TimeModeDto(TimeMode mode, TimeSemanticsDto timeSemantics) {
        this(mode, timeSemantics, null);
    }

    public TimeMode getMode() { return mode; }
    public TimeSemanticsDto getTimeSemantics() { return timeSemantics; }
    public Double getRealTimeFactor() { return realTimeFactor; }
}
