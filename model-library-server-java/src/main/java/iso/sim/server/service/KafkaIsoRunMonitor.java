package iso.sim.server.service;

import iso.sim.server.dto.run.Iso21175Message;
import iso.sim.server.dto.run.CurrentSimulationTimeDto;
import iso.sim.server.dto.run.RunStatusResponse;
import iso.sim.server.dto.run.TimeModeDto;
import iso.sim.server.executor.RunExecutionContext;
import iso.sim.server.runtime.RunHandle;
import iso.sim.server.store.RunStatusStore;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KafkaIsoRunMonitor implements RunMonitor {
    private static final Duration POLL_INTERVAL = Duration.ofMillis(250);
    private static final Logger logger = Logger.getLogger(KafkaIsoRunMonitor.class.getName());

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

        logger.info(() -> "Starting Kafka run monitor for runId=" + runId
            + ", topic=" + topic
            + ", consumerGroup=" + consumerGroup);

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
                    logger.info(() -> "Stopping Kafka run monitor for runId=" + runId);
                    consumer.close();
                }
            }
        };
    }

    private void monitorLoop(RunExecutionContext context, RunHandle runtimeHandle, KafkaConsumerAdapter consumer, AtomicBoolean active) {
        String runId = context.getRunId();
        try {
            while (active.get()) {
                var messages = consumer.poll(POLL_INTERVAL);
                if (!messages.isEmpty()) {
                    logger.fine(() -> "Polled " + messages.size() + " Kafka message(s) for runId=" + runId);
                }
                for (String rawValue : messages) {
                    if (!active.get()) {
                        // break;
                    }
                    Optional<Iso21175Message> maybeMessage = parser.parse(rawValue);
                    if (maybeMessage.isEmpty()) {
                        logger.fine(() -> "Ignoring Kafka message for runId=" + runId + " because parsing/validation failed");
                        continue;
                    }
                    Iso21175Message message = maybeMessage.get();
                    if (!runId.equals(message.getSimulationRunId())) {
                        logger.fine(() -> "Ignoring Kafka message with simulationRunId=" + message.getSimulationRunId()
                            + " while monitoring runId=" + runId
                            + ", messageType=" + message.getMessageType());
                        //continue;
                    }
                    logger.fine(() -> "Processing Kafka message for runId=" + runId
                        + ", messageType=" + message.getMessageType()
                        + ", messageId=" + message.getMessageId());
                    updateCurrentSimulationTime(context, message);
                    if (handleLifecycleMessage(runId, message)) {
                        active.set(false);
                        logger.info(() -> "Stopping Kafka monitor loop after terminal lifecycle event for runId=" + runId
                            + ", messageType=" + message.getMessageType());
                        consumer.close();
                        return;
                    }
                }
            }
        } catch (RuntimeException ex) {
            logger.log(Level.SEVERE, "Kafka monitor loop failed for runId=" + runId + ": " + ex.getMessage(), ex);
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
                logger.info(() -> "Marking run as running from messageType=NextInternalTimeReport for runId=" + runId
                    + ", messageId=" + message.getMessageId());
                runLifecycleService.markRunning(runId, "Simulation reported progress");
            } else {
                logger.fine(() -> "Received NextInternalTimeReport but run state is not ready for runId=" + runId
                    + ", currentStatus=" + (current == null ? "null" : current.getStatus()));
            }
            return false;
        }

        if ("ModelTerminated".equals(messageType)) {
            logger.info(() -> "Received ModelTerminated for runId=" + runId
                + ", messageId=" + message.getMessageId());
            runTerminalCoordinator.completeRun(runId, "Simulation terminated");
            return true;
        }

        if ("ErrorReport".equals(messageType) && isErrorOrFatal(message)) {
            logger.warning(() -> "Received ErrorReport with terminal severity for runId=" + runId
                + ", messageId=" + message.getMessageId());
            runTerminalCoordinator.failRun(runId, "Simulation reported error");
            return true;
        }

        logger.fine(() -> "Ignoring non-lifecycle Kafka message for runId=" + runId
            + ", messageType=" + messageType
            + ", messageId=" + message.getMessageId());
        return false;
    }

    private void updateCurrentSimulationTime(RunExecutionContext context, Iso21175Message message) {
        Optional<BigDecimal> maybeValue = resolveCurrentSimulationTimeValue(message);
        if (maybeValue.isEmpty()) {
            return;
        }

        String runId = context.getRunId();
        RunStatusResponse current = runStatusStore.get(runId);
        if (current == null) {
            return;
        }

        BigDecimal value = maybeValue.get();
        CurrentSimulationTimeDto existing = current.getCurrentSimulationTime();
        if (existing != null && existing.getValue() != null) {
            Optional<BigDecimal> existingValue = parseLogicalTime(existing.getValue());
            if (existingValue.isPresent() && value.compareTo(existingValue.get()) < 0) {
                logger.fine(() -> "Ignoring simulation time rollback for runId=" + runId
                    + ", incomingValue=" + value.toPlainString()
                    + ", existingValue=" + existing.getValue());
                return;
            }
        }

        TimeModeDto timeMode = context.getRequest().getSimulation() == null
            ? null
            : context.getRequest().getSimulation().getTimeMode();

        CurrentSimulationTimeDto next = new CurrentSimulationTimeDto(
            formatLogicalTime(value),
            timeMode == null ? null : timeMode.getTimeSemantics(),
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
        logger.fine(() -> "Updated currentSimulationTime for runId=" + runId
            + ", value=" + value.toPlainString()
            + ", sourceMessageType=" + message.getMessageType()
            + ", sourceMessageId=" + message.getMessageId());
    }

    private Optional<BigDecimal> resolveCurrentSimulationTimeValue(Iso21175Message message) {
        Optional<BigDecimal> nextInternalTime = parseLogicalTime(message.getNextInternalTime());
        if (nextInternalTime.isPresent()) {
            return nextInternalTime;
        }
        return parseLogicalTime(message.getEventTime());
    }

    private Optional<BigDecimal> parseLogicalTime(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(value.trim()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private String formatLogicalTime(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
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
