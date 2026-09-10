package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum TimeConversionPolicy {
    EXACT("exact"),
    APPROXIMATE("approximate");

    private final String value;

    TimeConversionPolicy(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TimeConversionPolicy fromValue(String value) {
        return Arrays.stream(values())
            .filter(policy -> policy.value.equals(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported time conversion policy: " + value));
    }
}
