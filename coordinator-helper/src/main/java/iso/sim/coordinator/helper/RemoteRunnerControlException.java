package iso.sim.coordinator.helper;

public class RemoteRunnerControlException extends RuntimeException {
    public RemoteRunnerControlException(String message) {
        super(message);
    }

    public RemoteRunnerControlException(String message, Throwable cause) {
        super(message, cause);
    }
}
