package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum TimeDomain {
    DISCRETE("discrete"),
    CONTINUOUS("continuous");

    private final String value;

    TimeDomain(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TimeDomain fromValue(String value) {
        return Arrays.stream(values())
            .filter(timeDomain -> timeDomain.value.equals(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported time domain: " + value));
    }
}
