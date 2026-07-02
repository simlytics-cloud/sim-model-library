package iso.sim.server.executor;

import iso.sim.server.runtime.RunHandle;

public interface RunExecutor {
    RunHandle start(RunExecutionContext context);
}
