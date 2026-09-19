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

package iso.sim.server.store;

import iso.sim.server.dto.run.RunStatusResponse;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRunStatusStore implements RunStatusStore {
    private final ConcurrentHashMap<String, RunStatusResponse> runs = new ConcurrentHashMap<>();

    @Override
    public void save(RunStatusResponse status) {
        runs.put(status.getRunId(), status);
    }

    @Override
    public RunStatusResponse get(String runId) {
        return runs.get(runId);
    }

    @Override
    public List<RunStatusResponse> getAll() {
        return runs.values().stream().toList();
    }
}