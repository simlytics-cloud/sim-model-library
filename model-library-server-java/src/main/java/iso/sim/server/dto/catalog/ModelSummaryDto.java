package iso.sim.server.dto.catalog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelSummaryDto {
    private final String modelId;
    private final String name;
    private final String description;
    private final String implementationLanguage;

    @JsonCreator
    public ModelSummaryDto(
        @JsonProperty("modelId") String modelId,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("implementationLanguage") String implementationLanguage
    ) {
        this.modelId = modelId;
        this.name = name;
        this.description = description;
        this.implementationLanguage = implementationLanguage;
    }

    public String getModelId() { return modelId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getImplementationLanguage() { return implementationLanguage; }
}
