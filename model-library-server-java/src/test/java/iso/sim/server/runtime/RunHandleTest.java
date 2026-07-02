package iso.sim.server.runtime;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunHandleTest {

    @Test
    void noopHandleIsNotAliveAndStopDoesNothing() {
        NoopRunHandle handle = new NoopRunHandle("run-1");
        assertFalse(handle.isAlive());
        handle.stop();
        handle.close();
    }

    @Test
    void compositeHandleStopsAllChildren() {
        RecordingHandle h1 = new RecordingHandle("run-1");
        RecordingHandle h2 = new RecordingHandle("run-1");

        CompositeRunHandle composite = new CompositeRunHandle("run-1", List.of(h1, h2));
        composite.stop();

        assertTrue(h1.stopped);
        assertTrue(h2.stopped);
    }

    @Test
    void processHandleReflectsAndStopsProcess() throws IOException {
        Process process = new ProcessBuilder("/bin/sh", "-c", "sleep 2").start();
        ProcessRunHandle handle = new ProcessRunHandle("run-proc", process);

        assertTrue(handle.isAlive());
        handle.stop();
    }

    private static class RecordingHandle implements RunHandle {
        private final String runId;
        private boolean stopped;

        private RecordingHandle(String runId) {
            this.runId = runId;
        }

        @Override
        public String runId() {
            return runId;
        }

        @Override
        public boolean isAlive() {
            return !stopped;
        }

        @Override
        public void stop() {
            stopped = true;
        }
    }
}