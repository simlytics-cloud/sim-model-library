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

import iso.sim.server.dto.run.Iso21175Message;
import iso.sim.server.dto.run.CurrentSimulationTimeDto;
import iso.sim.server.dto.run.RunStatusResponse;
import iso.sim.server.executor.RunExecutionContext;
import iso.sim.server.runtime.RunHandle;
import iso.sim.server.store.RunStatusStore;
import com.fasterxml.jackson.databind.JsonNode;

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
    private final CoordinatorHelperCallbackReporter coordinatorHelperCallbackReporter;

    public KafkaIsoRunMonitor(
        RunLifecycleService runLifecycleService,
        RunStatusStore runStatusStore,
        RunTerminalCoordinator runTerminalCoordinator,
        Iso21175MessageParser parser,
        KafkaConsumerAdapterFactory consumerFactory,
        Executor executor,
        CoordinatorHelperCallbackReporter coordinatorHelperCallbackReporter
    ) {
        this.runLifecycleService = runLifecycleService;
        this.runStatusStore = runStatusStore;
        this.runTerminalCoordinator = runTerminalCoordinator;
        this.parser = parser;
        this.consumerFactory = consumerFactory;
        this.executor = executor;
        this.coordinatorHelperCallbackReporter = coordinatorHelperCallbackReporter;
    }

    @Override
    public RunHandle startMonitoring(RunExecutionContext context, RunHandle runtimeHandle) {
        String runId = context.getRunId();
        String topic = context.getRequest().getKafka().getTopic();
        String receiverId = context.getRequest().getSimulation().getModelInstanceId();
        String consumerGroup = runId + ":" + receiverId;

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
                for (KafkaConsumerRecord record : messages) {
                    if (!active.get()) {
                        break;
                    }
                    if (!runId.equals(record.headerValue("X-Run-Id"))) {
                        logger.fine(() -> "Ignoring Kafka record before parsing because X-Run-Id does not match runId=" + runId);
                        continue;
                    }
                    Optional<Iso21175Message> maybeMessage = parser.parse(record.value());
                    if (maybeMessage.isEmpty()) {
                        logger.fine(() -> "Ignoring Kafka message for runId=" + runId + " because parsing/validation failed");
                        continue;
                    }
                    Iso21175Message message = maybeMessage.get();
                    if (!runId.equals(message.getSimulationRunId())) {
                        logger.fine(() -> "Ignoring Kafka message with simulationRunId=" + message.getSimulationRunId()
                            + " while monitoring runId=" + runId
                            + ", messageType=" + message.getMessageType());
                        continue;
                    }
                    logger.fine(() -> "Processing Kafka message for runId=" + runId
                        + ", messageType=" + message.getMessageType()
                        + ", messageId=" + message.getMessageId());
                    updateCurrentSimulationTime(context, message);
                    if (handleLifecycleMessage(context, message)) {
                        active.set(false);
                        logger.info(() -> "Stopping Kafka monitor loop after terminal lifecycle event for runId=" + runId
                            + ", messageType=" + message.getMessageType());
                        consumer.close();
                        return;
                    }
                }
            }
        } catch (org.apache.kafka.common.KafkaException | IllegalStateException ex) {
            logger.log(Level.SEVERE, "Kafka monitor loop failed for runId=" + runId + ": " + ex.getMessage(), ex);
            runTerminalCoordinator.failRun(runId, "Run monitor failed unexpectedly: " + ex.getMessage());
            try {
                coordinatorHelperCallbackReporter.report(
                    context, RemoteRunnerEventType.REMOTE_RUNNER_FAILED, "Run monitor failed unexpectedly: " + ex.getMessage()
                );
            } catch (RuntimeException reportFailure) {
                logger.log(Level.WARNING, "Could not report monitor failure to coordinator helper for runId=" + runId, reportFailure);
            }
            active.set(false);
            consumer.close();
        }
    }

    private boolean handleLifecycleMessage(RunExecutionContext context, Iso21175Message message) {
        String runId = context.getRunId();
        String messageType = message.getMessageType();
        if ("NextInternalTimeReport".equals(messageType)) {
            logger.fine(() -> "Observed coordinator progress for runId=" + runId
                + "; coordinated status remains coordinator-helper-owned");
            return false;
        }

        if ("ModelTerminated".equals(messageType)) {
            logger.info(() -> "Received ModelTerminated for runId=" + runId
                + ", messageId=" + message.getMessageId());
            runTerminalCoordinator.completeRun(runId, "Local model runtime observed termination");
            coordinatorHelperCallbackReporter.report(context, RemoteRunnerEventType.REMOTE_RUNNER_STOPPED, null);
            return true;
        }

        if ("ErrorReport".equals(messageType) && isErrorOrFatal(message)) {
            logger.warning(() -> "Received ErrorReport with terminal severity for runId=" + runId
                + ", messageId=" + message.getMessageId());
            runTerminalCoordinator.failRun(runId, "Local model runtime observed error");
            coordinatorHelperCallbackReporter.report(
                context, RemoteRunnerEventType.REMOTE_RUNNER_FAILED, "Local model runtime observed error"
            );
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
            if (value.compareTo(existing.getValue()) < 0) {
                logger.fine(() -> "Ignoring simulation time rollback for runId=" + runId
                    + ", incomingValue=" + value.toPlainString()
                    + ", existingValue=" + existing.getValue());
                return;
            }
        }

        CurrentSimulationTimeDto next = new CurrentSimulationTimeDto(
            value,
            message.getMessageType(),
            message.getMessageId(),
            Instant.now().toString()
        );

        runStatusStore.save(new RunStatusResponse(
            current.getRunId(),
            current.getModelId(),
            current.getSimulationId(),
            current.getModelInstanceId(),
            current.getCoordinatorId(),
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

    private Optional<BigDecimal> parseLogicalTime(JsonNode value) {
        if (value == null || !value.isNumber()) {
            return Optional.empty();
        }
        return Optional.of(value.decimalValue());
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
