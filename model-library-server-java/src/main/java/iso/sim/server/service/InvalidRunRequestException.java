package iso.sim.server.service;

public class InvalidRunRequestException extends RuntimeException {
    public InvalidRunRequestException(String message) {
        super(message);
    }
}
