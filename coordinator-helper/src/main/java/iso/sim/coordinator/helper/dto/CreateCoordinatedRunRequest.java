package iso.sim.coordinator.helper.dto;

public record CreateCoordinatedRunRequest(
    String runId,
    String simulationId,
    String coordinatorId,
    String coordinatorToken
) {
}
