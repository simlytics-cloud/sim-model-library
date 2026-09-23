/*
 * Sim Model Library Copyright (C) 2026 simlytics.cloud LLC and
 * Sim Model Library contributors.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package iso.sim.server.executor.example;

import iso.sim.server.example.store.ClerkModel;
import iso.sim.server.executor.RunExecutionContext;
import iso.sim.server.executor.RunExecutor;
import iso.sim.server.runtime.RunHandle;

/**
 * Explicitly selected non-production executor for the isolated local Clerk example.
 */
public class ExampleClerkModelRunExecutor implements RunExecutor {
    private final ClerkRuntime runtime;

    public ExampleClerkModelRunExecutor() {
        this(new InProcessClerkRuntime());
    }

    public ExampleClerkModelRunExecutor(ClerkRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public RunHandle start(RunExecutionContext context) {
        String modelInstanceId = context.getRequest().getSimulation().getModelInstanceId();
        ClerkModel model = new ClerkModel(modelInstanceId);
        return runtime.start(model, ClerkRuntimeTransport.from(context));
    }
}
