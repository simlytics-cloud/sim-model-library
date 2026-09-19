/*
 * Sim Model Library Copyright (C) 2026 simlytics.cloud LLC and
 * Sim Model Library contributors.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License
 *  is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing permissions and limitations under
 *  the License.
 *
 */

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
        ModelCatalogService service = new ModelCatalogService(
            new CatalogRepository(config.getStringList("model.library.catalog.resources"))
        );
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
