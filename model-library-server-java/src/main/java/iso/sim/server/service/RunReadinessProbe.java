package iso.sim.server.service;

import iso.sim.server.executor.RunExecutionContext;
import iso.sim.server.runtime.RunHandle;

public interface RunReadinessProbe {
    RunReadinessResult awaitReady(RunExecutionContext context, RunHandle handle);
}
