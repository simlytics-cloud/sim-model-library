package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum TimeInfinityPolicy {
    MAX_FINITE("max-finite"),
    STRING_SENTINEL("string-sentinel");

    private final String value;

    TimeInfinityPolicy(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TimeInfinityPolicy fromValue(String value) {
        return Arrays.stream(values())
            .filter(policy -> policy.value.equals(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported time infinity policy: " + value));
    }
}
