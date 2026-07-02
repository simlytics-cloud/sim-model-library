package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RunStatusResponse {
    private final String runId;
    private final String modelId;
    private final String status;
    private final String acceptedAt;
    private final String readyAt;
    private final String startedAt;
    private final String completedAt;
    private final String message;
    private final CurrentSimulationTimeDto currentSimulationTime;

    @JsonCreator
    public RunStatusResponse(
        @JsonProperty("runId") String runId,
        @JsonProperty("modelId") String modelId,
        @JsonProperty("status") String status,
        @JsonProperty("acceptedAt") String acceptedAt,
        @JsonProperty("readyAt") String readyAt,
        @JsonProperty("startedAt") String startedAt,
        @JsonProperty("completedAt") String completedAt,
        @JsonProperty("message") String message,
        @JsonProperty("currentSimulationTime") CurrentSimulationTimeDto currentSimulationTime
    ) {
        this.runId = runId;
        this.modelId = modelId;
        this.status = status;
        this.acceptedAt = acceptedAt;
        this.readyAt = readyAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.message = message;
        this.currentSimulationTime = currentSimulationTime;
    }

    public RunStatusResponse(String runId, String modelId, String status, String acceptedAt, String message) {
        this(runId, modelId, status, acceptedAt, null, null, null, message, null);
    }


    public String getRunId() { return runId; }
    public String getModelId() { return modelId; }
    public String getStatus() { return status; }
    public String getAcceptedAt() { return acceptedAt; }
    public String getReadyAt() { return readyAt; }
    public String getStartedAt() { return startedAt; }
    public String getCompletedAt() { return completedAt; }
    public String getMessage() { return message; }
    public CurrentSimulationTimeDto getCurrentSimulationTime() { return currentSimulationTime; }
}
