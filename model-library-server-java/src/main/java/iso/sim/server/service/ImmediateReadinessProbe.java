package iso.sim.server.service;

import iso.sim.server.executor.RunExecutionContext;
import iso.sim.server.runtime.RunHandle;

public class ImmediateReadinessProbe implements RunReadinessProbe {
    @Override
    public RunReadinessResult awaitReady(RunExecutionContext context, RunHandle handle) {
        return RunReadinessResult.ready("Runtime ready immediately");
    }
}
