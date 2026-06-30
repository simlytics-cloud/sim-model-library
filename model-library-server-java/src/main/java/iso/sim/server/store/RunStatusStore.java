package iso.sim.server.store;

import iso.sim.server.dto.run.RunStatusResponse;

import java.util.List;

public interface RunStatusStore {
    void save(RunStatusResponse status);

    RunStatusResponse get(String runId);

    List<RunStatusResponse> getAll();
}
