package iso.sim.server.dto.catalog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelListResponse {
    private final List<AtomicModelSummaryDto> models;

    @JsonCreator
    public ModelListResponse(@JsonProperty("models") List<AtomicModelSummaryDto> models) {
        this.models = models;
    }

    public List<AtomicModelSummaryDto> getModels() { return models; }
}
