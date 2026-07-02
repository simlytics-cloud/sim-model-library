package iso.sim.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import iso.sim.server.catalog.CatalogRepository;
import iso.sim.server.catalog.ModelCatalogService;
import iso.sim.server.dto.run.KafkaConfigurationDto;
import iso.sim.server.dto.run.SimulationContextDto;
import iso.sim.server.dto.run.StartModelRunRequest;
import iso.sim.server.dto.run.StartModelRunResponse;
import iso.sim.server.dto.run.RunStatusResponse;
import iso.sim.server.dto.run.TimeModeDto;
import iso.sim.server.executor.RunExecutionContext;
import iso.sim.server.executor.RunExecutor;
import iso.sim.server.runtime.NoopRunHandle;
import iso.sim.server.runtime.RunHandle;
import iso.sim.server.runtime.RunResourceRegistry;
import iso.sim.server.service.InvalidRunRequestException;
import iso.sim.server.service.RunNotFoundException;
import iso.sim.server.service.RunReadinessProbe;
import iso.sim.server.service.RunReadinessResult;
import iso.sim.server.service.RunService;
import iso.sim.server.service.RunMonitor;
import iso.sim.server.store.InMemoryRunStatusStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ModelCatalogService catalogService = new ModelCatalogService(
        new CatalogRepository(List.of("model-catalog.json"), objectMapper)
    );
    private final RecordingRunExecutor recordingExecutor = new RecordingRunExecutor();
    private final RunResourceRegistry runResourceRegistry = new RunResourceRegistry();
    private final RunReadinessProbe immediateReadinessProbe = (context, handle) -> RunReadinessResult.ready("Runtime ready immediately");
    private final RunMonitor noopMonitor = (context, runtimeHandle) -> new NoopRunHandle(context.getRunId());
    private final RunService runService = new RunService(
        catalogService,
        recordingExecutor,
        new InMemoryRunStatusStore(),
        runResourceRegistry,
        context -> immediateReadinessProbe,
        noopMonitor
    );

    @Test
    void startRunAcceptsValidRequestWithNewTimeMode() {
        StartModelRunRequest request = new StartModelRunRequest(
            "run-vehicle-002",
            objectMapper.valueToTree(java.util.Map.of("vehicleId", 1)),
            new KafkaConfigurationDto("kafka.example.com:9092", "irp-system", null, null, null, null),
            new SimulationContextDto(
                "sim-irp-001",
                "instance-irp-001",
                null,
                new TimeModeDto("fast-time", "long", 3600.0)
            )
        );

        StartModelRunResponse response = runService.startRun("irpsystem.irpmodel.Vehicle", request);
        assertEquals("run-vehicle-002", response.getRunId());
        RunExecutionContext context = recordingExecutor.lastContext;
        assertNotNull(context);
        assertEquals("fast-time", context.getRequest().getSimulation().getTimeMode().getMode());
        assertEquals("long", context.getRequest().getSimulation().getTimeMode().getTimeType());
        assertEquals(3600.0, context.getRequest().getSimulation().getTimeMode().getSecondsPerSimulationTimeUnit());
    }

    @Test
    void startRunAcceptsValidRequestAndStatusCanBeFetched() {
        StartModelRunRequest request = new StartModelRunRequest(
            "run-vehicle-001",
            objectMapper.valueToTree(java.util.Map.of("vehicleId", 1)),
            new KafkaConfigurationDto("kafka.example.com:9092", "irp-system", null, null, null, null),
            null
        );

        StartModelRunResponse response = runService.startRun("irpsystem.irpmodel.Vehicle", request);
        assertEquals("run-vehicle-001", response.getRunId());
        assertEquals("accepted", response.getStatus());
        assertEquals("/v1/runs/run-vehicle-001", response.getStatusUrl());
        assertEquals("run-vehicle-001", recordingExecutor.lastContext.getRunId());
        assertEquals("irpsystem.irpmodel.Vehicle", recordingExecutor.lastContext.getModelId());

        RunStatusResponse status = runService.getRunStatus(response.getRunId());
        assertEquals("run-vehicle-001", status.getRunId());
        assertEquals("irpsystem.irpmodel.Vehicle", status.getModelId());
        assertEquals("ready", status.getStatus());
        assertNotNull(status.getReadyAt());
        assertNotNull(runResourceRegistry.get("run-vehicle-001"));
    }

    @Test
    void startRunMarksFailedWhenExecutorThrows() {
        RunService failingRunService = new RunService(catalogService, context -> {
            throw new IllegalStateException("boom");
        }, new InMemoryRunStatusStore(), new RunResourceRegistry(), context -> immediateReadinessProbe, noopMonitor);

        StartModelRunRequest request = new StartModelRunRequest(
            "run-fail-001",
            objectMapper.valueToTree(java.util.Map.of("vehicleId", 1)),
            new KafkaConfigurationDto("kafka.example.com:9092", "irp-system", null, null, null, null),
            null
        );

        StartModelRunResponse response = failingRunService.startRun("irpsystem.irpmodel.Vehicle", request);
        assertEquals("accepted", response.getStatus());

        RunStatusResponse status = failingRunService.getRunStatus("run-fail-001");
        assertEquals("failed", status.getStatus());
    }

    @Test
    void startRunThrowsWhenKafkaSectionIsMissing() {
        StartModelRunRequest request = new StartModelRunRequest(
            null,
            objectMapper.valueToTree(java.util.Map.of("vehicleId", 1)),
            null,
            null
        );

        InvalidRunRequestException exception = assertThrows(InvalidRunRequestException.class,
            () -> runService.startRun("irpsystem.irpmodel.Vehicle", request));
        assertTrue(exception.getMessage().contains("kafka"));
    }

    @Test
    void getRunStatusThrowsForUnknownRun() {
        RunNotFoundException exception = assertThrows(RunNotFoundException.class,
            () -> runService.getRunStatus("missing-run"));
        assertTrue(exception.getMessage().contains("missing-run"));
    }

    private static class RecordingRunExecutor implements RunExecutor {
        private RunExecutionContext lastContext;

        @Override
        public RunHandle start(RunExecutionContext context) {
            lastContext = context;
            return new NoopRunHandle(context.getRunId());
        }
    }
}