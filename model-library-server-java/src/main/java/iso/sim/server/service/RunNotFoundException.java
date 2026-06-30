package iso.sim.server.service;

public class RunNotFoundException extends RuntimeException {
    public RunNotFoundException(String runId) {
        super("Run '" + runId + "' was not found");
    }
}
