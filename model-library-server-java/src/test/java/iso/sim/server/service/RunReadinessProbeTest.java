package iso.sim.server.service;

import iso.sim.server.dto.run.KafkaConfigurationDto;
import iso.sim.server.dto.run.StartModelRunRequest;
import iso.sim.server.executor.RunExecutionContext;
import iso.sim.server.runtime.RunHandle;
import org.junit.jupiter.api.Test;

import java.time.Duration;

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
            new KafkaConfigurationDto("kafka:9092", "topic", null, null, null),
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
