package iso.sim.server.dto.catalog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PortDefinitionDto {
    private final String name;
    private final String direction;
    private final String messageType;
    private final String description;

    @JsonCreator
    public PortDefinitionDto(
        @JsonProperty("name") String name,
        @JsonProperty("direction") String direction,
        @JsonProperty("messageType") String messageType,
        @JsonProperty("description") String description
    ) {
        this.name = name;
        this.direction = direction;
        this.messageType = messageType;
        this.description = description;
    }

    public String getName() { return name; }
    public String getDirection() { return direction; }
    public String getMessageType() { return messageType; }
    public String getDescription() { return description; }
}
