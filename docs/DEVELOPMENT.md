# Development and AI-agent guide

This document is the implementation record and contribution guide for the Model Library Server. It is intentionally separate from the [operator README](../README.md), which explains how to build and host the service.

## Source of truth

When documentation, plans, and code disagree, use the following evidence in order:

1. The committed [OpenAPI 3.1 contract](../model-library-server-java/src/main/resources/open-api/model-libary-open-api-spec.yaml).
2. The server routes, services, and resource files in `model-library-server-java/src/main`.
3. The Maven build descriptors and Angular source in `model-library-ui`.
4. Existing targeted tests in `model-library-server-java/src/test`.

The OpenAPI filename contains the committed `libary` spelling. Preserve that path when linking to it unless the file itself is deliberately renamed in a coordinated change.

## Implemented architecture

| Area | Implementation |
| --- | --- |
| HTTP server | Java 21 Apache Pekko HTTP application started by `iso.sim.server.ModelLibraryServerMain`. |
| API contract | OpenAPI 3.1 document hosted at `/openapi.yaml`; Swagger UI hosted at `/swagger`. |
| Catalog | `CatalogRepository` loads the classpath or file-system locations in `model.library.catalog.resources`, and `ModelCatalogService` serves model summaries and details. |
| Run lifecycle | `RunService`, `RunLifecycleManager`, and `RunStatusStore` accept, start, ready, run, complete, fail, and cancel runs. |
| Runtime boundary | `RunExecutor` and `RunHandle` isolate execution. The default `StubRunExecutor` creates a no-op handle. |
| Run monitoring | `KafkaIsoRunMonitor` reads Kafka records, parses ISO-21175-shaped messages, filters by run ID, updates time, and applies selected lifecycle transitions. |
| UI | Angular UI for model browsing, run configuration, run status polling, and cancellation; served under `/ui/`. |
| Packaging | Maven packages the Angular build as a dependency of the server. |

### Public HTTP surface

The currently routed operations are:

- `GET /v1/models`
- `GET /v1/models/{modelId}`
- `PUT /v1/models/{modelId}/run`
- `GET /v1/runs`
- `GET /v1/runs/{runId}`
- `DELETE /v1/runs/{runId}`
- `GET /v1/run-config/defaults`

`ModelLibraryRoutesIntegrationTest` covers the principal HTTP success and 400/404 error shapes, Swagger and OpenAPI routes, UI routing, run cancellation, and current-simulation-time representation.

## Development phase record

| Phase | Status | What is present | What remains |
| --- | --- | --- | --- |
| 1A: contract and scaffolding | Partially complete | Pekko server, OpenAPI and Swagger routes, Angular module, Maven reactor. | No standalone execution-client module or implemented public API version/deprecation policy. |
| 1B: JSON model catalog | Complete | Configurable classpath or file-system catalog locations, default `classpath:data/model-catalog.json`, list/detail routes, and repository/route tests. | Replace the demonstration catalog with deployment-specific catalog files as needed. |
| 1C: generic run endpoint | Partially complete | Start, list, get, and cancel routes; lifecycle states; 202 response and `Location` header; required Kafka-field checks. | Initialization values are not validated against the model JSON Schema. |
| 1D: execution client | Not implemented | No client source module or client tests are present. | Implement and package a standalone typed client only when its API and distribution requirements are defined. |
| 1E: runtime integration | Partially complete | Executor, handle, readiness, in-memory status-store, cancellation, and Kafka-monitor seams. | Supply a real executor and durable run-state store; the default executor does not start models. |
| 1F: Angular UI | Partially complete | Model list/detail, schema-informed run form, run list/detail, polling, and cancellation. | Add UI component and end-to-end tests, plus robust user-visible error handling. |
| 2: SysML catalog generation | Not implemented | No SysML/code-generation catalog pipeline is in this repository. | Define and implement source mapping, generated metadata, and contract regression coverage. |
| 4: ISO-21175 interoperability hardening | Not implemented | Selected Kafka message parsing and lifecycle mapping exist. | Define the supported profile, traceability matrix, conformance scenarios, and release gate before making a conformance claim. |

## Run-monitoring record

The core run-monitoring design is implemented. After `RunService` accepts a request, `RunLifecycleManager` marks it `starting`, launches the `RunExecutor`, registers the returned `RunHandle`, waits for the selected readiness probe, marks it `ready`, then starts `KafkaIsoRunMonitor`. The runtime and monitor handles are combined so terminal cleanup stops both.

`KafkaIsoRunMonitor` consumes the run request's Kafka topic. It derives the consumer group as `runId:receiverId`, where `receiverId` is the simulation `modelInstanceId`; callers cannot override Kafka `group.id`. The consumer starts at `latest`, auto-commits offsets, and polls every 250 ms.

| Observed message | Effect |
| --- | --- |
| `NextInternalTimeReport` for the current `simulationRunId` | Moves a `ready` run to `running` and updates `currentSimulationTime` from numeric `nextInternalTime`, or numeric `eventTime` when needed. Older time values are ignored. |
| `ModelTerminated` for the current `simulationRunId` | Marks the run `completed` and cleans up registered resources. |
| `ErrorReport` with `payload.severity` of `error` or `fatal` | Marks the run `failed` and cleans up registered resources. |
| Malformed, unrelated, or wrong-run messages | Ignores the message. |

The monitor parses message payloads and filters by `simulationRunId`; it does not implement Kafka-header filtering. Monitor polling failures mark the run `failed` and clean up registered resources. `DELETE /v1/runs/{runId}` stops non-terminal runs and marks them `canceled`.

### Coverage and deferred monitoring work

The lifecycle, handles, registry, readiness probes, parser, monitor transitions, terminal cleanup, cancellation, and route behavior are covered by focused server tests, including `KafkaIsoRunMonitorTest`, `RunLifecycleManagerTest`, `RunLifecycleServiceTest`, and `ModelLibraryRoutesIntegrationTest`.

The following monitoring-plan work remains:

1. Add a real runtime executor; the default `StubRunExecutor` does not launch a model.
2. Supervise process-backed runtimes and define exit-before-terminal-state behavior.
3. Make monitor polling, readiness, maximum-duration, and cleanup timeouts configurable and add operational diagnostics.
4. Stop active runtime and monitor handles during server shutdown and define the resulting non-terminal status policy.
5. Add broker-backed Kafka and real-executor integration tests.

## Known actions

The following work is intentionally not implemented by the documentation split:

1. Implement JSON Schema validation for `initializationParameters` to meet the OpenAPI contract description.
2. Complete the deferred run-monitoring work listed above.
3. Replace in-memory run storage when runs must survive process restarts or support multiple server instances.
4. Add a standalone execution client and its integration tests if external programmatic client support is a product requirement.
5. Add health/readiness endpoints, security integration points, structured logging, correlation IDs, and documented deployment security assumptions before production deployment.
6. Add Angular component and end-to-end test coverage.
7. Define the SysML catalog-generation pipeline and contract regression tests.
8. Define the ISO-21175-supported interoperability profile and prove it with conformance tests and traceability.
9. Publish an API versioning, compatibility, and deprecation policy.

## Configuration and runtime behavior

`application.conf` provides the default port, catalog-resource list, executor type, and Kafka defaults. Catalog entries prefixed with `classpath:` resolve from the server classpath; all other entries are file-system paths. Maven packages the root `data/` directory as classpath resource `data/`. `ModelLibraryServerMain` reads `model.library.host` and `model.library.port` system properties first, then `MODEL_LIBRARY_HOST` and `MODEL_LIBRARY_PORT` environment variables, then the HOCON port default.

The default runtime executor is `stub`. A successful `PUT` means the server accepted a syntactically valid request and moved it through the in-process lifecycle; it does not mean a simulation model has been launched. The Kafka monitor is started after readiness and uses a run-scoped consumer group derived as `runId:receiverId`.

## Contributor and AI-agent expectations

1. Keep the OpenAPI specification, route behavior, DTOs, UI types, and tests consistent in the same change.
2. Make focused changes. Do not claim an API, runtime, or interoperability capability that lacks source code and targeted tests.
3. Preserve `ErrorResponse` (`code`, `message`) for documented 400/404 responses unless the OpenAPI contract changes deliberately.
4. Add or update the smallest relevant existing test suite for behavior changes. Server tests run with:

   ```sh
   mvn -pl model-library-server-java test
   ```

5. Build both modules when modifying their integration or packaging:

   ```sh
   mvn package
   ```

6. Update the operator README when changing user-visible build, configuration, hosting, endpoint, or limitation information.
7. Update this document when a phase status or known action changes. Mark work complete only after code and tests demonstrate it.

## ISO-21175 terminology

Use “ISO-21175-aligned message handling” only for the implemented parsing and lifecycle behavior. Do not label the product “ISO-21175 compliant,” “conformant,” or “certified” until the open interoperability actions above are complete and supported by documented conformance evidence.
