package iso.sim.server.runtime;

import java.util.List;

public class CompositeRunHandle implements RunHandle {
    private final String runId;
    private final List<RunHandle> handles;

    public CompositeRunHandle(String runId, List<RunHandle> handles) {
        this.runId = runId;
        this.handles = List.copyOf(handles);
    }

    @Override
    public String runId() {
        return runId;
    }

    @Override
    public boolean isAlive() {
        return handles.stream().anyMatch(RunHandle::isAlive);
    }

    @Override
    public void stop() {
        for (RunHandle handle : handles) {
            handle.close();
        }
    }
}