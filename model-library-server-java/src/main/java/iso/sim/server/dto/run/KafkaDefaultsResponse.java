package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KafkaDefaultsResponse {
    private final String bootstrapServers;
    private final String topic;
    private final String consumerGroup;
    private final String securityProtocol;
    private final String saslMechanism;

    @JsonCreator
    public KafkaDefaultsResponse(
        @JsonProperty("bootstrapServers") String bootstrapServers,
        @JsonProperty("topic") String topic,
        @JsonProperty("consumerGroup") String consumerGroup,
        @JsonProperty("securityProtocol") String securityProtocol,
        @JsonProperty("saslMechanism") String saslMechanism
    ) {
        this.bootstrapServers = bootstrapServers;
        this.topic = topic;
        this.consumerGroup = consumerGroup;
        this.securityProtocol = securityProtocol;
        this.saslMechanism = saslMechanism;
    }

    public String getBootstrapServers() { return bootstrapServers; }
    public String getTopic() { return topic; }
    public String getConsumerGroup() { return consumerGroup; }
    public String getSecurityProtocol() { return securityProtocol; }
    public String getSaslMechanism() { return saslMechanism; }
}