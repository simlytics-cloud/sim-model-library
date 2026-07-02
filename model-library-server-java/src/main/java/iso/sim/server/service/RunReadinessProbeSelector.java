package iso.sim.server.service;

import iso.sim.server.executor.RunExecutionContext;

public interface RunReadinessProbeSelector {
    RunReadinessProbe select(RunExecutionContext context);
}
