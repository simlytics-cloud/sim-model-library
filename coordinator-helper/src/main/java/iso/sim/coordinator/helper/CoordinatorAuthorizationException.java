package iso.sim.coordinator.helper;

public class CoordinatorAuthorizationException extends RuntimeException {
    public CoordinatorAuthorizationException() {
        super("The supplied coordinator-helper token is not authorized");
    }
}
