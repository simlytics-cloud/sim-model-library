package iso.sim.server.service;

import iso.sim.server.dto.run.Iso21175Message;
import iso.sim.server.dto.run.CurrentSimulationTimeDto;
import iso.sim.server.dto.run.RunStatusResponse;
import iso.sim.server.dto.run.TimeModeDto;
import iso.sim.server.executor.RunExecutionContext;
import iso.sim.server.runtime.RunHandle;
import iso.sim.server.store.RunStatusStore;

import java.time.Instant;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public class KafkaIsoRunMonitor implements RunMonitor {
    private static final Duration POLL_INTERVAL = Duration.ofMillis(250);

    private final RunLifecycleService runLifecycleService;
    private final RunStatusStore runStatusStore;
    private final RunTerminalCoordinator runTerminalCoordinator;
    private final Iso21175MessageParser parser;
    private final KafkaConsumerAdapterFactory consumerFactory;
    private final Executor executor;

    public KafkaIsoRunMonitor(
        RunLifecycleService runLifecycleService,
        RunStatusStore runStatusStore,
        RunTerminalCoordinator runTerminalCoordinator,
        Iso21175MessageParser parser,
        KafkaConsumerAdapterFactory consumerFactory,
        Executor executor
    ) {
        this.runLifecycleService = runLifecycleService;
        this.runStatusStore = runStatusStore;
        this.runTerminalCoordinator = runTerminalCoordinator;
        this.parser = parser;
        this.consumerFactory = consumerFactory;
        this.executor = executor;
    }

    @Override
    public RunHandle startMonitoring(RunExecutionContext context, RunHandle runtimeHandle) {
        String runId = context.getRunId();
        String topic = context.getRequest().getKafka().getTopic();
        String configuredGroup = context.getRequest().getKafka().getConsumerGroup();
        String consumerGroup = configuredGroup != null && !configuredGroup.isBlank()
            ? configuredGroup
            : "model-library-run-monitor-" + runId;

        KafkaConsumerAdapter consumer = consumerFactory.create(context, topic, consumerGroup);
        AtomicBoolean active = new AtomicBoolean(true);

        executor.execute(() -> monitorLoop(context, runtimeHandle, consumer, active));

        return new RunHandle() {
            @Override
            public String runId() {
                return runId;
            }

            @Override
            public boolean isAlive() {
                return active.get();
            }

            @Override
            public void stop() {
                if (active.compareAndSet(true, false)) {
                    consumer.close();
                }
            }
        };
    }

    private void monitorLoop(RunExecutionContext context, RunHandle runtimeHandle, KafkaConsumerAdapter consumer, AtomicBoolean active) {
        String runId = context.getRunId();
        try {
            while (active.get()) {
                for (String rawValue : consumer.poll(POLL_INTERVAL)) {
                    if (!active.get()) {
                        break;
                    }
                    Optional<Iso21175Message> maybeMessage = parser.parse(rawValue);
                    if (maybeMessage.isEmpty()) {
                        continue;
                    }
                    Iso21175Message message = maybeMessage.get();
                    if (!runId.equals(message.getSimulationRunId())) {
                        continue;
                    }
                    updateCurrentSimulationTime(context, message);
                    if (handleLifecycleMessage(runId, message)) {
                        active.set(false);
                        consumer.close();
                        return;
                    }
                }
            }
        } catch (RuntimeException ex) {
            runTerminalCoordinator.failRun(runId, "Run monitor failed unexpectedly: " + ex.getMessage());
            active.set(false);
            consumer.close();
        }
    }

    private boolean handleLifecycleMessage(String runId, Iso21175Message message) {
        String messageType = message.getMessageType();
        if ("NextInternalTimeReport".equals(messageType)) {
            RunStatusResponse current = runStatusStore.get(runId);
            if (current != null && "ready".equals(current.getStatus())) {
                runLifecycleService.markRunning(runId, "Simulation reported progress");
            }
            return false;
        }

        if ("ModelTerminated".equals(messageType)) {
            runTerminalCoordinator.completeRun(runId, "Simulation terminated");
            return true;
        }

        if ("ErrorReport".equals(messageType) && isErrorOrFatal(message)) {
            runTerminalCoordinator.failRun(runId, "Simulation reported error");
            return true;
        }

        return false;
    }

    private void updateCurrentSimulationTime(RunExecutionContext context, Iso21175Message message) {
        Optional<Double> maybeValue = resolveCurrentSimulationTimeValue(message);
        if (maybeValue.isEmpty()) {
            return;
        }

        String runId = context.getRunId();
        RunStatusResponse current = runStatusStore.get(runId);
        if (current == null) {
            return;
        }

        Double value = maybeValue.get();
        CurrentSimulationTimeDto existing = current.getCurrentSimulationTime();
        if (existing != null && existing.getValue() != null && value < existing.getValue()) {
            return;
        }

        TimeModeDto timeMode = context.getRequest().getSimulation() == null
            ? null
            : context.getRequest().getSimulation().getTimeMode();

        CurrentSimulationTimeDto next = new CurrentSimulationTimeDto(
            value,
            timeMode == null ? null : timeMode.getTimeType(),
            timeMode == null ? null : timeMode.getSecondsPerSimulationTimeUnit(),
            message.getMessageType(),
            message.getMessageId(),
            Instant.now().toString()
        );

        runStatusStore.save(new RunStatusResponse(
            current.getRunId(),
            current.getModelId(),
            current.getStatus(),
            current.getAcceptedAt(),
            current.getReadyAt(),
            current.getStartedAt(),
            current.getCompletedAt(),
            current.getMessage(),
            next
        ));
    }

    private Optional<Double> resolveCurrentSimulationTimeValue(Iso21175Message message) {
        Optional<Double> nextInternalTime = parseDouble(message.getNextInternalTime());
        if (nextInternalTime.isPresent()) {
            return nextInternalTime;
        }
        return parseDouble(message.getEventTime());
    }

    private Optional<Double> parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private boolean isErrorOrFatal(Iso21175Message message) {
        if (message.getPayload() == null || message.getPayload().get("severity") == null) {
            return false;
        }
        String severity = message.getPayload().get("severity").asText();
        String normalized = severity.toLowerCase(Locale.ROOT);
        return "error".equals(normalized) || "fatal".equals(normalized);
    }
}
