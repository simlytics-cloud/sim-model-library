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
import iso.sim.server.dto.run.CurrentSimulationTimeDto;
import iso.sim.server.executor.RunExecutionContext;
import iso.sim.server.store.RunStatusStore;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

public class RunLifecycleService {
    private static final Set<String> VALID_STATUSES = Set.of(
        "accepted", "starting", "locally-ready", "locally-stopped", "locally-failed"
    );

    private final RunStatusStore runStatusStore;
    private final Clock clock;

    public RunLifecycleService(RunStatusStore runStatusStore) {
        this(runStatusStore, Clock.systemUTC());
    }

    public RunLifecycleService(RunStatusStore runStatusStore, Clock clock) {
        this.runStatusStore = runStatusStore;
        this.clock = clock;
    }

    public RunStatusResponse markAccepted(String runId, String modelId, String message) {
        return persist(runId, modelId, null, null, null, "accepted", now(), null, null, null, message, null);
    }

    public boolean markAccepted(RunExecutionContext context, String message) {
        var simulation = context.getRequest().getSimulation();
        return runStatusStore.saveIfAbsent(new RunStatusResponse(
            context.getRunId(),
            context.getModelId(),
            simulation.getSimulationId(),
            simulation.getModelInstanceId(),
            simulation.getCoordinatorId(),
            "accepted",
            now(),
            null,
            null,
            null,
            message,
            null
        ));
    }

    public RunStatusResponse markStarting(String runId, String message) {
        RunStatusResponse current = requireCurrent(runId);
        return persist(runId, current.getModelId(), current.getSimulationId(), current.getModelInstanceId(), current.getCoordinatorId(), "starting", current.getAcceptedAt(), current.getReadyAt(), current.getStartedAt(),
            current.getCompletedAt(), message, current.getCurrentSimulationTime());
    }

    public RunStatusResponse markLocallyReady(String runId, String message) {
        RunStatusResponse current = requireCurrent(runId);
        return persist(runId, current.getModelId(), current.getSimulationId(), current.getModelInstanceId(), current.getCoordinatorId(), "locally-ready", current.getAcceptedAt(), now(), current.getStartedAt(),
            current.getCompletedAt(), message, current.getCurrentSimulationTime());
    }

    public RunStatusResponse markLocallyStopped(String runId, String message) {
        RunStatusResponse current = requireCurrent(runId);
        return persist(runId, current.getModelId(), current.getSimulationId(), current.getModelInstanceId(), current.getCoordinatorId(), "locally-stopped", current.getAcceptedAt(), current.getReadyAt(), current.getStartedAt(),
            now(), message, current.getCurrentSimulationTime());
    }

    public RunStatusResponse markLocallyFailed(String runId, String message) {
        RunStatusResponse current = requireCurrent(runId);
        return persist(runId, current.getModelId(), current.getSimulationId(), current.getModelInstanceId(), current.getCoordinatorId(), "locally-failed", current.getAcceptedAt(), current.getReadyAt(), current.getStartedAt(),
            now(), message, current.getCurrentSimulationTime());
    }

    private RunStatusResponse requireCurrent(String runId) {
        RunStatusResponse current = runStatusStore.get(runId);
        if (current == null) {
            throw new RunNotFoundException(runId);
        }
        return current;
    }

    private RunStatusResponse persist(
        String runId,
        String modelId,
        String simulationId,
        String modelInstanceId,
        String coordinatorId,
        String status,
        String acceptedAt,
        String readyAt,
        String startedAt,
        String completedAt,
        String message,
        CurrentSimulationTimeDto currentSimulationTime
    ) {
        validateStatus(status);
        RunStatusResponse next = new RunStatusResponse(
            runId,
            modelId,
            simulationId,
            modelInstanceId,
            coordinatorId,
            status,
            acceptedAt,
            readyAt,
            startedAt,
            completedAt,
            message,
            currentSimulationTime
        );
        runStatusStore.save(next);
        return next;
    }

    private void validateStatus(String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Invalid run status: " + status);
        }
    }

    private String now() {
        return Instant.now(clock).toString();
    }
}