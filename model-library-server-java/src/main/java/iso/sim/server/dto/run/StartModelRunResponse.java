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
public class StartModelRunResponse {
    private final String runId;
    private final String modelId;
    private final String status;
    private final String statusUrl;
    private final String acceptedAt;
    private final String message;

    @JsonCreator
    public StartModelRunResponse(
        @JsonProperty("runId") String runId,
        @JsonProperty("modelId") String modelId,
        @JsonProperty("status") String status,
        @JsonProperty("statusUrl") String statusUrl,
        @JsonProperty("acceptedAt") String acceptedAt,
        @JsonProperty("message") String message
    ) {
        this.runId = runId;
        this.modelId = modelId;
        this.status = status;
        this.statusUrl = statusUrl;
        this.acceptedAt = acceptedAt;
        this.message = message;
    }

    public String getRunId() { return runId; }
    public String getModelId() { return modelId; }
    public String getStatus() { return status; }
    public String getStatusUrl() { return statusUrl; }
    public String getAcceptedAt() { return acceptedAt; }
    public String getMessage() { return message; }
}
