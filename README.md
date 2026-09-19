# Model Library Server

An OpenAPI 3.1 HTTP service and bundled Angular UI for discovering simulation models, submitting runs, and polling or cancelling those runs. The public contract is [the OpenAPI specification](model-library-server-java/src/main/resources/open-api/model-libary-open-api-spec.yaml).

This guide is for operators who want to build and host the server. For architecture, development status, and contributor or AI-agent guidance, see [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).

## Current hosting readiness

The server routes, catalog API, run lifecycle API, UI, and Kafka run monitor are implemented. This repository is **not yet a complete, production-ready model host**:

- The default catalog is a single demonstration model copied from the test fixture.
- The default `stub` runtime executor does not launch a simulation model.
- Run state is stored only in memory.
- The project has ISO-21175-aligned message handling, but does not claim ISO-21175 conformance or certification.

These are known product gaps, not configuration steps. Their implementation status and follow-up actions are recorded in the developer guide.

## Prerequisites

- Java 21
- Maven
- Network access during the first UI build, so Maven can provision Node.js 20.11.1 and npm 10.2.4 and install the Angular dependencies

## Build

From the repository root:

```sh
mvn package
```

The Maven reactor builds both modules:

- `model-library-ui` builds the Angular application and packages it as server classpath resources.
- `model-library-server-java` builds the Pekko HTTP server.

## Configure model catalogs

The server reads catalog resources from the `model.library.catalog.resources` list in [`model-library-server-java/src/main/resources/application.conf`](model-library-server-java/src/main/resources/application.conf). The repository default is:

```hocon
model.library.catalog.resources = ["classpath:data/model-catalog.json"]
```

`data/model-catalog.json` is a copy of the test model catalog and supplies a `Vehicle` demonstration model. Maven packages `data/` on the server classpath, so the default works from the repository and from the built server artifact.

Catalog entries beginning with `classpath:` identify packaged resources. Entries without that prefix are file-system paths. Configure one or more deployed catalog files by changing the list:

```hocon
model.library.catalog.resources = [
  "/srv/model-library/catalogs/vehicle.json",
  "/srv/model-library/catalogs/fleet.json"
]
```

Each file must contain an object with a non-empty `models` array. Every model requires at least a non-empty `modelId` and `name`; the OpenAPI contract describes the full model shape, including ports, parameters, schemas, and defaults. The server fails at startup when any configured catalog file is missing, unreadable, malformed, empty, or invalid.

## Configure and start

The committed HOCON configuration is at [`model-library-server-java/src/main/resources/application.conf`](model-library-server-java/src/main/resources/application.conf). Its defaults are:

| Setting | Default |
| --- | --- |
| HTTP host | `127.0.0.1` |
| HTTP port | `8090` |
| Catalog resources | `classpath:data/model-catalog.json` |
| Runtime executor | `stub` |
| Kafka bootstrap servers | `localhost:9092` |
| Kafka topic | `devs-sim` |

Override the host and port with system properties or environment variables:

```sh
mvn -pl model-library-server-java exec:java \
  -Dmodel.library.host=0.0.0.0 \
  -Dmodel.library.port=8090
```

Equivalent environment variables are `MODEL_LIBRARY_HOST` and `MODEL_LIBRARY_PORT`. The server fails at startup if the configured runtime executor is unsupported. The only built-in executor value is `stub`.

## Use the hosted service

After startup, the following resources are available:

| Resource | URL |
| --- | --- |
| Angular UI | `http://127.0.0.1:8090/ui/` |
| Swagger UI | `http://127.0.0.1:8090/swagger` |
| OpenAPI document | `http://127.0.0.1:8090/openapi.yaml` |
| Model catalog | `http://127.0.0.1:8090/v1/models` |
| Run list | `http://127.0.0.1:8090/v1/runs` |
| Run-configuration defaults | `http://127.0.0.1:8090/v1/run-config/defaults` |

## Access and use the web UI

After starting the server, open `http://<configured-host>:<configured-port>/ui/` in a browser. With the default configuration, this is [http://127.0.0.1:8090/ui/](http://127.0.0.1:8090/ui/). The UI is packaged with the server; it does not need a separate deployment or startup command.

1. Select **Models**, then choose **View Details** for a catalog model. Review its description, ports, parameters, time mode, and metadata.
2. Select **Configure Run**. Enter a unique run ID, supply the catalog-provided initialization parameters, and complete the simulation context and Kafka settings. The dialog pre-populates Kafka values from the server defaults; change them when the target simulation uses a different broker or topic.
3. Select **Start Run**. The UI confirms the accepted run ID. Select **Runs** to view each run's status, accepted time, and current simulation time; the list refreshes automatically.
4. Open a run to view its details. To stop a non-terminal run, use its delete button and confirm the action; this sends the cancellation request and changes the run status to `canceled`.

The default `stub` executor accepts runs but does not launch a simulation model. To observe a run advance or complete, deploy a runtime executor and Kafka messages appropriate to the model; see [Run monitoring and ISO-21175 scope](#run-monitoring-and-iso-21175-scope).

For example:

```sh
curl http://127.0.0.1:8090/v1/models
```

The OpenAPI document is authoritative for request and response schemas. In particular, `PUT /v1/models/{modelId}/run` requires `initializationParameters` and Kafka `bootstrapServers` and `topic`, then returns a pollable run URL. Use the hosted Swagger UI to inspect the model-specific initialization schema before submitting a run.

## Run monitoring and ISO-21175 scope

After a run becomes ready, the server monitors the Kafka topic in the run request. It derives the consumer group as `runId:receiverId`, where `receiverId` is the simulation `modelInstanceId`; callers cannot set Kafka `group.id`.

| Message for the current `simulationRunId` | Run result |
| --- | --- |
| `NextInternalTimeReport` | A `ready` run becomes `running`; numeric logical time updates `currentSimulationTime`. |
| `ModelTerminated` | The run becomes `completed` and allocated runtime and monitor resources are stopped. |
| `ErrorReport` with `error` or `fatal` severity | The run becomes `failed` and allocated resources are stopped. |

Malformed messages, messages for another run, and non-terminal message types are ignored. `DELETE /v1/runs/{runId}` stops a non-terminal run and marks it `canceled`.

The default executor does not launch a model, and monitor polling, readiness timeouts, shutdown cleanup, process supervision, and broker-backed integration testing are not yet production-hardened. This is ISO-21175-aligned message handling, not a statement of full protocol coverage or conformance. Review the detailed implementation record and open work in [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) before relying on it for cross-organization integration.
