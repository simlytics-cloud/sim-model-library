package iso.sim.server.service;

import iso.sim.server.dto.run.RunStatusResponse;
import iso.sim.server.runtime.RunResourceRegistry;
import iso.sim.server.store.RunStatusStore;

import java.util.Set;

public class RunTerminalCoordinator {
    private static final Set<String> TERMINAL_STATUSES = Set.of("completed", "failed", "canceled");

    private final RunLifecycleService runLifecycleService;
    private final RunStatusStore runStatusStore;
    private final RunResourceRegistry runResourceRegistry;

    public RunTerminalCoordinator(
        RunLifecycleService runLifecycleService,
        RunStatusStore runStatusStore,
        RunResourceRegistry runResourceRegistry
    ) {
        this.runLifecycleService = runLifecycleService;
        this.runStatusStore = runStatusStore;
        this.runResourceRegistry = runResourceRegistry;
    }

    public void completeRun(String runId, String message) {
        transitionToTerminal(runId, message, TerminalStatus.COMPLETED);
    }

    public void failRun(String runId, String message) {
        transitionToTerminal(runId, message, TerminalStatus.FAILED);
    }

    private void transitionToTerminal(String runId, String message, TerminalStatus targetStatus) {
        RunStatusResponse current = runStatusStore.get(runId);
        if (current != null && !TERMINAL_STATUSES.contains(current.getStatus())) {
            if (targetStatus == TerminalStatus.COMPLETED) {
                runLifecycleService.markCompleted(runId, message);
            } else {
                runLifecycleService.markFailed(runId, message);
            }
        }

        cleanupRunResources(runId);
    }

    private void cleanupRunResources(String runId) {
        try {
            runResourceRegistry.stop(runId);
        } catch (RuntimeException ignored) {
        }
    }

    private enum TerminalStatus {
        COMPLETED,
        FAILED
    }
}