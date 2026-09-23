package iso.sim.coordinator.helper.dto;

public record RemoteRunnerStatusResponse(
    String modelId,
    String modelInstanceId,
    String localStatus,
    String updatedAt,
    String errorDetail
) {
}
