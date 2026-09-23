package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum KafkaSaslMechanism {
    GSSAPI("GSSAPI"),
    PLAIN("PLAIN"),
    SCRAM_SHA_256("SCRAM-SHA-256"),
    SCRAM_SHA_512("SCRAM-SHA-512"),
    OAUTHBEARER("OAUTHBEARER");

    private final String value;

    KafkaSaslMechanism(String value) {
        this.value = value;
    }

    @JsonCreator
    public static KafkaSaslMechanism fromValue(String value) {
        return Arrays.stream(values())
            .filter(mechanism -> mechanism.value.equals(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Invalid Kafka SASL mechanism '" + value + "'. Expected one of: "
                    + Arrays.stream(values()).map(KafkaSaslMechanism::getValue).toList()
            ));
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
