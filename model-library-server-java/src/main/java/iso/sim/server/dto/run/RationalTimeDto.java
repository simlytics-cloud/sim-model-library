package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RationalTimeDto {
    private final long numerator;
    private final long denominator;

    @JsonCreator
    public RationalTimeDto(
        @JsonProperty("numerator") long numerator,
        @JsonProperty("denominator") long denominator
    ) {
        if (denominator <= 0L) {
            throw new IllegalArgumentException("denominator must be greater than zero");
        }
        this.numerator = numerator;
        this.denominator = denominator;
    }

    public long getNumerator() { return numerator; }
    public long getDenominator() { return denominator; }
}
