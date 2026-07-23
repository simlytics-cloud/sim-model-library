package iso.sim.server.dto.catalog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelCatalogDto {
    private final List<ModelDto> models;

    @JsonCreator
    public ModelCatalogDto(@JsonProperty("models") List<ModelDto> models) {
        this.models = models;
    }

    public List<ModelDto> getModels() {
        return models;
    }
}
