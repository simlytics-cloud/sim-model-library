package iso.sim.server.runtime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RunResourceRegistry {
    private final Map<String, RunHandle> handlesByRunId = new ConcurrentHashMap<>();

    public void register(String runId, RunHandle handle) {
        handlesByRunId.put(runId, handle);
    }

    public RunHandle get(String runId) {
        return handlesByRunId.get(runId);
    }

    public void stop(String runId) {
        RunHandle handle = handlesByRunId.remove(runId);
        if (handle != null) {
            handle.close();
        }
    }

    public void remove(String runId) {
        handlesByRunId.remove(runId);
    }

    public void stopAll() {
        handlesByRunId.values().forEach(RunHandle::close);
        handlesByRunId.clear();
    }
}