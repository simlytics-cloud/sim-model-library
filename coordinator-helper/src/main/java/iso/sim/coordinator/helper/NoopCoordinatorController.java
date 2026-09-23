package iso.sim.coordinator.helper;

import iso.sim.coordinator.helper.dto.CoordinatedRunStatusResponse;

/**
 * Explicit seam for applications that attach a real coordinator outside this optional helper.
 */
public class NoopCoordinatorController implements CoordinatorController {
    @Override
    public void start(CoordinatedRunStatusResponse coordinatedRun) {
    }

    @Override
    public void cancel(CoordinatedRunStatusResponse coordinatedRun) {
    }
}
