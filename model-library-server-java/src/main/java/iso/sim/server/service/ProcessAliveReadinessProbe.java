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
