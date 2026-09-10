package iso.sim.server.dto.catalog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import iso.sim.server.dto.run.TimeModeDto;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelDto {
    private final String modelId;
    private final String name;
    private final String description;
    private final TimeModeDto timeMode;
    private final String implementationLanguage;
    private final List<PortDefinitionDto> inputPorts;
    private final List<PortDefinitionDto> outputPorts;
    private final List<ParameterDefinitionDto> parameters;
    private final Map<String, JsonNode> messageSchemas;
    private final JsonNode initializationSchema;
    private final JsonNode defaultParameterSet;
    private final String behaviorDescription;
    private final Map<String, JsonNode> metadata;

    @JsonCreator
    public ModelDto(
        @JsonProperty("modelId") String modelId,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("timeMode") TimeModeDto timeMode,
        @JsonProperty("implementationLanguage") String implementationLanguage,
        @JsonProperty("inputPorts") List<PortDefinitionDto> inputPorts,
        @JsonProperty("outputPorts") List<PortDefinitionDto> outputPorts,
        @JsonProperty("parameters") List<ParameterDefinitionDto> parameters,
        @JsonProperty("messageSchemas") Map<String, JsonNode> messageSchemas,
        @JsonProperty("initializationSchema") JsonNode initializationSchema,
        @JsonProperty("defaultParameterSet") JsonNode defaultParameterSet,
        @JsonProperty("behaviorDescription") String behaviorDescription,
        @JsonProperty("metadata") Map<String, JsonNode> metadata
    ) {
        this.modelId = modelId;
        this.name = name;
        this.description = description;
        this.timeMode = timeMode;
        this.implementationLanguage = implementationLanguage;
        this.inputPorts = inputPorts;
        this.outputPorts = outputPorts;
        this.parameters = parameters;
        this.messageSchemas = messageSchemas;
        this.initializationSchema = initializationSchema;
        this.defaultParameterSet = defaultParameterSet;
        this.behaviorDescription = behaviorDescription;
        this.metadata = metadata == null ? Map.of() : metadata;
    }

    public String getModelId() { return modelId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public TimeModeDto getTimeMode() { return timeMode; }
    public String getImplementationLanguage() { return implementationLanguage; }
    public List<PortDefinitionDto> getInputPorts() { return inputPorts; }
    public List<PortDefinitionDto> getOutputPorts() { return outputPorts; }
    public List<ParameterDefinitionDto> getParameters() { return parameters; }
    public Map<String, JsonNode> getMessageSchemas() { return messageSchemas; }
    public JsonNode getInitializationSchema() { return initializationSchema; }
    public JsonNode getDefaultParameterSet() { return defaultParameterSet; }
    public String getBehaviorDescription() { return behaviorDescription; }
    public Map<String, JsonNode> getMetadata() { return metadata; }
}
