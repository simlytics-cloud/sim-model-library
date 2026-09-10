package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum TimeRoundingMode {
    FLOOR("floor"),
    CEILING("ceiling"),
    HALF_UP("half-up");

    private final String value;

    TimeRoundingMode(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TimeRoundingMode fromValue(String value) {
        return Arrays.stream(values())
            .filter(roundingMode -> roundingMode.value.equals(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported time rounding mode: " + value));
    }
}
