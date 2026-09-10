package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KafkaConfigurationDto {
    private final String bootstrapServers;
    private final String topic;
    private final String securityProtocol;
    private final String saslMechanism;
    private final Map<String, String> properties;

    @JsonCreator
    public KafkaConfigurationDto(
        @JsonProperty("bootstrapServers") String bootstrapServers,
        @JsonProperty("topic") String topic,
        @JsonProperty("securityProtocol") String securityProtocol,
        @JsonProperty("saslMechanism") String saslMechanism,
        @JsonProperty("properties") Map<String, String> properties
    ) {
        this.bootstrapServers = bootstrapServers;
        this.topic = topic;
        this.securityProtocol = securityProtocol;
        this.saslMechanism = saslMechanism;
        this.properties = properties;
    }

    public String getBootstrapServers() { return bootstrapServers; }
    public String getTopic() { return topic; }
    public String getSecurityProtocol() { return securityProtocol; }
    public String getSaslMechanism() { return saslMechanism; }
    public Map<String, String> getProperties() { return properties; }
}
