/*
 * Sim Model Library Copyright (C) 2026 simlytics.cloud LLC and
 * Sim Model Library contributors.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package iso.sim.server.dto.run;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteRunnerEventReport {
    private final String runId;
    private final String modelInstanceId;
    private final String coordinatorId;
    private final String eventId;
    private final String eventType;
    private final String timestamp;
    private final String errorDetail;

    @JsonCreator
    public RemoteRunnerEventReport(
        @JsonProperty("runId") String runId,
        @JsonProperty("modelInstanceId") String modelInstanceId,
        @JsonProperty("coordinatorId") String coordinatorId,
        @JsonProperty("eventId") String eventId,
        @JsonProperty("eventType") String eventType,
        @JsonProperty("timestamp") String timestamp,
        @JsonProperty("errorDetail") String errorDetail
    ) {
        this.runId = runId;
        this.modelInstanceId = modelInstanceId;
        this.coordinatorId = coordinatorId;
        this.eventId = eventId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.errorDetail = errorDetail;
    }

    public String getRunId() { return runId; }
    public String getModelInstanceId() { return modelInstanceId; }
    public String getCoordinatorId() { return coordinatorId; }
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getTimestamp() { return timestamp; }
    public String getErrorDetail() { return errorDetail; }
}
