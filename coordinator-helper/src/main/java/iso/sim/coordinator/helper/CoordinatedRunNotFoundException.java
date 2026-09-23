package iso.sim.coordinator.helper;

public class CoordinatedRunNotFoundException extends RuntimeException {
    public CoordinatedRunNotFoundException(String runId) {
        super("Coordinated run '" + runId + "' was not found");
    }
}
