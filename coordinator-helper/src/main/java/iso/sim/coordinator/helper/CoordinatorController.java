package iso.sim.coordinator.helper;

import iso.sim.coordinator.helper.dto.CoordinatedRunStatusResponse;

public interface CoordinatorController {
    void start(CoordinatedRunStatusResponse coordinatedRun);

    void cancel(CoordinatedRunStatusResponse coordinatedRun);
}
