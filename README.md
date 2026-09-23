# Sim Model Library

This repository hosts a **remote model library**: it discovers models and starts
remote model runners that communicate with an external DEVS coordinator through
the existing DEVS Streaming Kafka transport. It does not compose or manage local
coordinator sub-models. A coordinator that hosts local sub-models attaches them
directly through its DEVS Java framework APIs.

The model-library contract is [OpenAPI](model-library-server-java/src/main/resources/open-api/model-libary-open-api-spec.yaml).

## Remote model-run request

`PUT /v1/models/{modelId}/run` starts one remote model runner. It requires a
caller-supplied, immutable `runId`, `initializationParameters`, direct `kafka`
configuration, and `simulation.simulationId`, `modelInstanceId`,
`coordinatorId`, and `timeMode`. The model library never generates a
replacement run ID and rejects every duplicate local ID.

`kafka` is transport configuration, not a control-plane wrapper. The runner
uses consumer group `runId:modelInstanceId`; `runId` remains the Kafka key and
`X-Run-Id` value, and Kafka records are filtered by that header before their
ISO-21175 payload is deserialized. `coordinatorId` is the receiver target for
coordinator-bound simulation messages. This project does not modify Kafka
headers, payloads, or control messages in the externally owned simulation
protocol.

An optional, separate HTTP callback is available only when a coordinator helper
is used:

```json
"coordinatorHelper": {
  "endpoint": "https://helper.example",
  "token": "remote-runner-callback-token"
}
```

It reports remote-runner accepted, starting, ready, stopped, and failed facts to
the helper. It is deliberately independent of Kafka and best-effort: an
unreachable callback does not alter the local remote-runner lifecycle.

`GET /v1/runs` and `GET`/`DELETE /v1/runs/{runId}` expose remote-runner
diagnostics only (`accepted`, `starting`, `locally-ready`, `locally-stopped`,
`locally-failed`). They do not describe the coordinated execution. There are no
`/v1/runner` routes in the model library.

## Optional coordinator helper

`coordinator-helper` is a separate Maven module with no dependency from the
model-library server. It owns coordinated execution state and optionally drives
an application-provided coordinator controller. Its HTTP API is:

- `POST /v1/coordinated-runs`
- `POST /v1/coordinated-runs/{runId}/remote-runners`
- `POST /v1/coordinated-runs/{runId}/start`
- `POST /v1/coordinated-runs/{runId}/remote-runners/{modelInstanceId}/events`
- `POST /v1/coordinated-runs/{runId}/coordinator/events`
- `GET` and `DELETE /v1/coordinated-runs/{runId}`

Its OpenAPI contract is
[`coordinator-helper-open-api-spec.yaml`](coordinator-helper/src/main/resources/open-api/coordinator-helper-open-api-spec.yaml)
and is served at `/openapi.yaml` when the helper is running.

Remote-runner registration includes the model-library URL, model and receiver
identity, initialization parameters, Kafka configuration, and callback token.
Starting a coordinated run fans out the model-library start requests with the
shared `runId`, `coordinatorId`, and Kafka settings. The helper aggregates
remote readiness/failure/termination reports with coordinator reports into
`accepted`, `starting`, `ready`, `running`, `completed`, `failed`, and
`canceled`, and fans out HTTP cleanup on cancellation or failure. It never
adds control traffic to the simulation topic.

The helper defaults to port `8091`. Set
`COORDINATOR_HELPER_CALLBACK_ENDPOINT` (or
`-Dcoordinator.helper.callback.endpoint`) to its externally reachable base URL
when it is behind a proxy.

## Runtime status

The default `stub` executor accepts a request but launches no simulation model.
The opt-in `example-clerk` executor uses locally copied, attributed Clerk source
without a Store artifact dependency. Its `InProcessClerkRuntime` is explicitly a
non-production test seam; it is **not** a remote Kafka runner. A real Clerk
adapter requires a selected DEVS Streaming Framework Java Kafka integration and
has not been invented here.

Run state is in memory. This repository has ISO-21175-aligned observation
handling but makes no conformance or certification claim.

## Build and run

Prerequisites: Java 21, Maven, and network access for the first Angular build.

```sh
mvn package
mvn -pl model-library-server-java exec:java
# Optional, separately deployed:
mvn -pl coordinator-helper exec:java
```

The model-library UI is served at `http://127.0.0.1:8090/ui/`. The development
guide has focused test commands and implementation details:
[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).

Licensed under Apache License 2.0. See [LICENSE](LICENSE).
