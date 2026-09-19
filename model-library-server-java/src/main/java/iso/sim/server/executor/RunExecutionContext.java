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

package iso.sim.server.executor;

import iso.sim.server.dto.run.StartModelRunRequest;

public class RunExecutionContext {
    private final String runId;
    private final String modelId;
    private final StartModelRunRequest request;

    public RunExecutionContext(String runId, String modelId, StartModelRunRequest request) {
        this.runId = runId;
        this.modelId = modelId;
        this.request = request;
    }

    public String getRunId() {
        return runId;
    }

    public String getModelId() {
        return modelId;
    }

    public StartModelRunRequest getRequest() {
        return request;
    }
}
