package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StartModelRunRequest {
    private final String runId;
    private final JsonNode initializationParameters;
    private final KafkaConfigurationDto kafka;
    private final SimulationContextDto simulation;

    @JsonCreator
    public StartModelRunRequest(
        @JsonProperty("runId") String runId,
        @JsonProperty("initializationParameters") JsonNode initializationParameters,
        @JsonProperty("kafka") KafkaConfigurationDto kafka,
        @JsonProperty("simulation") SimulationContextDto simulation
    ) {
        this.runId = runId;
        this.initializationParameters = initializationParameters;
        this.kafka = kafka;
        this.simulation = simulation;
    }

    public String getRunId() { return runId; }
    public JsonNode getInitializationParameters() { return initializationParameters; }
    public KafkaConfigurationDto getKafka() { return kafka; }
    public SimulationContextDto getSimulation() { return simulation; }
}
