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
import iso.sim.server.dto.run.CurrentSimulationTimeDto;
import iso.sim.server.dto.run.KafkaConfigurationDto;
import iso.sim.server.dto.run.RunStatusResponse;
import iso.sim.server.dto.run.SimulationContextDto;
import iso.sim.server.dto.run.StartModelRunRequest;
import iso.sim.server.dto.run.TimeMode;
import iso.sim.server.dto.run.TimeModeDto;
import iso.sim.server.executor.RunExecutionContext;
import iso.sim.server.runtime.RunHandle;
import iso.sim.server.runtime.RunResourceRegistry;
import iso.sim.server.store.InMemoryRunStatusStore;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaIsoRunMonitorTest {

    @Test
    void monitorUpdatesCurrentSimulationTimeAndPreventsRollback() throws Exception {
        InMemoryRunStatusStore store = new InMemoryRunStatusStore();
        RunLifecycleService lifecycleService = new RunLifecycleService(store);
        RunResourceRegistry registry = new RunResourceRegistry();
        RunTerminalCoordinator terminalCoordinator = new RunTerminalCoordinator(lifecycleService, store, registry);
        lifecycleService.markAccepted("run-1", "model-1", "accepted");
        RecordingHandle runtimeHandle = new RecordingHandle("run-1");
        registry.register("run-1", runtimeHandle);

        QueueConsumerAdapter consumer = new QueueConsumerAdapter();
        KafkaIsoRunMonitor monitor = new KafkaIsoRunMonitor(
            lifecycleService,
            store,
            terminalCoordinator,
            new Iso21175MessageParser(new ObjectMapper()),
            (context, topic, group) -> consumer,
            Runnable::run
        );

        StartModelRunRequest request = request("run-1", new TimeModeDto(TimeMode.VIRTUAL_TIME));
        RunExecutionContext context = new RunExecutionContext("run-1", "model-1", request);

        consumer.offer("""
            {"simulationRunId":"run-1","messageId":"msg-1","messageType":"NextInternalTimeReport","eventTime":120.0,"nextInternalTime":125.0}
            """);
        consumer.offer("""
            {"simulationRunId":"other-run","messageId":"msg-x","messageType":"NextInternalTimeReport","nextInternalTime":999.0}
            """);
        consumer.offer("""
            {"simulationRunId":"run-1","messageId":"msg-2","messageType":"ModelRunning","eventTime":124.0}
            """);
        consumer.offer("""
            {"simulationRunId":"run-1","messageId":"msg-3","messageType":"ModelRunning","eventTime":130.0}
            """);
        consumer.offer("""
            {"simulationRunId":"run-1","messageId":"msg-4","messageType":"ModelTerminated","eventTime":131.0}
            """);

        monitor.startMonitoring(context, runtimeHandle);

        RunStatusResponse status = store.get("run-1");
        assertEquals("completed", status.getStatus());
        CurrentSimulationTimeDto currentSimulationTime = status.getCurrentSimulationTime();
        assertNotNull(currentSimulationTime);
        assertEquals(new BigDecimal("131.0"), currentSimulationTime.getValue());
        assertEquals("ModelTerminated", currentSimulationTime.getSourceMessageType());
        assertEquals("msg-4", currentSimulationTime.getSourceMessageId());
        assertNotNull(currentSimulationTime.getUpdatedAt());
        assertEquals(1, runtimeHandle.stopCount);
        assertNull(registry.get("run-1"));
    }

    @Test
    void monitorIgnoresWrongSimulationRunId() throws Exception {
        InMemoryRunStatusStore store = new InMemoryRunStatusStore();
        RunLifecycleService lifecycleService = new RunLifecycleService(store);
        RunTerminalCoordinator terminalCoordinator = new RunTerminalCoordinator(lifecycleService, store, new RunResourceRegistry());
        lifecycleService.markAccepted("run-2", "model-1", "accepted");

        QueueConsumerAdapter consumer = new QueueConsumerAdapter();
        consumer.offer("""
            {"simulationRunId":"other-run","messageId":"msg-x","messageType":"NextInternalTimeReport","nextInternalTime":"125.0"}
            """);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            KafkaIsoRunMonitor monitor = new KafkaIsoRunMonitor(
                lifecycleService,
                store,
                terminalCoordinator,
                new Iso21175MessageParser(new ObjectMapper()),
                (context, topic, group) -> consumer,
                executor
            );

            RunHandle monitorHandle = monitor.startMonitoring(
                new RunExecutionContext("run-2", "model-1", request("run-2", null)),
                new NoopRunHandle("run-2")
            );

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
            while (!consumer.isEmpty() && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }

            assertNull(store.get("run-2").getCurrentSimulationTime());
            assertEquals("accepted", store.get("run-2").getStatus());
            monitorHandle.stop();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void monitorIgnoresStringEncodedLogicalTime() {
        InMemoryRunStatusStore store = new InMemoryRunStatusStore();
        RunLifecycleService lifecycleService = new RunLifecycleService(store);
        RunTerminalCoordinator terminalCoordinator = new RunTerminalCoordinator(lifecycleService, store, new RunResourceRegistry());
        lifecycleService.markAccepted("run-numeric", "model-1", "accepted");

        QueueConsumerAdapter consumer = new QueueConsumerAdapter();
        consumer.offer("""
            {"simulationRunId":"run-numeric","messageId":"msg-1","messageType":"ModelRunning","eventTime":"125.0"}
            """);
        consumer.offer("""
            {"simulationRunId":"run-numeric","messageId":"msg-2","messageType":"ModelTerminated"}
            """);

        KafkaIsoRunMonitor monitor = new KafkaIsoRunMonitor(
            lifecycleService,
            store,
            terminalCoordinator,
            new Iso21175MessageParser(new ObjectMapper()),
            (context, topic, group) -> consumer,
            Runnable::run
        );

        monitor.startMonitoring(
            new RunExecutionContext("run-numeric", "model-1", request("run-numeric", null)),
            new NoopRunHandle("run-numeric")
        );

        assertNull(store.get("run-numeric").getCurrentSimulationTime());
    }

    @Test
    void errorReportWithFatalSeverityMarksFailedAndCleansUpResources() {
        InMemoryRunStatusStore store = new InMemoryRunStatusStore();
        RunLifecycleService lifecycleService = new RunLifecycleService(store);
        RunResourceRegistry registry = new RunResourceRegistry();
        RunTerminalCoordinator terminalCoordinator = new RunTerminalCoordinator(lifecycleService, store, registry);
        lifecycleService.markAccepted("run-3", "model-1", "accepted");
        RecordingHandle runtimeHandle = new RecordingHandle("run-3");
        registry.register("run-3", runtimeHandle);

        QueueConsumerAdapter consumer = new QueueConsumerAdapter();
        consumer.offer("""
            {"simulationRunId":"run-3","messageId":"msg-1","messageType":"ErrorReport","payload":{"severity":"fatal"}}
            """);

        KafkaIsoRunMonitor monitor = new KafkaIsoRunMonitor(
            lifecycleService,
            store,
            terminalCoordinator,
            new Iso21175MessageParser(new ObjectMapper()),
            (context, topic, group) -> consumer,
            Runnable::run
        );

        monitor.startMonitoring(new RunExecutionContext("run-3", "model-1", request("run-3", null)), runtimeHandle);

        assertEquals("failed", store.get("run-3").getStatus());
        assertEquals(1, runtimeHandle.stopCount);
        assertNull(registry.get("run-3"));
    }

    @Test
    void monitorPollingExceptionMarksFailedAndCleansUpResources() {
        InMemoryRunStatusStore store = new InMemoryRunStatusStore();
        RunLifecycleService lifecycleService = new RunLifecycleService(store);
        RunResourceRegistry registry = new RunResourceRegistry();
        RunTerminalCoordinator terminalCoordinator = new RunTerminalCoordinator(lifecycleService, store, registry);
        lifecycleService.markAccepted("run-4", "model-1", "accepted");
        RecordingHandle runtimeHandle = new RecordingHandle("run-4");
        registry.register("run-4", runtimeHandle);

        KafkaIsoRunMonitor monitor = new KafkaIsoRunMonitor(
            lifecycleService,
            store,
            terminalCoordinator,
            new Iso21175MessageParser(new ObjectMapper()),
            (context, topic, group) -> new ThrowingConsumerAdapter(),
            Runnable::run
        );

        monitor.startMonitoring(new RunExecutionContext("run-4", "model-1", request("run-4", null)), runtimeHandle);

        assertEquals("failed", store.get("run-4").getStatus());
        assertEquals(1, runtimeHandle.stopCount);
        assertNull(registry.get("run-4"));
    }

    @Test
    void duplicateTerminalEventsDoNotOverwriteTerminalStatus() {
        InMemoryRunStatusStore store = new InMemoryRunStatusStore();
        RunLifecycleService lifecycleService = new RunLifecycleService(store);
        RunResourceRegistry registry = new RunResourceRegistry();
        RunTerminalCoordinator terminalCoordinator = new RunTerminalCoordinator(lifecycleService, store, registry);
        lifecycleService.markAccepted("run-5", "model-1", "accepted");
        RecordingHandle runtimeHandle = new RecordingHandle("run-5");
        registry.register("run-5", runtimeHandle);

        QueueConsumerAdapter consumer = new QueueConsumerAdapter();
        consumer.offer("""
            {"simulationRunId":"run-5","messageId":"msg-1","messageType":"ModelTerminated"}
            """);
        consumer.offer("""
            {"simulationRunId":"run-5","messageId":"msg-2","messageType":"ErrorReport","payload":{"severity":"error"}}
            """);

        KafkaIsoRunMonitor monitor = new KafkaIsoRunMonitor(
            lifecycleService,
            store,
            terminalCoordinator,
            new Iso21175MessageParser(new ObjectMapper()),
            (context, topic, group) -> consumer,
            Runnable::run
        );

        monitor.startMonitoring(new RunExecutionContext("run-5", "model-1", request("run-5", null)), runtimeHandle);

        assertEquals("completed", store.get("run-5").getStatus());
        assertTrue(runtimeHandle.stopCount >= 1);
    }

    @Test
    void monitorUsesTopicFromRunExecutionContext() {
        InMemoryRunStatusStore store = new InMemoryRunStatusStore();
        RunLifecycleService lifecycleService = new RunLifecycleService(store);
        RunTerminalCoordinator terminalCoordinator = new RunTerminalCoordinator(lifecycleService, store, new RunResourceRegistry());
        lifecycleService.markAccepted("run-topic", "model-1", "accepted");

        AtomicReference<String> capturedTopic = new AtomicReference<>();
        AtomicReference<String> capturedConsumerGroup = new AtomicReference<>();
        QueueConsumerAdapter consumer = new QueueConsumerAdapter();
        KafkaIsoRunMonitor monitor = new KafkaIsoRunMonitor(
            lifecycleService,
            store,
            terminalCoordinator,
            new Iso21175MessageParser(new ObjectMapper()),
            (context, topic, group) -> {
                capturedTopic.set(topic);
                capturedConsumerGroup.set(group);
                return consumer;
            },
            runnable -> {
            }
        );

        StartModelRunRequest request = new StartModelRunRequest(
            "run-topic",
            null,
            new KafkaConfigurationDto("kafka:9092", "mission-system", null, null, null),
            new SimulationContextDto("sim-1", "receiver-1", "coord-1", null)
        );

        monitor.startMonitoring(
            new RunExecutionContext("run-topic", "model-1", request),
            new NoopRunHandle("run-topic")
        );
        assertEquals("mission-system", capturedTopic.get());
        assertEquals("run-topic:receiver-1", capturedConsumerGroup.get());
    }

    private static StartModelRunRequest request(String runId, TimeModeDto timeModeDto) {
        return new StartModelRunRequest(
            runId,
            null,
            new KafkaConfigurationDto("kafka:9092", "topic", null, null, null),
            new SimulationContextDto("sim-1", "instance-1", "coord-1", timeModeDto)
        );
    }

    private static class QueueConsumerAdapter implements KafkaConsumerAdapter {
        private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

        @Override
        public List<String> poll(Duration timeout) {
            String value = queue.poll();
            return value == null ? List.of() : List.of(value);
        }

        @Override
        public void close() {
        }

        void offer(String value) {
            queue.offer(value);
        }

        boolean isEmpty() {
            return queue.isEmpty();
        }
    }

    private static class NoopRunHandle implements RunHandle {
        private final String runId;

        private NoopRunHandle(String runId) {
            this.runId = runId;
        }

        @Override
        public String runId() {
            return runId;
        }

        @Override
        public boolean isAlive() {
            return true;
        }

        @Override
        public void stop() {
        }
    }

    private static class ThrowingConsumerAdapter implements KafkaConsumerAdapter {
        @Override
        public List<String> poll(Duration timeout) {
            throw new IllegalStateException("boom");
        }

        @Override
        public void close() {
        }
    }

    private static class RecordingHandle implements RunHandle {
        private final String runId;
        private int stopCount;

        private RecordingHandle(String runId) {
            this.runId = runId;
        }

        @Override
        public String runId() {
            return runId;
        }

        @Override
        public boolean isAlive() {
            return stopCount == 0;
        }

        @Override
        public void stop() {
            stopCount++;
        }
    }
}
