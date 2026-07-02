package iso.sim.server.runtime;

public interface RunHandle extends AutoCloseable {
    String runId();

    boolean isAlive();

    void stop();

    @Override
    default void close() {
        stop();
    }
}