package iso.sim.server

import com.fasterxml.jackson.databind.JsonNode

data class ModelCatalogDto(
    val models: List<AtomicModelDto>
)

data class AtomicModelSummaryDto(
    val modelId: String,
    val name: String,
    val description: String,
    val implementationLanguage: String
)

data class AtomicModelDto(
    val modelId: String,
    val name: String,
    val description: String,
    val implementationLanguage: String,
    val inputPorts: List<PortDefinitionDto>,
    val outputPorts: List<PortDefinitionDto>,
    val parameters: List<ParameterDefinitionDto>,
    val messageSchemas: Map<String, JsonNode>,
    val initializationSchema: JsonNode,
    val behaviorDescription: String? = null,
    val metadata: Map<String, JsonNode> = emptyMap()
)

data class PortDefinitionDto(
    val name: String,
    val direction: String,
    val messageType: String,
    val description: String? = null
)

data class ParameterDefinitionDto(
    val name: String,
    val type: String,
    val required: Boolean,
    val defaultValue: JsonNode? = null,
    val description: String? = null
)

data class ErrorResponse(
    val code: String,
    val message: String
)

data class ModelListResponse(
    val models: List<AtomicModelSummaryDto>
)

data class StartModelRunRequest(
    val runId: String? = null,
    val initializationParameters: Map<String, Any?>? = null,
    val kafka: KafkaConfigurationDto? = null,
    val simulation: SimulationContextDto? = null
)

data class KafkaConfigurationDto(
    val bootstrapServers: String? = null,
    val topic: String? = null,
    val consumerGroup: String? = null,
    val securityProtocol: String? = null,
    val saslMechanism: String? = null,
    val properties: Map<String, String>? = null
)

data class SimulationContextDto(
    val simulationId: String? = null,
    val federationId: String? = null,
    val timeMode: TimeModeDto? = null
)

data class TimeModeDto(
    val mode: String,
    val timeType: String,
    val secondsPerSimulationTimeUnit: Double? = null
)

data class StartModelRunResponse(
    val runId: String,
    val modelId: String,
    val status: String,
    val statusUrl: String,
    val acceptedAt: String,
    val message: String
)

data class RunStatusResponse(
    val runId: String,
    val modelId: String,
    val status: String,
    val acceptedAt: String,
    val message: String
)