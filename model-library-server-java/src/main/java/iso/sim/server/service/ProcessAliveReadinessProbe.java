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

import iso.sim.server.executor.RunExecutionContext;
import iso.sim.server.runtime.RunHandle;

import java.time.Duration;

public class ProcessAliveReadinessProbe implements RunReadinessProbe {
    private final Duration waitDuration;

    public ProcessAliveReadinessProbe(Duration waitDuration) {
        this.waitDuration = waitDuration;
    }

    @Override
    public RunReadinessResult awaitReady(RunExecutionContext context, RunHandle handle) {
        if (waitDuration != null && !waitDuration.isZero() && !waitDuration.isNegative()) {
            try {
                Thread.sleep(waitDuration.toMillis());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return RunReadinessResult.notReady("Readiness check interrupted");
            }
        }
        if (handle.isAlive()) {
            return RunReadinessResult.ready("Runtime process is alive");
        }
        return RunReadinessResult.notReady("Runtime process is not alive");
    }
}
