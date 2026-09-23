package iso.sim.coordinator.helper.dto;

public record CoordinatorEventReport(
    String eventId,
    String eventType,
    String timestamp,
    String errorDetail
) {
}
