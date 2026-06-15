package iso.sim.server

import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.javadsl.Http
import java.util.concurrent.CountDownLatch

object ModelLibraryServerMain {
    @JvmStatic
    fun main(args: Array<String>) {
        val config = ConfigFactory.load()
        val host = System.getProperty("model.library.host")
            ?: System.getenv("MODEL_LIBRARY_HOST")
            ?: "127.0.0.1"
        val port = (System.getProperty("model.library.port")
            ?: System.getenv("MODEL_LIBRARY_PORT")
            ?: config.getInt("model.library.server.port").toString()).toInt()
        val runtimeExecutorType = config.getString("model.library.runtime.executor")

        val actorSystem = ActorSystem.create("model-library-server")
        val service = ModelCatalogService(CatalogRepository())
        val runExecutor = when (runtimeExecutorType.lowercase()) {
            "stub" -> StubRunExecutor()
            else -> throw IllegalArgumentException("Unsupported runtime executor '$runtimeExecutorType'")
        }
        val runService = RunService(service, runExecutor = runExecutor)
        val routes = ModelLibraryRoutes(service, runService)

        val binding = Http.get(actorSystem)
            .newServerAt(host, port)
            .bind(routes.routes().function(actorSystem))
            .toCompletableFuture()
            .get()

        println("Model Library server running at http://$host:$port")
        println("Swagger endpoint: http://$host:$port/swagger")
        println("OpenAPI endpoint: http://$host:$port/openapi.yaml")

        Runtime.getRuntime().addShutdownHook(Thread {
            binding.unbind().toCompletableFuture().join()
            actorSystem.terminate()
        })

        CountDownLatch(1).await()
    }
}
