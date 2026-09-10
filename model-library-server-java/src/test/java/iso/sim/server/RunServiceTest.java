package iso.sim.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import iso.sim.server.catalog.CatalogRepository;
import iso.sim.server.catalog.ModelCatalogService;
import iso.sim.server.dto.run.KafkaConfigurationDto;
import iso.sim.server.dto.run.SimulationContextDto;
import iso.sim.server.dto.run.StartModelRunRequest;
import iso.sim.server.dto.run.StartModelRunResponse;
import iso.sim.server.dto.run.RunStatusResponse;
import iso.sim.server.dto.run.RationalTimeDto;
import iso.sim.server.dto.run.TimeConversionPolicy;
import iso.sim.server.dto.run.TimeDomain;
import iso.sim.server.dto.run.TimeInfinityPolicy;
import iso.sim.server.dto.run.TimeMode;
import iso.sim.server.dto.run.TimeModeDto;
import iso.sim.server.dto.run.TimeSemanticsDto;
import iso.sim.server.dto.run.TimeValueEncoding;
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
import static org.junit.jupiter.api.Assertions.assertNull;
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
            new KafkaConfigurationDto("kafka.example.com:9092", "irp-system", null, null, null),
            new SimulationContextDto(
                "sim-irp-001",
                "instance-irp-001",
                null,
                new TimeModeDto(
                    TimeMode.VIRTUAL_TIME,
                    new TimeSemanticsDto(
                        TimeDomain.DISCRETE,
                        TimeValueEncoding.INT64,
                        new RationalTimeDto(3600, 1),
                        new RationalTimeDto(1, 1),
                        new RationalTimeDto(0, 1),
                        TimeConversionPolicy.EXACT,
                        null,
                        null,
                        TimeInfinityPolicy.MAX_FINITE
                    ),
                    null
                )
            )
        );

        StartModelRunResponse response = runService.startRun("irpsystem.irpmodel.Vehicle", request);
        assertEquals("run-vehicle-002", response.getRunId());
        RunExecutionContext context = recordingExecutor.lastContext;
        assertNotNull(context);
        assertEquals(TimeMode.VIRTUAL_TIME, context.getRequest().getSimulation().getTimeMode().getMode());
        assertEquals(TimeDomain.DISCRETE, context.getRequest().getSimulation().getTimeMode().getTimeSemantics().getTimeDomain());
        assertEquals(3600L, context.getRequest().getSimulation().getTimeMode().getTimeSemantics().getUnitSeconds().getNumerator());
        assertEquals(1L, context.getRequest().getSimulation().getTimeMode().getTimeSemantics().getUnitSeconds().getDenominator());
    }

    @Test
    void startRunAcceptsValidRequestAndStatusCanBeFetched() {
        StartModelRunRequest request = new StartModelRunRequest(
            "run-vehicle-001",
            objectMapper.valueToTree(java.util.Map.of("vehicleId", 1)),
            new KafkaConfigurationDto("kafka.example.com:9092", "irp-system", null, null, null),
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
            new KafkaConfigurationDto("kafka.example.com:9092", "irp-system", null, null, null),
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

    @Test
    void cancelRunStopsResourcesAndMarksCanceled() {
        StartModelRunRequest request = new StartModelRunRequest(
            "run-cancel-001",
            objectMapper.valueToTree(java.util.Map.of("vehicleId", 1)),
            new KafkaConfigurationDto("kafka.example.com:9092", "irp-system", null, null, null),
            null
        );

        runService.startRun("irpsystem.irpmodel.Vehicle", request);

        RunStatusResponse canceled = runService.cancelRun("run-cancel-001");
        assertEquals("canceled", canceled.getStatus());
        assertEquals("Run canceled by request", canceled.getMessage());
        assertTrue(recordingExecutor.lastHandle.wasStopped());
        assertEquals("canceled", runService.getRunStatus("run-cancel-001").getStatus());
        assertNull(runResourceRegistry.get("run-cancel-001"));
    }

    @Test
    void cancelRunReturnsCurrentStatusWhenRunIsAlreadyTerminal() {
        RunService failingRunService = new RunService(catalogService, context -> {
            throw new IllegalStateException("boom");
        }, new InMemoryRunStatusStore(), new RunResourceRegistry(), context -> immediateReadinessProbe, noopMonitor);

        StartModelRunRequest request = new StartModelRunRequest(
            "run-failed-001",
            objectMapper.valueToTree(java.util.Map.of("vehicleId", 1)),
            new KafkaConfigurationDto("kafka.example.com:9092", "irp-system", null, null, null),
            null
        );
        failingRunService.startRun("irpsystem.irpmodel.Vehicle", request);

        RunStatusResponse canceled = failingRunService.cancelRun("run-failed-001");
        assertEquals("failed", canceled.getStatus());
        assertEquals("failed", failingRunService.getRunStatus("run-failed-001").getStatus());
    }

    @Test
    void cancelRunThrowsForUnknownRun() {
        RunNotFoundException exception = assertThrows(RunNotFoundException.class,
            () -> runService.cancelRun("missing-run"));
        assertTrue(exception.getMessage().contains("missing-run"));
    }

    private static class RecordingRunExecutor implements RunExecutor {
        private RunExecutionContext lastContext;
        private RecordingRunHandle lastHandle;

        @Override
        public RunHandle start(RunExecutionContext context) {
            lastContext = context;
            lastHandle = new RecordingRunHandle(context.getRunId());
            return lastHandle;
        }
    }

    private static class RecordingRunHandle implements RunHandle {
        private final String runId;
        private boolean stopped;

        private RecordingRunHandle(String runId) {
            this.runId = runId;
        }

        @Override
        public String runId() {
            return runId;
        }

        @Override
        public boolean isAlive() {
            return !stopped;
        }

        @Override
        public void stop() {
            stopped = true;
        }

        private boolean wasStopped() {
            return stopped;
        }
    }
}
