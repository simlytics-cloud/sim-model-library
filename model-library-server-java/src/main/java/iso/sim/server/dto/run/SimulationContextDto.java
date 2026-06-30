package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SimulationContextDto {
    private final String simulationId;
    private final String federationId;
    private final TimeModeDto timeMode;

    @JsonCreator
    public SimulationContextDto(
        @JsonProperty("simulationId") String simulationId,
        @JsonProperty("federationId") String federationId,
        @JsonProperty("timeMode") TimeModeDto timeMode
    ) {
        this.simulationId = simulationId;
        this.federationId = federationId;
        this.timeMode = timeMode;
    }

    public String getSimulationId() { return simulationId; }
    public String getFederationId() { return federationId; }
    public TimeModeDto getTimeMode() { return timeMode; }
}
