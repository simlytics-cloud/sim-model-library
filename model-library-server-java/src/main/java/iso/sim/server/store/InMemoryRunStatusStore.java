package iso.sim.server.store;

import iso.sim.server.dto.run.RunStatusResponse;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRunStatusStore implements RunStatusStore {
    private final ConcurrentHashMap<String, RunStatusResponse> runs = new ConcurrentHashMap<>();

    @Override
    public void save(RunStatusResponse status) {
        runs.put(status.getRunId(), status);
    }

    @Override
    public RunStatusResponse get(String runId) {
        return runs.get(runId);
    }

    @Override
    public List<RunStatusResponse> getAll() {
        return runs.values().stream().toList();
    }
}