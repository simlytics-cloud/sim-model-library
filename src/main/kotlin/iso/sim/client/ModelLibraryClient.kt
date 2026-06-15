package iso.sim.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import iso.sim.server.AtomicModelDto
import iso.sim.server.ErrorResponse
import iso.sim.server.ModelListResponse
import iso.sim.server.RunStatusResponse
import iso.sim.server.StartModelRunRequest
import iso.sim.server.StartModelRunResponse
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.javadsl.Http
import org.apache.pekko.http.javadsl.model.ContentTypes
import org.apache.pekko.http.javadsl.model.HttpRequest
import org.apache.pekko.http.javadsl.model.HttpResponse
import org.apache.pekko.http.javadsl.model.StatusCodes
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit

class ModelLibraryClient(
    private val baseUrl: String,
    private val actorSystem: ActorSystem,
    private val config: ModelLibraryClientConfig = ModelLibraryClientConfig(),
    private val objectMapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
) {
    fun listModels(): CompletionStage<ModelListResponse> {
        return executeJsonGet("$baseUrl/v1/models")
    }

    fun getModel(modelId: String): CompletionStage<AtomicModelDto> {
        return executeJsonGet("$baseUrl/v1/models/$modelId")
    }

    fun runModel(modelId: String, request: StartModelRunRequest): CompletionStage<StartModelRunResponse> {
        val requestJson = objectMapper.writeValueAsString(request)
        val httpRequest = HttpRequest.PUT("$baseUrl/v1/models/$modelId/run")
            .withEntity(ContentTypes.APPLICATION_JSON, requestJson)
        return executeJsonRequest(httpRequest, retriesRemaining = 0)
    }

    fun runModelRaw(modelId: String, requestJson: String): CompletionStage<StartModelRunResponse> {
        val httpRequest = HttpRequest.PUT("$baseUrl/v1/models/$modelId/run")
            .withEntity(ContentTypes.APPLICATION_JSON, requestJson)
        return executeJsonRequest(httpRequest, retriesRemaining = 0)
    }

    fun getRunStatus(runId: String): CompletionStage<RunStatusResponse> {
        return executeJsonGet("$baseUrl/v1/runs/$runId")
    }

    private inline fun <reified T> executeJsonGet(url: String): CompletionStage<T> {
        return executeJsonRequest(HttpRequest.create(url), retriesRemaining = config.maxGetRetries)
    }

    private inline fun <reified T> executeJsonRequest(request: HttpRequest, retriesRemaining: Int): CompletionStage<T> {
        return executeRequest(request, retriesRemaining)
            .thenCompose { response ->
                if (response.status().isSuccess()) {
                    response.entity().toStrict(config.requestTimeoutMillis, actorSystem).thenApply { strictEntity ->
                        objectMapper.readValue<T>(strictEntity.data.utf8String())
                    }
                } else {
                    response.entity().toStrict(config.requestTimeoutMillis, actorSystem).thenCompose { strictEntity ->
                        val rawBody = strictEntity.data.utf8String()
                        val parsedError = parseErrorResponse(rawBody)
                        val exception = ModelLibraryClientException(
                            response.status().intValue(),
                            parsedError?.code,
                            parsedError?.message,
                            rawBody
                        )
                        CompletableFuture.failedFuture<T>(exception)
                    }
                }
            }
    }

    private fun executeRequest(request: HttpRequest, retriesRemaining: Int): CompletionStage<HttpResponse> {
        val timedRequest = withTimeout(Http.get(actorSystem).singleRequest(request))
        return timedRequest.handle { response, throwable ->
            when {
                throwable != null -> {
                    if (retriesRemaining > 0) {
                        executeRequest(request, retriesRemaining - 1)
                    } else {
                        CompletableFuture.failedFuture(throwable)
                    }
                }

                response.status().isFailure() && response.status().intValue() >= StatusCodes.INTERNAL_SERVER_ERROR.intValue() && retriesRemaining > 0 -> {
                    response.discardEntityBytes(actorSystem)
                    executeRequest(request, retriesRemaining - 1)
                }

                else -> CompletableFuture.completedFuture(response)
            }
        }.thenCompose { it }
    }

    private fun <T> withTimeout(stage: CompletionStage<T>): CompletionStage<T> {
        return stage.toCompletableFuture().orTimeout(config.requestTimeoutMillis, TimeUnit.MILLISECONDS)
    }

    private fun parseErrorResponse(rawBody: String): ErrorResponse? {
        return try {
            objectMapper.readValue<ErrorResponse>(rawBody)
        } catch (_: Exception) {
            null
        }
    }
}

data class ModelLibraryClientConfig(
    val requestTimeoutMillis: Long = 5_000,
    val maxGetRetries: Int = 1
)

class ModelLibraryClientException(
    val httpStatus: Int,
    val errorCode: String?,
    val errorMessagePayload: String?,
    val rawBody: String
) : RuntimeException(errorMessagePayload ?: "Model library request failed with status $httpStatus")
