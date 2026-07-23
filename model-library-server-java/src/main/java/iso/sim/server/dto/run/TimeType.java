package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum TimeType {
    DOUBLE("double"),
    LONG("long");

    private final String value;

    TimeType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TimeType fromValue(String value) {
        return Arrays.stream(values())
            .filter(timeType -> timeType.value.equals(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported time type: " + value));
    }
}
