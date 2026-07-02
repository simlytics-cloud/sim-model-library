package iso.sim.server.service;

public class RunReadinessResult {
    private final boolean ready;
    private final String message;

    private RunReadinessResult(boolean ready, String message) {
        this.ready = ready;
        this.message = message;
    }

    public static RunReadinessResult ready(String message) {
        return new RunReadinessResult(true, message);
    }

    public static RunReadinessResult notReady(String message) {
        return new RunReadinessResult(false, message);
    }

    public boolean isReady() {
        return ready;
    }

    public String getMessage() {
        return message;
    }
}
