package iso.sim.server.catalog;

public class ModelNotFoundException extends RuntimeException {
    public ModelNotFoundException(String modelId) {
        super("Model '" + modelId + "' was not found");
    }
}
