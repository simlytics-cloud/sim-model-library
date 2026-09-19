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

package iso.sim.server.runtime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RunResourceRegistry {
    private final Map<String, RunHandle> handlesByRunId = new ConcurrentHashMap<>();

    public void register(String runId, RunHandle handle) {
        handlesByRunId.put(runId, handle);
    }

    public RunHandle get(String runId) {
        return handlesByRunId.get(runId);
    }

    public void stop(String runId) {
        RunHandle handle = handlesByRunId.remove(runId);
        if (handle != null) {
            handle.close();
        }
    }

    public void remove(String runId) {
        handlesByRunId.remove(runId);
    }

    public void stopAll() {
        handlesByRunId.values().forEach(RunHandle::close);
        handlesByRunId.clear();
    }
}