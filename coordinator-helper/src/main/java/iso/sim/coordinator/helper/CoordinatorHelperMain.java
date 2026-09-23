package iso.sim.coordinator.helper;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.http.javadsl.Http;

import java.util.concurrent.CountDownLatch;

public final class CoordinatorHelperMain {
    private CoordinatorHelperMain() {
    }

    public static void main(String[] args) throws Exception {
        Config config = ConfigFactory.load();
        String host = System.getProperty(
            "coordinator.helper.host", System.getenv().getOrDefault("COORDINATOR_HELPER_HOST", "127.0.0.1")
        );
        int port = Integer.parseInt(System.getProperty(
            "coordinator.helper.port",
            System.getenv().getOrDefault("COORDINATOR_HELPER_PORT", String.valueOf(config.getInt("coordinator.helper.server.port")))
        ));
        String callbackEndpoint = System.getProperty(
            "coordinator.helper.callback.endpoint",
            System.getenv().getOrDefault(
                "COORDINATOR_HELPER_CALLBACK_ENDPOINT", config.getString("coordinator.helper.callback.endpoint")
            )
        );

        ActorSystem actorSystem = ActorSystem.create("coordinator-helper");
        CoordinatedRunService service = new CoordinatedRunService(callbackEndpoint);
        var binding = Http.get(actorSystem)
            .newServerAt(host, port)
            .bind(new CoordinatorHelperRoutes(service).routes().function(actorSystem))
            .toCompletableFuture()
            .get();

        System.out.println("Coordinator helper running at http://" + host + ":" + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            binding.unbind().toCompletableFuture().join();
            actorSystem.terminate();
        }));
        new CountDownLatch(1).await();
    }
}
