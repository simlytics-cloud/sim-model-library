package iso.sim.server.service;

import iso.sim.server.catalog.ModelCatalogService;
import iso.sim.server.dto.run.KafkaConfigurationDto;
import iso.sim.server.dto.run.RunStatusResponse;
import iso.sim.server.dto.run.StartModelRunRequest;
import iso.sim.server.dto.run.StartModelRunResponse;
import iso.sim.server.executor.RunExecutionContext;
import iso.sim.server.executor.RunExecutor;
import iso.sim.server.executor.StubRunExecutor;
import iso.sim.server.runtime.RunResourceRegistry;
import iso.sim.server.store.InMemoryRunStatusStore;
import iso.sim.server.store.RunStatusStore;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.Executors;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RunService {
    private static final Set<String> TERMINAL_STATUSES = Set.of("completed", "failed", "canceled");

    private final ModelCatalogService modelCatalogService;
    private final RunStatusStore runStatusStore;
    private final RunLifecycleService runLifecycleService;
    private final RunResourceRegistry runResourceRegistry;
    private final RunTerminalCoordinator runTerminalCoordinator;
    private final RunLifecycleManager runLifecycleManager;

    public RunService(ModelCatalogService modelCatalogService, RunReadinessProbeSelector runReadinessProbeSelector) {
        this(modelCatalogService, new StubRunExecutor(), new InMemoryRunStatusStore(), new RunResourceRegistry(), runReadinessProbeSelector);
    }

    public RunService(
        ModelCatalogService modelCatalogService,
        RunExecutor runExecutor,
        RunStatusStore runStatusStore,
        RunReadinessProbeSelector runReadinessProbeSelector
    ) {
        this(modelCatalogService, runExecutor, runStatusStore, new RunResourceRegistry(), runReadinessProbeSelector);
    }

    public RunService(
        ModelCatalogService modelCatalogService,
        RunExecutor runExecutor,
        RunStatusStore runStatusStore,
        RunResourceRegistry runResourceRegistry,
        RunReadinessProbeSelector runReadinessProbeSelector
    ) {
        this(
            modelCatalogService,
            runExecutor,
            runStatusStore,
            runResourceRegistry,
            runReadinessProbeSelector,
            buildDefaultMonitor(runStatusStore, runResourceRegistry)
        );
    }

    private static RunMonitor buildDefaultMonitor(RunStatusStore runStatusStore, RunResourceRegistry runResourceRegistry) {
        RunLifecycleService lifecycleService = new RunLifecycleService(runStatusStore);
        RunTerminalCoordinator terminalCoordinator = new RunTerminalCoordinator(lifecycleService, runStatusStore, runResourceRegistry);
        return new KafkaIsoRunMonitor(
            lifecycleService,
            runStatusStore,
            terminalCoordinator,
            new Iso21175MessageParser(new ObjectMapper()),
            new DefaultKafkaConsumerAdapterFactory(),
            Executors.newSingleThreadExecutor()
        );
    }

    public RunService(
        ModelCatalogService modelCatalogService,
        RunExecutor runExecutor,
        RunStatusStore runStatusStore,
        RunResourceRegistry runResourceRegistry,
        RunReadinessProbeSelector runReadinessProbeSelector,
        RunMonitor runMonitor
    ) {
        this(
            modelCatalogService,
            runExecutor,
            runStatusStore,
            runResourceRegistry,
            runReadinessProbeSelector,
            runMonitor,
            new RunLifecycleService(runStatusStore)
        );
    }

    public RunService(
        ModelCatalogService modelCatalogService,
        RunExecutor runExecutor,
        RunStatusStore runStatusStore,
        RunResourceRegistry runResourceRegistry,
        RunReadinessProbeSelector runReadinessProbeSelector,
        RunMonitor runMonitor,
        RunLifecycleService runLifecycleService
    ) {
        this.modelCatalogService = modelCatalogService;
        this.runStatusStore = runStatusStore;
        this.runLifecycleService = runLifecycleService;
        this.runResourceRegistry = runResourceRegistry;
        this.runTerminalCoordinator = new RunTerminalCoordinator(runLifecycleService, runStatusStore, runResourceRegistry);
        this.runLifecycleManager = new RunLifecycleManager(
            runExecutor,
            runLifecycleService,
            runResourceRegistry,
            runTerminalCoordinator,
            runReadinessProbeSelector,
            runMonitor
        );
    }

    public StartModelRunResponse startRun(String modelId, StartModelRunRequest request) {
        modelCatalogService.getModel(modelId);
        validateRequest(request);

        String runId = request.getRunId() != null && !request.getRunId().isBlank()
            ? request.getRunId()
            : "run-" + UUID.randomUUID();
        String statusUrl = "/v1/runs/" + runId;
        RunStatusResponse status = runLifecycleService.markAccepted(runId, modelId, "Run request accepted");

        runLifecycleManager.startRun(new RunExecutionContext(runId, modelId, request));

        return new StartModelRunResponse(
            runId,
            modelId,
            "accepted",
            statusUrl,
            status.getAcceptedAt(),
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

    public RunStatusResponse cancelRun(String runId) {
        RunStatusResponse current = runStatusStore.get(runId);
        if (current == null) {
            throw new RunNotFoundException(runId);
        }
        if (TERMINAL_STATUSES.contains(current.getStatus())) {
            return current;
        }

        runResourceRegistry.stop(runId);
        return runLifecycleService.markCanceled(runId, "Run canceled by request");
    }

    public RunStatusStore getRunStatusStore() {
        return runStatusStore;
    }

    public RunResourceRegistry getRunResourceRegistry() {
        return runResourceRegistry;
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
