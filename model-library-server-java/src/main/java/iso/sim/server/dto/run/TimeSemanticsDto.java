package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeSemanticsDto {
    private final TimeDomain timeDomain;
    private final TimeValueEncoding valueEncoding;
    private final RationalTimeDto unitSeconds;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final RationalTimeDto quantum;
    private final RationalTimeDto originOffset;
    private final TimeConversionPolicy conversionPolicy;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Double maxAbsErrorSeconds;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final TimeRoundingMode roundingMode;
    private final TimeInfinityPolicy infinityPolicy;

    @JsonCreator
    public TimeSemanticsDto(
        @JsonProperty("timeDomain") TimeDomain timeDomain,
        @JsonProperty("valueEncoding") TimeValueEncoding valueEncoding,
        @JsonProperty("unitSeconds") RationalTimeDto unitSeconds,
        @JsonProperty("quantum") RationalTimeDto quantum,
        @JsonProperty("originOffset") RationalTimeDto originOffset,
        @JsonProperty("conversionPolicy") TimeConversionPolicy conversionPolicy,
        @JsonProperty("maxAbsErrorSeconds") Double maxAbsErrorSeconds,
        @JsonProperty("roundingMode") TimeRoundingMode roundingMode,
        @JsonProperty("infinityPolicy") TimeInfinityPolicy infinityPolicy
    ) {
        if (timeDomain == null) {
            throw new IllegalArgumentException("timeDomain is required");
        }
        if (valueEncoding == null) {
            throw new IllegalArgumentException("valueEncoding is required");
        }
        if (unitSeconds == null) {
            throw new IllegalArgumentException("unitSeconds is required");
        }
        if (timeDomain == TimeDomain.DISCRETE && quantum == null) {
            throw new IllegalArgumentException("quantum is required when timeDomain is discrete");
        }
        if (originOffset == null) {
            throw new IllegalArgumentException("originOffset is required");
        }
        if (conversionPolicy == null) {
            throw new IllegalArgumentException("conversionPolicy is required");
        }
        if (conversionPolicy == TimeConversionPolicy.APPROXIMATE) {
            if (maxAbsErrorSeconds == null || maxAbsErrorSeconds <= 0.0d) {
                throw new IllegalArgumentException("maxAbsErrorSeconds must be > 0 when conversionPolicy is approximate");
            }
            if (roundingMode == null) {
                throw new IllegalArgumentException("roundingMode is required when conversionPolicy is approximate");
            }
        }
        if (infinityPolicy == null) {
            throw new IllegalArgumentException("infinityPolicy is required");
        }
        this.timeDomain = timeDomain;
        this.valueEncoding = valueEncoding;
        this.unitSeconds = unitSeconds;
        this.quantum = quantum;
        this.originOffset = originOffset;
        this.conversionPolicy = conversionPolicy;
        this.maxAbsErrorSeconds = conversionPolicy == TimeConversionPolicy.APPROXIMATE ? maxAbsErrorSeconds : null;
        this.roundingMode = conversionPolicy == TimeConversionPolicy.APPROXIMATE ? roundingMode : null;
        this.infinityPolicy = infinityPolicy;
    }

    public TimeDomain getTimeDomain() { return timeDomain; }
    public TimeValueEncoding getValueEncoding() { return valueEncoding; }
    public RationalTimeDto getUnitSeconds() { return unitSeconds; }
    public RationalTimeDto getQuantum() { return quantum; }
    public RationalTimeDto getOriginOffset() { return originOffset; }
    public TimeConversionPolicy getConversionPolicy() { return conversionPolicy; }
    public Double getMaxAbsErrorSeconds() { return maxAbsErrorSeconds; }
    public TimeRoundingMode getRoundingMode() { return roundingMode; }
    public TimeInfinityPolicy getInfinityPolicy() { return infinityPolicy; }
}
