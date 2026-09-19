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

import iso.sim.server.dto.run.RunStatusResponse;
import iso.sim.server.store.InMemoryRunStatusStore;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RunLifecycleServiceTest {

    @Test
    void transitionsUseAllowedStatusesAndSetTimestamps() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-30T12:00:00Z"), ZoneOffset.UTC);
        RunLifecycleService lifecycleService = new RunLifecycleService(new InMemoryRunStatusStore(), clock);

        RunStatusResponse accepted = lifecycleService.markAccepted("run-1", "model-1", "accepted");
        assertEquals("accepted", accepted.getStatus());
        assertNotNull(accepted.getAcceptedAt());

        RunStatusResponse starting = lifecycleService.markStarting("run-1", "starting");
        assertEquals("starting", starting.getStatus());

        RunStatusResponse ready = lifecycleService.markReady("run-1", "ready");
        assertEquals("ready", ready.getStatus());
        assertNotNull(ready.getReadyAt());

        RunStatusResponse running = lifecycleService.markRunning("run-1", "running");
        assertEquals("running", running.getStatus());
        assertNotNull(running.getStartedAt());

        RunStatusResponse completed = lifecycleService.markCompleted("run-1", "completed");
        assertEquals("completed", completed.getStatus());
        assertNotNull(completed.getCompletedAt());
    }
}