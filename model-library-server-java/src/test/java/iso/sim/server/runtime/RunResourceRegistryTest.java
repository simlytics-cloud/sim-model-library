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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RunResourceRegistryTest {

    @Test
    void registerGetAndRemoveWork() {
        RunResourceRegistry registry = new RunResourceRegistry();
        NoopRunHandle handle = new NoopRunHandle("run-1");

        registry.register("run-1", handle);
        assertEquals(handle, registry.get("run-1"));

        registry.remove("run-1");
        assertNull(registry.get("run-1"));
    }

    @Test
    void stopClosesRegisteredHandle() {
        RunResourceRegistry registry = new RunResourceRegistry();
        RecordingHandle handle = new RecordingHandle("run-2");

        registry.register("run-2", handle);
        registry.stop("run-2");

        assertEquals(1, handle.stopCount);
        assertNull(registry.get("run-2"));
    }

    @Test
    void stopAllClosesAllHandlesAndClearsRegistry() {
        RunResourceRegistry registry = new RunResourceRegistry();
        RecordingHandle h1 = new RecordingHandle("run-3");
        RecordingHandle h2 = new RecordingHandle("run-4");

        registry.register("run-3", h1);
        registry.register("run-4", h2);
        registry.stopAll();

        assertEquals(1, h1.stopCount);
        assertEquals(1, h2.stopCount);
        assertNull(registry.get("run-3"));
        assertNull(registry.get("run-4"));
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