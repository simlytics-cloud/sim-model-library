/*
 * Sim Model Library Copyright (C) 2026 simlytics.cloud LLC and
 * Sim Model Library contributors.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License
 *  is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing permissions and limitations under
 *  the License.
 *
 */

package iso.sim.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.concurrent.Executors;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class RunService {
    private static final Set<String> TERMINAL_STATUSES = Set.of("locally-stopped", "locally-failed");
    private static final Pattern RECEIVER_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,127}");

    private final ModelCatalogService modelCatalogService;
    private final RunStatusStore runStatusStore;
    private final RunLifecycleService runLifecycleService;
    private final RunResourceRegistry runResourceRegistry;
    private final RunTerminalCoordinator runTerminalCoordinator;
    private final RunLifecycleManager runLifecycleManager;
    private final CoordinatorHelperCallbackReporter coordinatorHelperCallbackReporter;
    private final Map<String, RunExecutionContext> executionContexts = new ConcurrentHashMap<>();

    public RunService(ModelCatalogService modelCatalogService, RunReadinessProbeSelector runReadinessProbeSelector) {
        this(
            modelCatalogService,
            new StubRunExecutor(),
            new InMemoryRunStatusStore(),
            new RunResourceRegistry(),
            runReadinessProbeSelector,
            new HttpCoordinatorHelperCallbackReporter(new com.fasterxml.jackson.databind.ObjectMapper())
        );
    }

    public RunService(
        ModelCatalogService modelCatalogService,
        RunExecutor runExecutor,
        RunStatusStore runStatusStore,
        RunReadinessProbeSelector runReadinessProbeSelector
    ) {
        this(
            modelCatalogService,
            runExecutor,
            runStatusStore,
            new RunResourceRegistry(),
            runReadinessProbeSelector,
            new HttpCoordinatorHelperCallbackReporter(new com.fasterxml.jackson.databind.ObjectMapper())
        );
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
            new HttpCoordinatorHelperCallbackReporter(new com.fasterxml.jackson.databind.ObjectMapper())
        );
    }

    public RunService(
        ModelCatalogService modelCatalogService,
        RunExecutor runExecutor,
        RunStatusStore runStatusStore,
        RunResourceRegistry runResourceRegistry,
        RunReadinessProbeSelector runReadinessProbeSelector,
        CoordinatorHelperCallbackReporter coordinatorHelperCallbackReporter
    ) {
        this(
            modelCatalogService,
            runExecutor,
            runStatusStore,
            runResourceRegistry,
            runReadinessProbeSelector,
            buildDefaultMonitor(runStatusStore, runResourceRegistry, coordinatorHelperCallbackReporter),
            coordinatorHelperCallbackReporter
        );
    }

    private static RunMonitor buildDefaultMonitor(
        RunStatusStore runStatusStore,
        RunResourceRegistry runResourceRegistry,
        CoordinatorHelperCallbackReporter coordinatorHelperCallbackReporter
    ) {
        RunLifecycleService lifecycleService = new RunLifecycleService(runStatusStore);
        RunTerminalCoordinator terminalCoordinator = new RunTerminalCoordinator(lifecycleService, runStatusStore, runResourceRegistry);
        return new KafkaIsoRunMonitor(
            lifecycleService,
            runStatusStore,
            terminalCoordinator,
            new Iso21175MessageParser(new ObjectMapper()),
            new DefaultKafkaConsumerAdapterFactory(),
            Executors.newSingleThreadExecutor(),
            coordinatorHelperCallbackReporter
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
            (context, eventType, errorDetail) -> { }
        );
    }

    public RunService(
        ModelCatalogService modelCatalogService,
        RunExecutor runExecutor,
        RunStatusStore runStatusStore,
        RunResourceRegistry runResourceRegistry,
        RunReadinessProbeSelector runReadinessProbeSelector,
        RunMonitor runMonitor,
        CoordinatorHelperCallbackReporter coordinatorHelperCallbackReporter
    ) {
        this(
            modelCatalogService,
            runExecutor,
            runStatusStore,
            runResourceRegistry,
            runReadinessProbeSelector,
            runMonitor,
            new RunLifecycleService(runStatusStore),
            coordinatorHelperCallbackReporter
        );
    }

    public RunService(
        ModelCatalogService modelCatalogService,
        RunExecutor runExecutor,
        RunStatusStore runStatusStore,
        RunResourceRegistry runResourceRegistry,
        RunReadinessProbeSelector runReadinessProbeSelector,
        RunMonitor runMonitor,
        RunLifecycleService runLifecycleService,
        CoordinatorHelperCallbackReporter coordinatorHelperCallbackReporter
    ) {
        this.modelCatalogService = modelCatalogService;
        this.runStatusStore = runStatusStore;
        this.runLifecycleService = runLifecycleService;
        this.runResourceRegistry = runResourceRegistry;
        this.runTerminalCoordinator = new RunTerminalCoordinator(runLifecycleService, runStatusStore, runResourceRegistry);
        this.coordinatorHelperCallbackReporter = coordinatorHelperCallbackReporter;
        this.runLifecycleManager = new RunLifecycleManager(
            runExecutor,
            runLifecycleService,
            runResourceRegistry,
            runTerminalCoordinator,
            runReadinessProbeSelector,
            runMonitor,
            coordinatorHelperCallbackReporter
        );
    }

    public StartModelRunResponse startRun(String modelId, StartModelRunRequest request) {
        modelCatalogService.getModel(modelId);
        validateRequest(request);

        String runId = request.getRunId();
        String statusUrl = "/v1/runs/" + runId;
        RunExecutionContext context = new RunExecutionContext(runId, modelId, request);
        if (!runLifecycleService.markAccepted(context, "Model library accepted remote model-run request")) {
            throw new RunAlreadyExistsException(runId);
        }
        executionContexts.put(runId, context);
        RunStatusResponse status = runStatusStore.get(runId);

        coordinatorHelperCallbackReporter.report(context, RemoteRunnerEventType.REMOTE_RUNNER_ACCEPTED, null);
        runLifecycleManager.startRun(context);

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
        RunStatusResponse stopped = runLifecycleService.markLocallyStopped(runId, "Local instance stopped by request");
        RunExecutionContext context = executionContexts.get(runId);
        if (context != null) {
            coordinatorHelperCallbackReporter.report(context, RemoteRunnerEventType.REMOTE_RUNNER_STOPPED, null);
        }
        return stopped;
    }

    public RunStatusStore getRunStatusStore() {
        return runStatusStore;
    }

    public RunResourceRegistry getRunResourceRegistry() {
        return runResourceRegistry;
    }

    private void validateRequest(StartModelRunRequest request) {
        if (request == null) {
            throw new InvalidRunRequestException("Request body is required");
        }
        if (request.getRunId() == null || request.getRunId().isBlank()) {
            throw new InvalidRunRequestException("'runId' is required and must be supplied by the coordinator or its orchestrator");
        }
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
        if (request.getCoordinatorHelper() != null
            && (request.getCoordinatorHelper().getEndpoint() == null || request.getCoordinatorHelper().getEndpoint().isBlank()
            || request.getCoordinatorHelper().getToken() == null || request.getCoordinatorHelper().getToken().isBlank())) {
            throw new InvalidRunRequestException("'coordinatorHelper.endpoint' and 'coordinatorHelper.token' are both required when coordinatorHelper is supplied");
        }
        if (request.getSimulation() == null) {
            throw new InvalidRunRequestException("'simulation' is required");
        }
        if (request.getSimulation().getSimulationId() == null || request.getSimulation().getSimulationId().isBlank()) {
            throw new InvalidRunRequestException("'simulation.simulationId' is required");
        }
        if (!isReceiverId(request.getSimulation().getModelInstanceId())) {
            throw new InvalidRunRequestException("'simulation.modelInstanceId' must use letters, digits, '.', '_', or '-'");
        }
        if (!isReceiverId(request.getSimulation().getCoordinatorId())) {
            throw new InvalidRunRequestException("'simulation.coordinatorId' must use letters, digits, '.', '_', or '-'");
        }
        if (request.getSimulation().getTimeMode() == null) {
            throw new InvalidRunRequestException("'simulation.timeMode' is required");
        }
    }

    private boolean isReceiverId(String value) {
        return value != null && RECEIVER_ID.matcher(value).matches();
    }
}
