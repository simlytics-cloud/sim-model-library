package iso.sim.coordinator.helper;

import iso.sim.coordinator.helper.dto.CoordinatedRunStatusResponse;
import iso.sim.coordinator.helper.dto.CoordinatorEventReport;
import iso.sim.coordinator.helper.dto.CreateCoordinatedRunRequest;
import iso.sim.coordinator.helper.dto.RegisterRemoteRunnerRequest;
import iso.sim.coordinator.helper.dto.RemoteRunnerEventReport;
import iso.sim.coordinator.helper.dto.RemoteRunnerStatusResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authoritative control-plane state for one coordinator and its registered remote runners.
 * Simulation traffic remains exclusively on the externally owned Kafka protocol.
 */
public class CoordinatedRunService {
    private static final Set<String> REMOTE_RUNNER_EVENTS = Set.of(
        "remote-runner-accepted",
        "remote-runner-starting",
        "remote-runner-ready",
        "remote-runner-stopped",
        "remote-runner-failed"
    );
    private static final Set<String> COORDINATOR_EVENTS = Set.of("progress", "completed", "failed");
    private static final Set<String> TERMINAL_STATUSES = Set.of("completed", "failed", "canceled");

    private final Map<String, MutableRun> runs = new ConcurrentHashMap<>();
    private final CoordinatorController coordinatorController;
    private final RemoteRunnerClient remoteRunnerClient;
    private final String callbackEndpoint;

    public CoordinatedRunService(String callbackEndpoint) {
        this(new NoopCoordinatorController(), new HttpRemoteRunnerClient(new com.fasterxml.jackson.databind.ObjectMapper()), callbackEndpoint);
    }

    public CoordinatedRunService(
        CoordinatorController coordinatorController,
        RemoteRunnerClient remoteRunnerClient,
        String callbackEndpoint
    ) {
        requireText(callbackEndpoint, "A coordinator-helper callback endpoint is required");
        this.coordinatorController = coordinatorController;
        this.remoteRunnerClient = remoteRunnerClient;
        this.callbackEndpoint = trimTrailingSlash(callbackEndpoint);
    }

    public CoordinatedRunStatusResponse createRun(CreateCoordinatedRunRequest request) {
        if (request == null) {
            throw new CoordinatedRunRequestException("Request body is required");
        }
        requireText(request.runId(), "'runId' is required");
        requireText(request.simulationId(), "'simulationId' is required");
        requireText(request.coordinatorId(), "'coordinatorId' is required");
        requireText(request.coordinatorToken(), "'coordinatorToken' is required");
        MutableRun created = new MutableRun(request, now());
        if (runs.putIfAbsent(request.runId(), created) != null) {
            throw new CoordinatedRunConflictException("Coordinated runId '" + request.runId() + "' already exists and cannot be reused");
        }
        return created.snapshot();
    }

    public CoordinatedRunStatusResponse registerRemoteRunner(String runId, RegisterRemoteRunnerRequest request) {
        validateRegistration(request);
        MutableRun run = requireRun(runId);
        synchronized (run) {
            if (!"accepted".equals(run.status)) {
                throw new CoordinatedRunConflictException("Remote runners cannot be registered after a coordinated run has started");
            }
            RemoteRunner current = run.remoteRunners.get(request.modelInstanceId());
            if (current != null) {
                if (current.matches(request)) {
                    return run.snapshot();
                }
                throw new CoordinatedRunConflictException(
                    "modelInstanceId '" + request.modelInstanceId() + "' is already registered differently"
                );
            }
            run.remoteRunners.put(request.modelInstanceId(), new RemoteRunner(request));
            return run.snapshot();
        }
    }

    /**
     * Starts registered remote model runners. The coordinator is started only after every required
     * runner reports remote-runner-ready through the callback endpoint.
     */
    public CoordinatedRunStatusResponse startRun(String runId) {
        MutableRun run = requireRun(runId);
        List<RemoteRunner> remoteRunners;
        synchronized (run) {
            if (!"accepted".equals(run.status)) {
                throw new CoordinatedRunConflictException("A coordinated run can start only from accepted state");
            }
            if (run.remoteRunners.isEmpty()) {
                throw new CoordinatedRunConflictException("A coordinated run requires at least one registered remote runner");
            }
            transition(run, "starting", "Coordinator helper is starting registered remote runners");
            remoteRunners = List.copyOf(run.remoteRunners.values());
        }

        try {
            for (RemoteRunner remoteRunner : remoteRunners) {
                remoteRunnerClient.start(
                    remoteRunner.modelLibraryUrl,
                    remoteRunner.modelId,
                    run.runId,
                    run.simulationId,
                    remoteRunner.modelInstanceId,
                    run.coordinatorId,
                    remoteRunner.initializationParameters,
                    remoteRunner.kafka,
                    remoteRunner.timeMode,
                    callbackEndpoint,
                    remoteRunner.callbackToken
                );
            }
        } catch (RemoteRunnerControlException ex) {
            failAndCancel(run, "Could not start every remote runner: " + ex.getMessage());
        }
        return getRun(runId);
    }

    public CoordinatedRunStatusResponse reportRemoteRunnerEvent(
        String runId,
        String modelInstanceId,
        String callbackToken,
        RemoteRunnerEventReport report
    ) {
        MutableRun run = requireRun(runId);
        CoordinatedRunStatusResponse snapshot;
        boolean startCoordinator = false;
        boolean cancel = false;
        synchronized (run) {
            RemoteRunner remoteRunner = requireRemoteRunner(run, modelInstanceId);
            authorize(remoteRunner.callbackToken, callbackToken);
            validateRemoteRunnerReport(run, remoteRunner, report);
            if (!run.remoteRunnerEventIds.add(report.eventId())) {
                return run.snapshot();
            }
            remoteRunner.localStatus = report.eventType();
            remoteRunner.updatedAt = report.timestamp();
            remoteRunner.errorDetail = report.errorDetail();

            if ("remote-runner-failed".equals(report.eventType()) && !TERMINAL_STATUSES.contains(run.status)) {
                transition(run, "failed", report.errorDetail() == null
                    ? "A required remote runner failed" : report.errorDetail());
                cancel = true;
            } else if ("remote-runner-ready".equals(report.eventType())
                && "starting".equals(run.status) && allRemoteRunnersReady(run)) {
                transition(run, "ready", "All required remote runners are ready");
                startCoordinator = true;
            }
            snapshot = run.snapshot();
        }
        if (cancel) {
            cancelCoordinatorAndRemoteRunners(run, snapshot);
        } else if (startCoordinator) {
            try {
                coordinatorController.start(snapshot);
            } catch (RuntimeException ex) {
                failAndCancel(run, "Coordinator start failed: " + ex.getMessage());
            }
        }
        return getRun(runId);
    }

    public CoordinatedRunStatusResponse reportCoordinatorEvent(
        String runId,
        String coordinatorToken,
        CoordinatorEventReport report
    ) {
        MutableRun run = requireRun(runId);
        boolean cancel = false;
        synchronized (run) {
            authorize(run.coordinatorToken, coordinatorToken);
            validateCoordinatorReport(report);
            if (!run.coordinatorEventIds.add(report.eventId())) {
                return run.snapshot();
            }
            if ("progress".equals(report.eventType())) {
                if (!"ready".equals(run.status) && !"starting".equals(run.status)) {
                    throw new CoordinatedRunConflictException("Coordinator progress is valid only after remote runners are ready");
                }
                transition(run, "running", "Coordinator reported progress");
            } else if ("failed".equals(report.eventType()) && !TERMINAL_STATUSES.contains(run.status)) {
                transition(run, "failed", report.errorDetail() == null ? "Coordinator reported failure" : report.errorDetail());
                cancel = true;
            } else if ("completed".equals(report.eventType())) {
                if (!allRemoteRunnersStopped(run)) {
                    throw new CoordinatedRunConflictException(
                        "Coordinator completion requires every remote runner to report remote-runner-stopped"
                    );
                }
                transition(run, "completed", "Coordinator and required remote runners completed");
            }
        }
        if (cancel) {
            cancelCoordinatorAndRemoteRunners(run, getRun(runId));
        }
        return getRun(runId);
    }

    public CoordinatedRunStatusResponse cancelRun(String runId) {
        MutableRun run = requireRun(runId);
        CoordinatedRunStatusResponse snapshot;
        synchronized (run) {
            if (TERMINAL_STATUSES.contains(run.status)) {
                return run.snapshot();
            }
            transition(run, "canceled", "Coordinator helper canceled the coordinated execution");
            snapshot = run.snapshot();
        }
        cancelCoordinatorAndRemoteRunners(run, snapshot);
        return getRun(runId);
    }

    public CoordinatedRunStatusResponse getRun(String runId) {
        MutableRun run = requireRun(runId);
        synchronized (run) {
            return run.snapshot();
        }
    }

    private void failAndCancel(MutableRun run, String message) {
        CoordinatedRunStatusResponse snapshot;
        synchronized (run) {
            if (TERMINAL_STATUSES.contains(run.status)) {
                return;
            }
            transition(run, "failed", message);
            snapshot = run.snapshot();
        }
        cancelCoordinatorAndRemoteRunners(run, snapshot);
    }

    private void cancelCoordinatorAndRemoteRunners(MutableRun run, CoordinatedRunStatusResponse snapshot) {
        List<String> failures = new ArrayList<>();
        try {
            coordinatorController.cancel(snapshot);
        } catch (RuntimeException ex) {
            failures.add("coordinator");
        }
        for (RemoteRunner remoteRunner : run.remoteRunners.values()) {
            try {
                remoteRunnerClient.stop(remoteRunner.modelLibraryUrl, run.runId);
            } catch (RemoteRunnerControlException ex) {
                failures.add(remoteRunner.modelInstanceId);
            }
        }
        if (!failures.isEmpty()) {
            synchronized (run) {
                run.message = run.message + "; cleanup requests could not be delivered to " + String.join(", ", failures);
            }
        }
    }

    private MutableRun requireRun(String runId) {
        MutableRun run = runs.get(runId);
        if (run == null) {
            throw new CoordinatedRunNotFoundException(runId);
        }
        return run;
    }

    private RemoteRunner requireRemoteRunner(MutableRun run, String modelInstanceId) {
        RemoteRunner remoteRunner = run.remoteRunners.get(modelInstanceId);
        if (remoteRunner == null) {
            throw new CoordinatedRunRequestException(
                "modelInstanceId '" + modelInstanceId + "' is not registered for this coordinated run"
            );
        }
        return remoteRunner;
    }

    private void validateRegistration(RegisterRemoteRunnerRequest request) {
        if (request == null) {
            throw new CoordinatedRunRequestException("Request body is required");
        }
        requireText(request.modelId(), "'modelId' is required");
        requireText(request.modelInstanceId(), "'modelInstanceId' is required");
        requireText(request.modelLibraryUrl(), "'modelLibraryUrl' is required");
        if (request.initializationParameters() == null) {
            throw new CoordinatedRunRequestException("'initializationParameters' is required");
        }
        if (request.kafka() == null || request.kafka().bootstrapServers() == null || request.kafka().bootstrapServers().isBlank()) {
            throw new CoordinatedRunRequestException("'kafka.bootstrapServers' is required");
        }
        if (request.kafka().topic() == null || request.kafka().topic().isBlank()) {
            throw new CoordinatedRunRequestException("'kafka.topic' is required");
        }
        if (request.timeMode() == null || request.timeMode().mode() == null || request.timeMode().mode().isBlank()) {
            throw new CoordinatedRunRequestException("'timeMode.mode' is required");
        }
        requireText(request.callbackToken(), "'callbackToken' is required");
    }

    private void validateRemoteRunnerReport(MutableRun run, RemoteRunner remoteRunner, RemoteRunnerEventReport report) {
        if (report == null) {
            throw new CoordinatedRunRequestException("Request body is required");
        }
        requireText(report.runId(), "'runId' is required");
        requireText(report.modelInstanceId(), "'modelInstanceId' is required");
        requireText(report.coordinatorId(), "'coordinatorId' is required");
        requireText(report.eventId(), "'eventId' is required");
        requireText(report.timestamp(), "'timestamp' is required");
        if (!run.runId.equals(report.runId()) || !remoteRunner.modelInstanceId.equals(report.modelInstanceId())) {
            throw new CoordinatedRunRequestException("Remote runner event identity does not match its URL");
        }
        if (!run.coordinatorId.equals(report.coordinatorId())) {
            throw new CoordinatedRunRequestException("Remote runner event coordinatorId does not match the coordinated run");
        }
        if (!REMOTE_RUNNER_EVENTS.contains(report.eventType())) {
            throw new CoordinatedRunRequestException("Unsupported remote runner eventType '" + report.eventType() + "'");
        }
    }

    private void validateCoordinatorReport(CoordinatorEventReport report) {
        if (report == null) {
            throw new CoordinatedRunRequestException("Request body is required");
        }
        requireText(report.eventId(), "'eventId' is required");
        requireText(report.timestamp(), "'timestamp' is required");
        if (!COORDINATOR_EVENTS.contains(report.eventType())) {
            throw new CoordinatedRunRequestException("'eventType' must be progress, completed, or failed");
        }
    }

    private boolean allRemoteRunnersReady(MutableRun run) {
        return !run.remoteRunners.isEmpty() && run.remoteRunners.values().stream()
            .allMatch(remoteRunner -> "remote-runner-ready".equals(remoteRunner.localStatus));
    }

    private boolean allRemoteRunnersStopped(MutableRun run) {
        return !run.remoteRunners.isEmpty() && run.remoteRunners.values().stream()
            .allMatch(remoteRunner -> "remote-runner-stopped".equals(remoteRunner.localStatus));
    }

    private void transition(MutableRun run, String status, String message) {
        run.status = status;
        run.message = message;
        String timestamp = now();
        if ("ready".equals(status)) {
            run.readyAt = timestamp;
        } else if ("starting".equals(status) || "running".equals(status)) {
            run.startedAt = timestamp;
        } else if (TERMINAL_STATUSES.contains(status)) {
            run.completedAt = timestamp;
        }
    }

    private void authorize(String expectedToken, String providedToken) {
        if (providedToken == null || !MessageDigest.isEqual(
            expectedToken.getBytes(StandardCharsets.UTF_8), providedToken.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new CoordinatorAuthorizationException();
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new CoordinatedRunRequestException(message);
        }
    }

    private String now() {
        return Instant.now().toString();
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static final class MutableRun {
        private final String runId;
        private final String simulationId;
        private final String coordinatorId;
        private final String coordinatorToken;
        private final Map<String, RemoteRunner> remoteRunners = new LinkedHashMap<>();
        private final Set<String> remoteRunnerEventIds = new LinkedHashSet<>();
        private final Set<String> coordinatorEventIds = new LinkedHashSet<>();
        private final String acceptedAt;
        private String status = "accepted";
        private String readyAt;
        private String startedAt;
        private String completedAt;
        private String message = "Coordinator helper accepted coordinated execution";

        private MutableRun(CreateCoordinatedRunRequest request, String acceptedAt) {
            this.runId = request.runId();
            this.simulationId = request.simulationId();
            this.coordinatorId = request.coordinatorId();
            this.coordinatorToken = request.coordinatorToken();
            this.acceptedAt = acceptedAt;
        }

        private CoordinatedRunStatusResponse snapshot() {
            List<RemoteRunnerStatusResponse> statuses = remoteRunners.values().stream()
                .map(RemoteRunner::snapshot)
                .toList();
            return new CoordinatedRunStatusResponse(
                runId, simulationId, coordinatorId, status, acceptedAt, readyAt, startedAt, completedAt, message, statuses
            );
        }
    }

    private static final class RemoteRunner {
        private final String modelId;
        private final String modelInstanceId;
        private final String modelLibraryUrl;
        private final com.fasterxml.jackson.databind.JsonNode initializationParameters;
        private final iso.sim.coordinator.helper.dto.KafkaConfigurationDto kafka;
        private final iso.sim.coordinator.helper.dto.TimeModeConfigurationDto timeMode;
        private final String callbackToken;
        private String localStatus = "registered";
        private String updatedAt;
        private String errorDetail;

        private RemoteRunner(RegisterRemoteRunnerRequest request) {
            this.modelId = request.modelId();
            this.modelInstanceId = request.modelInstanceId();
            this.modelLibraryUrl = request.modelLibraryUrl();
            this.initializationParameters = request.initializationParameters();
            this.kafka = request.kafka();
            this.timeMode = request.timeMode();
            this.callbackToken = request.callbackToken();
        }

        private boolean matches(RegisterRemoteRunnerRequest request) {
            return modelId.equals(request.modelId())
                && modelLibraryUrl.equals(request.modelLibraryUrl())
                && initializationParameters.equals(request.initializationParameters())
                && kafka.equals(request.kafka())
                && timeMode.equals(request.timeMode())
                && callbackToken.equals(request.callbackToken());
        }

        private RemoteRunnerStatusResponse snapshot() {
            return new RemoteRunnerStatusResponse(modelId, modelInstanceId, localStatus, updatedAt, errorDetail);
        }
    }
}
