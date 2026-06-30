package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StartModelRunResponse {
    private final String runId;
    private final String modelId;
    private final String status;
    private final String statusUrl;
    private final String acceptedAt;
    private final String message;

    @JsonCreator
    public StartModelRunResponse(
        @JsonProperty("runId") String runId,
        @JsonProperty("modelId") String modelId,
        @JsonProperty("status") String status,
        @JsonProperty("statusUrl") String statusUrl,
        @JsonProperty("acceptedAt") String acceptedAt,
        @JsonProperty("message") String message
    ) {
        this.runId = runId;
        this.modelId = modelId;
        this.status = status;
        this.statusUrl = statusUrl;
        this.acceptedAt = acceptedAt;
        this.message = message;
    }

    public String getRunId() { return runId; }
    public String getModelId() { return modelId; }
    public String getStatus() { return status; }
    public String getStatusUrl() { return statusUrl; }
    public String getAcceptedAt() { return acceptedAt; }
    public String getMessage() { return message; }
}
