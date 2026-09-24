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

import iso.sim.server.dto.run.KafkaConfigurationDto;
import iso.sim.server.dto.run.SimulationContextDto;
import iso.sim.server.dto.run.StartModelRunRequest;
import iso.sim.server.dto.run.TimeMode;
import iso.sim.server.dto.run.TimeModeDto;
import iso.sim.server.executor.RunExecutionContext;
import iso.sim.server.executor.RunExecutor;
import iso.sim.server.runtime.NoopRunHandle;
import iso.sim.server.runtime.RunHandle;
import iso.sim.server.runtime.RunResourceRegistry;
import iso.sim.server.store.InMemoryRunStatusStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.Map;

class RunLifecycleManagerTest {

    @Test
    void startRunMarksReadyAfterStartingAndReadinessSuccess() {
        InMemoryRunStatusStore store = new InMemoryRunStatusStore();
        RunLifecycleService lifecycleService = new RunLifecycleService(store);
        lifecycleService.markAccepted("run-1", "model-1", "accepted");

        RunResourceRegistry registry = new RunResourceRegistry();
        RunExecutor executor = context -> new NoopRunHandle(context.getRunId());
        RunReadinessProbe readinessProbe = (context, handle) -> RunReadinessResult.ready("ready");
        RunTerminalCoordinator terminalCoordinator = new RunTerminalCoordinator(lifecycleService, store, registry);
        RunLifecycleManager manager = new RunLifecycleManager(executor, lifecycleService, registry, terminalCoordinator, context -> readinessProbe, (context, runHandle) -> runHandle);

        manager.startRun(context("run-1"));

        assertEquals("locally-ready", store.get("run-1").getStatus());
        assertNotNull(registry.get("run-1"));
    }

    @Test
    void startRunMarksFailedWhenExecutorThrows() {
        InMemoryRunStatusStore store = new InMemoryRunStatusStore();
        RunLifecycleService lifecycleService = new RunLifecycleService(store);
        lifecycleService.markAccepted("run-2", "model-1", "accepted");

        RunResourceRegistry registry = new RunResourceRegistry();
        RunExecutor executor = context -> {
            throw new IllegalStateException("boom");
        };
        RunTerminalCoordinator terminalCoordinator = new RunTerminalCoordinator(lifecycleService, store, registry);
        RunLifecycleManager manager = new RunLifecycleManager(executor, lifecycleService, registry, terminalCoordinator, context -> new ImmediateReadinessProbe(), (context, handle) -> handle);

        manager.startRun(context("run-2"));

        assertEquals("locally-failed", store.get("run-2").getStatus());
        assertNull(registry.get("run-2"));
    }

    @Test
    void startRunMarksFailedAndStopsResourcesWhenReadinessFails() {
        InMemoryRunStatusStore store = new InMemoryRunStatusStore();
        RunLifecycleService lifecycleService = new RunLifecycleService(store);
        lifecycleService.markAccepted("run-3", "model-1", "accepted");

        RunResourceRegistry registry = new RunResourceRegistry();
        RecordingHandle handle = new RecordingHandle("run-3");
        RunExecutor executor = context -> handle;
        RunReadinessProbe readinessProbe = (context, runHandle) -> RunReadinessResult.notReady("probe failed");
        RunTerminalCoordinator terminalCoordinator = new RunTerminalCoordinator(lifecycleService, store, registry);
        RunLifecycleManager manager = new RunLifecycleManager(executor, lifecycleService, registry, terminalCoordinator, context -> readinessProbe, (context, runHandle2) -> runHandle2);

        manager.startRun(context("run-3"));

        assertEquals("locally-failed", store.get("run-3").getStatus());
        assertEquals(1, handle.stopCount);
        assertNull(registry.get("run-3"));
    }

    private static RunExecutionContext context(String runId) {
        StartModelRunRequest request = new StartModelRunRequest(
            runId,
            null,
            new KafkaConfigurationDto("topic", Map.of("bootstrap.servers", "kafka:9092")),
            new SimulationContextDto("simulation-1", "instance-1", "coordinator-1", new TimeModeDto(TimeMode.VIRTUAL_TIME)),
            null
        );
        return new RunExecutionContext(runId, "model-1", request);
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
            return true;
        }

        @Override
        public void stop() {
            stopCount++;
        }
    }
}
