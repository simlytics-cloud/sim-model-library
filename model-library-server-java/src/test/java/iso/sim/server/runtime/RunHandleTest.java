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