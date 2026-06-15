package iso.sim.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import iso.sim.server.CatalogRepository
import iso.sim.server.ModelCatalogService
import iso.sim.server.ModelLibraryRoutes
import iso.sim.server.RunService
import iso.sim.server.StartModelRunRequest
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.javadsl.Http
import org.apache.pekko.http.javadsl.ServerBinding
import org.apache.pekko.http.javadsl.server.Route
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class ModelLibraryClientServerIntegrationTest {
    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    private val actorSystem = ActorSystem.create("model-library-client-server-test")

    @AfterEach
    fun tearDown() {
        actorSystem.terminate()
    }

    @Test
    fun `client can call server list and get endpoints`() {
        val service = ModelCatalogService(CatalogRepository())
        val routes = ModelLibraryRoutes(service, RunService(service))
        val binding = bind(routes.routes())

        try {
            val baseUrl = "http://127.0.0.1:${binding.localAddress().port}"
            val client = ModelLibraryClient(baseUrl, actorSystem)

            val listResponse = client.listModels().toCompletableFuture().get(10, TimeUnit.SECONDS)
            assertEquals("irpsystem.irpmodel.Vehicle", listResponse.models.first().modelId)

            val getResponse = client.getModel("irpsystem.irpmodel.Vehicle").toCompletableFuture().get(10, TimeUnit.SECONDS)
            assertEquals("Vehicle", getResponse.name)

            val missingResponse = org.junit.jupiter.api.Assertions.assertThrows(ExecutionException::class.java) {
                client.getModel("missing.model").toCompletableFuture().get(10, TimeUnit.SECONDS)
            }
            val missingCause = missingResponse.cause as ModelLibraryClientException
            assertEquals(404, missingCause.httpStatus)
            assertEquals("MODEL_NOT_FOUND", missingCause.errorCode)
            assertTrue(missingCause.message!!.contains("missing.model"))

            val runRequest = java.io.File("irpsystem.irpmodel.Vehicle.put-run-request.json").readText()
            val runResponse = client.runModel(
                "irpsystem.irpmodel.Vehicle",
                objectMapper.readValue<StartModelRunRequest>(runRequest)
            ).toCompletableFuture().get(10, TimeUnit.SECONDS)
            assertEquals("irpsystem.irpmodel.Vehicle", runResponse.modelId)
            assertEquals("accepted", runResponse.status)
            assertTrue(runResponse.statusUrl.startsWith("/v1/runs/"))

            val runId = runResponse.runId
            val statusResponse = client.getRunStatus(runId).toCompletableFuture().get(10, TimeUnit.SECONDS)
            assertEquals(runId, statusResponse.runId)
            assertEquals("accepted", statusResponse.status)

            val missingRunResponse = org.junit.jupiter.api.Assertions.assertThrows(ExecutionException::class.java) {
                client.getRunStatus("missing-run").toCompletableFuture().get(10, TimeUnit.SECONDS)
            }
            val missingRunCause = missingRunResponse.cause as ModelLibraryClientException
            assertEquals(404, missingRunCause.httpStatus)
            assertEquals("RUN_NOT_FOUND", missingRunCause.errorCode)
        } finally {
            binding.unbind().toCompletableFuture().get(10, TimeUnit.SECONDS)
        }
    }

    @Test
    fun `client maps run validation failure to typed exception`() {
        val service = ModelCatalogService(CatalogRepository())
        val routes = ModelLibraryRoutes(service, RunService(service))
        val binding = bind(routes.routes())

        try {
            val baseUrl = "http://127.0.0.1:${binding.localAddress().port}"
            val client = ModelLibraryClient(baseUrl, actorSystem)

            val ex = org.junit.jupiter.api.Assertions.assertThrows(ExecutionException::class.java) {
                client.runModelRaw("irpsystem.irpmodel.Vehicle", "{}")
                    .toCompletableFuture()
                    .get(10, TimeUnit.SECONDS)
            }
            val cause = ex.cause as ModelLibraryClientException
            assertEquals(400, cause.httpStatus)
            assertEquals("INVALID_RUN_REQUEST", cause.errorCode)
            assertTrue(cause.message!!.contains("required"))
        } finally {
            binding.unbind().toCompletableFuture().get(10, TimeUnit.SECONDS)
        }
    }

    private fun bind(route: Route): ServerBinding {
        return Http.get(actorSystem)
            .newServerAt("127.0.0.1", 0)
            .bind(route.function(actorSystem))
            .toCompletableFuture()
            .get(10, TimeUnit.SECONDS)
    }
}