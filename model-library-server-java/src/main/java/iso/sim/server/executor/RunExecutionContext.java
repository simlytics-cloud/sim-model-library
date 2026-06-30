package iso.sim.server.executor;

import iso.sim.server.dto.run.StartModelRunRequest;

public class RunExecutionContext {
    private final String runId;
    private final String modelId;
    private final StartModelRunRequest request;

    public RunExecutionContext(String runId, String modelId, StartModelRunRequest request) {
        this.runId = runId;
        this.modelId = modelId;
        this.request = request;
    }

    public String getRunId() {
        return runId;
    }

    public String getModelId() {
        return modelId;
    }

    public StartModelRunRequest getRequest() {
        return request;
    }
}
