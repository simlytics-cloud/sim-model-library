package iso.sim.coordinator.helper.dto;

import java.util.List;

public record CoordinatedRunStatusResponse(
    String runId,
    String simulationId,
    String coordinatorId,
    String status,
    String acceptedAt,
    String readyAt,
    String startedAt,
    String completedAt,
    String message,
    List<RemoteRunnerStatusResponse> remoteRunners
) {
}
