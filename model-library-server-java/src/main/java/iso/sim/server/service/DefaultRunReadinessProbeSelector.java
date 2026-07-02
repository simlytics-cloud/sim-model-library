package iso.sim.server.service;

import iso.sim.server.executor.RunExecutionContext;

public class DefaultRunReadinessProbeSelector implements RunReadinessProbeSelector {
    @Override
    public RunReadinessProbe select(RunExecutionContext context) {
        return new ImmediateReadinessProbe();
    }
}
