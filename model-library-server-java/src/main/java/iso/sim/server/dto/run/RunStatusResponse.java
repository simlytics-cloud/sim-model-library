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

package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RunStatusResponse {
    private final String runId;
    private final String modelId;
    private final String simulationId;
    private final String modelInstanceId;
    private final String coordinatorId;
    private final String status;
    private final String acceptedAt;
    private final String readyAt;
    private final String startedAt;
    private final String completedAt;
    private final String message;
    private final CurrentSimulationTimeDto currentSimulationTime;

    @JsonCreator
    public RunStatusResponse(
        @JsonProperty("runId") String runId,
        @JsonProperty("modelId") String modelId,
        @JsonProperty("simulationId") String simulationId,
        @JsonProperty("modelInstanceId") String modelInstanceId,
        @JsonProperty("coordinatorId") String coordinatorId,
        @JsonProperty("status") String status,
        @JsonProperty("acceptedAt") String acceptedAt,
        @JsonProperty("readyAt") String readyAt,
        @JsonProperty("startedAt") String startedAt,
        @JsonProperty("completedAt") String completedAt,
        @JsonProperty("message") String message,
        @JsonProperty("currentSimulationTime") CurrentSimulationTimeDto currentSimulationTime
    ) {
        this.runId = runId;
        this.modelId = modelId;
        this.simulationId = simulationId;
        this.modelInstanceId = modelInstanceId;
        this.coordinatorId = coordinatorId;
        this.status = status;
        this.acceptedAt = acceptedAt;
        this.readyAt = readyAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.message = message;
        this.currentSimulationTime = currentSimulationTime;
    }

    public RunStatusResponse(String runId, String modelId, String status, String acceptedAt, String message) {
        this(runId, modelId, null, null, null, status, acceptedAt, null, null, null, message, null);
    }

    public RunStatusResponse(
        String runId,
        String modelId,
        String status,
        String acceptedAt,
        String readyAt,
        String startedAt,
        String completedAt,
        String message,
        CurrentSimulationTimeDto currentSimulationTime
    ) {
        this(
            runId, modelId, null, null, null, status, acceptedAt, readyAt, startedAt, completedAt, message, currentSimulationTime
        );
    }


    public String getRunId() { return runId; }
    public String getModelId() { return modelId; }
    public String getSimulationId() { return simulationId; }
    public String getModelInstanceId() { return modelInstanceId; }
    public String getCoordinatorId() { return coordinatorId; }
    public String getStatus() { return status; }
    public String getAcceptedAt() { return acceptedAt; }
    public String getReadyAt() { return readyAt; }
    public String getStartedAt() { return startedAt; }
    public String getCompletedAt() { return completedAt; }
    public String getMessage() { return message; }
    public CurrentSimulationTimeDto getCurrentSimulationTime() { return currentSimulationTime; }
}
