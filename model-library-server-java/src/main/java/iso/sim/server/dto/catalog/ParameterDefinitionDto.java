package iso.sim.server.dto.catalog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParameterDefinitionDto {
    private final String name;
    private final String type;
    private final boolean required;
    private final JsonNode defaultValue;
    private final String description;

    @JsonCreator
    public ParameterDefinitionDto(
        @JsonProperty("name") String name,
        @JsonProperty("type") String type,
        @JsonProperty("required") boolean required,
        @JsonProperty("defaultValue") JsonNode defaultValue,
        @JsonProperty("description") String description
    ) {
        this.name = name;
        this.type = type;
        this.required = required;
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public boolean isRequired() { return required; }
    public JsonNode getDefaultValue() { return defaultValue; }
    public String getDescription() { return description; }
}
