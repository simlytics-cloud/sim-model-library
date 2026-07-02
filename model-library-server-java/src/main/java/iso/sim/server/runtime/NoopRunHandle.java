package iso.sim.server.runtime;

public class NoopRunHandle implements RunHandle {
    private final String runId;

    public NoopRunHandle(String runId) {
        this.runId = runId;
    }

    @Override
    public String runId() {
        return runId;
    }

    @Override
    public boolean isAlive() {
        return false;
    }

    @Override
    public void stop() {
        // no-op
    }
}