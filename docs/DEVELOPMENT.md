# Development guide

The model-library OpenAPI document is the public contract:
[`model-libary-open-api-spec.yaml`](../model-library-server-java/src/main/resources/open-api/model-libary-open-api-spec.yaml).
The committed `libary` filename is intentional.

## Ownership boundaries

- The **model library** starts remote model runners only. It requires a
  coordinator/orchestrator-supplied `runId` and rejects collisions. It does not
  offer `/v1/runner` orchestration routes or manage local coordinator models.
- An external **coordinator** owns DEVS Java composition. Local sub-models are
  connected directly through framework actors, simulators, couplings, and
  coordinators, without a model-library HTTP call or Kafka proxy.
- The optional **coordinator helper** is a separate application/module that owns
  the status of coordinated execution across one coordinator and registered
  remote runners. It is not a replacement for the coordinator application's
  direct DEVS composition.

`simulationId` identifies an experiment, `runId` identifies one immutable
execution, `modelInstanceId` is the remote runner receiver identity, and
`coordinatorId` is the coordinator receiver identity.

## Model-library HTTP surface

- `GET /v1/models`, `GET /v1/models/{modelId}`
- `PUT /v1/models/{modelId}/run`
- `GET /v1/runs`, `GET` and `DELETE /v1/runs/{runId}`
- `GET /v1/run-config/defaults`

The start request requires `kafka.topic` and
`kafka.properties["bootstrap.servers"]`, plus the required run and simulation
identities including `timeMode`. The properties map uses native Kafka client
keys such as `security.protocol`, `sasl.mechanism`, and `sasl.jaas.config`;
the server derives `group.id` as `runId:modelInstanceId`. `coordinatorHelper` is
optional and, if supplied, requires both `endpoint` and `token`. It is strictly
an HTTP callback control-plane configuration; it never changes the Kafka
transport contract.

`KafkaIsoRunMonitor` uses `runId:modelInstanceId` as its consumer group and
checks `X-Run-Id` before JSON deserialization. It preserves externally owned
record-key, `X-Run-Id`, and receiver-addressing semantics. `ModelTerminated`
and terminal `ErrorReport` observations update only remote-runner diagnostics
and optional helper callbacks.

## Coordinator helper

The `coordinator-helper` Maven module owns:

- remote-runner registration, including start URL, credentials, initialization
  parameters, and Kafka configuration;
- fan-out of remote model-library starts and cancellations;
- authenticated idempotent remote-runner callbacks and coordinator reports;
- aggregation into `accepted`, `starting`, `ready`, `running`, `completed`,
  `failed`, and `canceled`.

Its standalone OpenAPI document is
[`coordinator-helper-open-api-spec.yaml`](../coordinator-helper/src/main/resources/open-api/coordinator-helper-open-api-spec.yaml).

The callback endpoint is configured independently from `kafka`, normally with
`COORDINATOR_HELPER_CALLBACK_ENDPOINT`. The model library calls
`POST /v1/coordinated-runs/{runId}/remote-runners/{modelInstanceId}/events`
with `X-Coordinator-Helper-Token`. Coordinator reports use the corresponding
`/coordinator/events` endpoint. A production deployment must provide a durable
store, authorization policy, and a real `CoordinatorController`; the packaged
controller is intentionally a no-op seam.

## Clerk example and protocol limits

`stub` remains the default executor. `example-clerk` constructs
`new ClerkModel(modelInstanceId)` using locally copied and attributed source
from `simlytics-cloud/devs-sf-store-java`; its provenance is retained in
[`EXAMPLE_STORE_SOURCES.md`](../model-library-server-java/src/main/java/iso/sim/server/example/store/EXAMPLE_STORE_SOURCES.md).
There is no Store artifact dependency. `InProcessClerkRuntime` is a test seam,
not a DEVS/Kafka remote runner. Do not represent it as a production integration
until an appropriate DEVS Streaming Framework Java Kafka adapter is selected.

## Validation

```sh
mvn -pl model-library-server-java test
mvn -pl coordinator-helper test
cd model-library-ui && npm run build
mvn package
```

Focused server tests cover run-ID collision protection, direct Kafka request
validation, remote-runner callback events, Kafka record/header preservation and
header-first filtering, and removal of `/v1/runner` routes. Coordinator-helper
tests cover fan-out, lifecycle aggregation, authorization, cancellation, and
the HTTP API.
