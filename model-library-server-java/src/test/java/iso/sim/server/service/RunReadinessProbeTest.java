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
import iso.sim.server.runtime.RunHandle;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunReadinessProbeTest {

    @Test
    void immediateProbeReturnsReady() {
        RunReadinessProbe probe = new ImmediateReadinessProbe();
        RunReadinessResult result = probe.awaitReady(context("run-ready"), new StaticHandle("run-ready", true));
        assertTrue(result.isReady());
    }

    @Test
    void processAliveProbeReturnsReadyWhenHandleAlive() {
        RunReadinessProbe probe = new ProcessAliveReadinessProbe(Duration.ZERO);
        RunReadinessResult result = probe.awaitReady(context("run-alive"), new StaticHandle("run-alive", true));
        assertTrue(result.isReady());
    }

    @Test
    void processAliveProbeReturnsNotReadyWhenHandleIsDead() {
        RunReadinessProbe probe = new ProcessAliveReadinessProbe(Duration.ZERO);
        RunReadinessResult result = probe.awaitReady(context("run-dead"), new StaticHandle("run-dead", false));
        assertFalse(result.isReady());
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

    private static class StaticHandle implements RunHandle {
        private final String runId;
        private final boolean alive;

        private StaticHandle(String runId, boolean alive) {
            this.runId = runId;
            this.alive = alive;
        }

        @Override
        public String runId() {
            return runId;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public void stop() {
            // no-op
        }
    }
}
