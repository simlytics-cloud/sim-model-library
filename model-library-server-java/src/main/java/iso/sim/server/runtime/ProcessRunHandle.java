package iso.sim.server.runtime;

public class ProcessRunHandle implements RunHandle {
    private final String runId;
    private final Process process;

    public ProcessRunHandle(String runId, Process process) {
        this.runId = runId;
        this.process = process;
    }

    @Override
    public String runId() {
        return runId;
    }

    @Override
    public boolean isAlive() {
        return process.isAlive();
    }

    @Override
    public void stop() {
        process.destroy();
    }
}