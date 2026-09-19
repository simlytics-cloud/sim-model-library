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