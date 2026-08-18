package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum TimeMode {
    REAL_TIME("real-time"),
    SCALED_REAL_TIME("scaled-real-time"),
    VIRTUAL_TIME("virtual-time");

    private final String value;

    TimeMode(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TimeMode fromValue(String value) {
        return Arrays.stream(values())
            .filter(timeMode -> timeMode.value.equals(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported time mode: " + value));
    }
}
