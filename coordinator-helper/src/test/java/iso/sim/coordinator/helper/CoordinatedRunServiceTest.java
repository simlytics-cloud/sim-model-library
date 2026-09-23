package iso.sim.coordinator.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import iso.sim.coordinator.helper.dto.CoordinatedRunStatusResponse;
import iso.sim.coordinator.helper.dto.CoordinatorEventReport;
import iso.sim.coordinator.helper.dto.CreateCoordinatedRunRequest;
import iso.sim.coordinator.helper.dto.KafkaConfigurationDto;
import iso.sim.coordinator.helper.dto.RegisterRemoteRunnerRequest;
import iso.sim.coordinator.helper.dto.RemoteRunnerEventReport;
import iso.sim.coordinator.helper.dto.TimeModeConfigurationDto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CoordinatedRunServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void startsRegisteredRemoteRunnersThenAggregatesCallbacksAndCoordinatorFacts() {
        List<String> actions = new ArrayList<>();
        AtomicReference<StartCall> startCall = new AtomicReference<>();
        CoordinatedRunService service = new CoordinatedRunService(
            new CoordinatorController() {
                @Override
                public void start(CoordinatedRunStatusResponse run) {
                    actions.add("start-coordinator:" + run.runId());
                }

                @Override
                public void cancel(CoordinatedRunStatusResponse run) {
                    actions.add("cancel-coordinator:" + run.runId());
                }
            },
            new RemoteRunnerClient() {
                @Override
                public void start(
                    String modelLibraryUrl,
                    String modelId,
                    String runId,
                    String simulationId,
                    String modelInstanceId,
                    String coordinatorId,
                    com.fasterxml.jackson.databind.JsonNode initializationParameters,
                    KafkaConfigurationDto kafka,
                    TimeModeConfigurationDto timeMode,
                    String callbackEndpoint,
                    String callbackToken
                ) {
                    startCall.set(new StartCall(modelLibraryUrl, modelId, runId, simulationId, modelInstanceId,
                        coordinatorId, kafka.topic(), callbackEndpoint, callbackToken));
                }

                @Override
                public void stop(String modelLibraryUrl, String runId) {
                    actions.add("stop:" + modelLibraryUrl + ":" + runId);
                }
            },
            "https://helper.example/"
        );
        service.createRun(new CreateCoordinatedRunRequest("run-1", "simulation-1", "coordinator-1", "coordinator-token"));
        service.registerRemoteRunner("run-1", registration("instance-1"));

        assertEquals("starting", service.startRun("run-1").status());
        assertEquals(new StartCall(
            "https://model-library.example", "model-1", "run-1", "simulation-1", "instance-1",
            "coordinator-1", "simulation-topic", "https://helper.example", "callback-token"
        ), startCall.get());

        RemoteRunnerEventReport ready = event("run-1", "instance-1", "remote-runner-ready", "ready-1");
        assertEquals("ready", service.reportRemoteRunnerEvent("run-1", "instance-1", "callback-token", ready).status());
        assertEquals("ready", service.reportRemoteRunnerEvent("run-1", "instance-1", "callback-token", ready).status());
        assertEquals(List.of("start-coordinator:run-1"), actions);

        assertEquals("running", service.reportCoordinatorEvent(
            "run-1", "coordinator-token", new CoordinatorEventReport("progress-1", "progress", "2026-01-01T00:00:00Z", null)
        ).status());
        service.reportRemoteRunnerEvent(
            "run-1", "instance-1", "callback-token", event("run-1", "instance-1", "remote-runner-stopped", "stopped-1")
        );
        assertEquals("completed", service.reportCoordinatorEvent(
            "run-1", "coordinator-token", new CoordinatorEventReport("complete-1", "completed", "2026-01-01T00:01:00Z", null)
        ).status());
    }

    @Test
    void rejectsUnauthorizedCallbacksAndFansOutCancellation() {
        List<String> stopped = new ArrayList<>();
        CoordinatedRunService service = new CoordinatedRunService(
            new NoopCoordinatorController(),
            new RemoteRunnerClient() {
                @Override
                public void start(
                    String modelLibraryUrl, String modelId, String runId, String simulationId, String modelInstanceId,
                    String coordinatorId, com.fasterxml.jackson.databind.JsonNode initializationParameters,
                    KafkaConfigurationDto kafka, TimeModeConfigurationDto timeMode,
                    String callbackEndpoint, String callbackToken
                ) {
                }

                @Override
                public void stop(String modelLibraryUrl, String runId) {
                    stopped.add(modelLibraryUrl + ":" + runId);
                }
            },
            "https://helper.example"
        );
        service.createRun(new CreateCoordinatedRunRequest("run-2", "simulation-2", "coordinator-2", "coordinator-token"));
        service.registerRemoteRunner("run-2", registration("instance-2"));

        assertThrows(
            CoordinatorAuthorizationException.class,
            () -> service.reportRemoteRunnerEvent(
                "run-2", "instance-2", "wrong-token", event("run-2", "instance-2", "remote-runner-ready", "ready-2")
            )
        );
        assertEquals("canceled", service.cancelRun("run-2").status());
        assertEquals(List.of("https://model-library.example:run-2"), stopped);
    }

    private RegisterRemoteRunnerRequest registration(String instanceId) {
        return new RegisterRemoteRunnerRequest(
            "model-1",
            instanceId,
            "https://model-library.example",
            objectMapper.createObjectNode().put("example", true),
            new KafkaConfigurationDto("kafka.example:9092", "simulation-topic", "PLAINTEXT", null, null),
            new TimeModeConfigurationDto("virtual-time", null),
            "callback-token"
        );
    }

    private RemoteRunnerEventReport event(String runId, String instanceId, String eventType, String eventId) {
        return new RemoteRunnerEventReport(
            runId, instanceId, "coordinator-" + runId.substring(runId.length() - 1),
            eventId, eventType, "2026-01-01T00:00:00Z", null
        );
    }

    private record StartCall(
        String modelLibraryUrl,
        String modelId,
        String runId,
        String simulationId,
        String modelInstanceId,
        String coordinatorId,
        String topic,
        String callbackEndpoint,
        String callbackToken
    ) {
    }
}
