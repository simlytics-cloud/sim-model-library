package iso.sim.coordinator.helper.dto;

public record RemoteRunnerEventReport(
    String runId,
    String modelInstanceId,
    String coordinatorId,
    String eventId,
    String eventType,
    String timestamp,
    String errorDetail
) {
}
