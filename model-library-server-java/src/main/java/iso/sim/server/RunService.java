package iso.sim.server.service;

import iso.sim.server.dto.run.KafkaConfigurationDto;
import iso.sim.server.dto.run.RunStatusResponse;
import iso.sim.server.dto.run.StartModelRunRequest;
import iso.sim.server.dto.run.StartModelRunResponse;
import iso.sim.server.executor.RunExecutionContext;
import iso.sim.server.executor.RunExecutor;
import iso.sim.server.executor.StubRunExecutor;
import iso.sim.server.store.InMemoryRunStatusStore;
import iso.sim.server.store.RunStatusStore;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RunService {
    private final ModelCatalogService modelCatalogService;
    private final RunExecutor runExecutor;
    private final RunStatusStore runStatusStore;

    public RunService(ModelCatalogService modelCatalogService) {
        this(modelCatalogService, new StubRunExecutor(), new InMemoryRunStatusStore());
    }

    public RunService(ModelCatalogService modelCatalogService, RunExecutor runExecutor, RunStatusStore runStatusStore) {
        this.modelCatalogService = modelCatalogService;
        this.runExecutor = runExecutor;
        this.runStatusStore = runStatusStore;
    }

    public StartModelRunResponse startRun(String modelId, StartModelRunRequest request) {
        modelCatalogService.getModel(modelId);
        validateRequest(request);

        String runId = request.getRunId() != null && !request.getRunId().isBlank()
            ? request.getRunId()
            : "run-" + UUID.randomUUID();
        String acceptedAt = Instant.now().toString();
        String statusUrl = "/v1/runs/" + runId;
        RunStatusResponse status = new RunStatusResponse(
            runId,
            modelId,
            "accepted",
            acceptedAt,
            "Run request accepted; backend start is stubbed for Phase 1E"
        );
        runStatusStore.save(status);
        runExecutor.start(new RunExecutionContext(runId, modelId, request));

        return new StartModelRunResponse(
            runId,
            modelId,
            status.getStatus(),
            statusUrl,
            acceptedAt,
            status.getMessage()
        );
    }

    public RunStatusResponse getRunStatus(String runId) {
        RunStatusResponse status = runStatusStore.get(runId);
        if (status == null) {
            throw new RunNotFoundException(runId);
        }
        return status;
    }

    public List<RunStatusResponse> listRuns() {
        return runStatusStore.getAll();
    }

    public RunStatusStore getRunStatusStore() {
        return runStatusStore;
    }

    private void validateRequest(StartModelRunRequest request) {
        if (request.getInitializationParameters() == null) {
            throw new InvalidRunRequestException("'initializationParameters' is required");
        }
        KafkaConfigurationDto kafka = request.getKafka();
        if (kafka == null) {
            throw new InvalidRunRequestException("'kafka' is required");
        }
        if (kafka.getBootstrapServers() == null || kafka.getBootstrapServers().isBlank()) {
            throw new InvalidRunRequestException("'kafka.bootstrapServers' is required");
        }
        if (kafka.getTopic() == null || kafka.getTopic().isBlank()) {
            throw new InvalidRunRequestException("'kafka.topic' is required");
        }
    }
}