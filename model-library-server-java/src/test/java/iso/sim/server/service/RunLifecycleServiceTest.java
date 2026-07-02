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