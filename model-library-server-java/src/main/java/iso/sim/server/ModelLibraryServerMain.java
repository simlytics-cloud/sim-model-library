package iso.sim.server;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import iso.sim.server.catalog.CatalogRepository;
import iso.sim.server.catalog.ModelCatalogService;
import iso.sim.server.dto.run.KafkaDefaultsResponse;
import iso.sim.server.executor.RunExecutor;
import iso.sim.server.executor.StubRunExecutor;
import iso.sim.server.service.KafkaDefaultsConfig;
import iso.sim.server.service.DefaultRunReadinessProbeSelector;
import iso.sim.server.service.RunService;
import iso.sim.server.store.InMemoryRunStatusStore;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.http.javadsl.Http;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;

public final class ModelLibraryServerMain {
    private ModelLibraryServerMain() {
    }

    public static void main(String[] args) throws Exception {
        Config config = ConfigFactory.load();
        String host = System.getProperty("model.library.host", System.getenv().getOrDefault("MODEL_LIBRARY_HOST", "127.0.0.1"));
        int port = Integer.parseInt(System.getProperty(
            "model.library.port",
            System.getenv().getOrDefault("MODEL_LIBRARY_PORT", String.valueOf(config.getInt("model.library.server.port")))
        ));
        String runtimeExecutorType = config.getString("model.library.runtime.executor");
        KafkaDefaultsResponse kafkaDefaults = KafkaDefaultsConfig.from(config).toResponse();

        ActorSystem actorSystem = ActorSystem.create("model-library-server");
        ModelCatalogService service = new ModelCatalogService(new CatalogRepository());
        RunExecutor runExecutor = switch (runtimeExecutorType.toLowerCase(Locale.ROOT)) {
            case "stub" -> new StubRunExecutor();
            default -> throw new IllegalArgumentException("Unsupported runtime executor '" + runtimeExecutorType + "'");
        };
        RunService runService = new RunService(service, runExecutor, new InMemoryRunStatusStore(), new DefaultRunReadinessProbeSelector());
        ModelLibraryRoutes routes = new ModelLibraryRoutes(service, runService, kafkaDefaults);

        var binding = Http.get(actorSystem)
            .newServerAt(host, port)
            .bind(routes.routes().function(actorSystem))
            .toCompletableFuture()
            .get();

        System.out.println("Model Library server running at http://" + host + ":" + port);
        System.out.println("Swagger endpoint: http://" + host + ":" + port + "/swagger");
        System.out.println("OpenAPI endpoint: http://" + host + ":" + port + "/openapi.yaml");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            binding.unbind().toCompletableFuture().join();
            actorSystem.terminate();
        }));

        new CountDownLatch(1).await();
    }
}
