/*
 * Sim Model Library Copyright (C) 2026 simlytics.cloud LLC and
 * Sim Model Library contributors.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package iso.sim.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import iso.sim.server.dto.run.KafkaConfigurationDto;
import iso.sim.server.dto.run.SimulationContextDto;
import iso.sim.server.dto.run.StartModelRunRequest;
import iso.sim.server.dto.run.TimeMode;
import iso.sim.server.dto.run.TimeModeDto;
import iso.sim.server.executor.RunExecutionContext;
import iso.sim.server.runtime.NoopRunHandle;
import iso.sim.server.runtime.RunResourceRegistry;
import iso.sim.server.store.InMemoryRunStatusStore;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkaIsoRunMonitorTest {
    @Test
    void filtersXRunIdBeforePayloadParsingAndReportsOnlyLocalStop() {
        InMemoryRunStatusStore store = new InMemoryRunStatusStore();
        RunLifecycleService lifecycle = new RunLifecycleService(store);
        lifecycle.markAccepted("run-1", "model", "accepted");
        lifecycle.markLocallyReady("run-1", "ready");
        AtomicReference<RemoteRunnerEventType> reported = new AtomicReference<>();
        KafkaConsumerAdapter consumer = new FixedConsumer(List.of(
            new KafkaConsumerRecord("other-run", "{not json", Map.of("X-Run-Id", bytes("other-run"))),
            new KafkaConsumerRecord("run-1", """
                {"simulationRunId":"run-1","messageId":"done","messageType":"ModelTerminated"}
                """, Map.of("X-Run-Id", bytes("run-1")))
        ));
        KafkaIsoRunMonitor monitor = new KafkaIsoRunMonitor(
            lifecycle,
            store,
            new RunTerminalCoordinator(lifecycle, store, new RunResourceRegistry()),
            new Iso21175MessageParser(new ObjectMapper()),
            (context, topic, group) -> consumer,
            Runnable::run,
            (context, eventType, detail) -> reported.set(eventType)
        );

        monitor.startMonitoring(context("run-1"), new NoopRunHandle("run-1"));

        assertEquals("locally-stopped", store.get("run-1").getStatus());
        assertEquals(RemoteRunnerEventType.REMOTE_RUNNER_STOPPED, reported.get());
    }

    @Test
    void derivesConsumerGroupFromRequiredLocalReceiverIdentity() {
        InMemoryRunStatusStore store = new InMemoryRunStatusStore();
        RunLifecycleService lifecycle = new RunLifecycleService(store);
        lifecycle.markAccepted("run-2", "model", "accepted");
        AtomicReference<String> group = new AtomicReference<>();
        KafkaIsoRunMonitor monitor = new KafkaIsoRunMonitor(
            lifecycle,
            store,
            new RunTerminalCoordinator(lifecycle, store, new RunResourceRegistry()),
            new Iso21175MessageParser(new ObjectMapper()),
            (context, topic, consumerGroup) -> {
                group.set(consumerGroup);
                return new FixedConsumer(List.of());
            },
            command -> { },
            (context, eventType, detail) -> { }
        );

        monitor.startMonitoring(context("run-2"), new NoopRunHandle("run-2"));

        assertEquals("run-2:instance-1", group.get());
    }

    private static RunExecutionContext context(String runId) {
        return new RunExecutionContext(runId, "model", new StartModelRunRequest(
            runId,
            new ObjectMapper().createObjectNode(),
            new KafkaConfigurationDto("topic", Map.of("bootstrap.servers", "kafka:9092")),
            new SimulationContextDto("simulation-1", "instance-1", "coordinator-1", new TimeModeDto(TimeMode.VIRTUAL_TIME)),
            null
        ));
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static final class FixedConsumer implements KafkaConsumerAdapter {
        private List<KafkaConsumerRecord> records;

        private FixedConsumer(List<KafkaConsumerRecord> records) {
            this.records = records;
        }

        @Override
        public List<KafkaConsumerRecord> poll(Duration timeout) {
            List<KafkaConsumerRecord> polled = records;
            records = List.of();
            return polled;
        }

        @Override
        public void close() {
        }
    }
}
