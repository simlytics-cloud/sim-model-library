package iso.sim.server.executor;

import iso.sim.server.runtime.NoopRunHandle;
import iso.sim.server.runtime.RunHandle;

public class StubRunExecutor implements RunExecutor {
    @Override
    public RunHandle start(RunExecutionContext context) {
        // Phase 1E stub: runtime start intentionally does nothing.
        return new NoopRunHandle(context.getRunId());
    }
}
