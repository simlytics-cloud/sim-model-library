package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SimulationContextDto {
    private final String simulationId;
    private final String modelInstanceId;
    private final String coordinatorId;
    private final TimeModeDto timeMode;

    @JsonCreator
    public SimulationContextDto(
        @JsonProperty("simulationId") String simulationId,
        @JsonProperty("modelInstanceId") String modelInstanceId,
        @JsonProperty("coordinatorId") String coordinatorId,
        @JsonProperty("timeMode") TimeModeDto timeMode
    ) {
        this.simulationId = simulationId;
        this.modelInstanceId = modelInstanceId;
        this.coordinatorId = coordinatorId;
        this.timeMode = timeMode;
    }

    public String getSimulationId() { return simulationId; }
    public String getModelInstanceId() { return modelInstanceId; }
    public String getCoordinatorId() { return coordinatorId; }
    public TimeModeDto getTimeMode() { return timeMode; }
}
