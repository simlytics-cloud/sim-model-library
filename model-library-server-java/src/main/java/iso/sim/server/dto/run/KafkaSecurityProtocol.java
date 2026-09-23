package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum KafkaSecurityProtocol {
    PLAINTEXT,
    SSL,
    SASL_PLAINTEXT,
    SASL_SSL;

    @JsonCreator
    public static KafkaSecurityProtocol fromValue(String value) {
        return Arrays.stream(values())
            .filter(protocol -> protocol.name().equals(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Invalid Kafka security protocol '" + value + "'. Expected one of: "
                    + Arrays.toString(values())
            ));
    }

    @JsonValue
    public String getValue() {
        return name();
    }

    public boolean usesSasl() {
        return this == SASL_PLAINTEXT || this == SASL_SSL;
    }
}
