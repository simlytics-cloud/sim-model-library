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
        val runDefaults = loadRunDefaults(config)

        val actorSystem = ActorSystem.create("model-library-server")
        val service = ModelCatalogService(CatalogRepository())
        val runExecutor = when (runtimeExecutorType.lowercase()) {
            "stub" -> StubRunExecutor()
            else -> throw IllegalArgumentException("Unsupported runtime executor '$runtimeExecutorType'")
        }
        val runService = RunService(service, runExecutor = runExecutor, defaults = runDefaults)
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

    private fun loadRunDefaults(config: com.typesafe.config.Config): RunRequestDefaults {
        fun str(path: String, fallback: String): String {
            if (!config.hasPath(path)) {
                println("[WARN] Missing config '$path'. Using default '$fallback'.")
                return fallback
            }
            return config.getString(path)
        }

        return RunRequestDefaults(
            simulationId = str("model.library.defaults.simulation.simulationId", "sim-001"),
            modelInstanceId = str("model.library.defaults.simulation.modelInstanceId", "instance-001"),
            coordinatorId = str("model.library.defaults.simulation.coordinatorId", "demo-coordinator"),
            kafkaBootstrapServers = str("model.library.defaults.kafka.bootstrapServers", "localhost:9092"),
            kafkaTopic = str("model.library.defaults.kafka.topic", "simulation-events"),
            kafkaConsumerGroup = str("model.library.defaults.kafka.consumerGroup", "simulation-runner"),
            kafkaSecurityProtocol = str("model.library.defaults.kafka.securityProtocol", "PLAINTEXT"),
            kafkaSaslMechanism = if (config.hasPath("model.library.defaults.kafka.saslMechanism")) {
                config.getString("model.library.defaults.kafka.saslMechanism")
            } else {
                println("[WARN] Missing config 'model.library.defaults.kafka.saslMechanism'. Using default 'null'.")
                null
            },
            kafkaProperties = emptyMap()
        )
    }
}
