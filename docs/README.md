# Model Library Development README

This document defines the implementation plan for the **Model Library** capability in Foundry, with a focus on:

- A **Pekko HTTP backend API server**
- An **Angular UI client** for browsing and launching model runs
- A **Pekko HTTP execution client** for programmatic remote model execution

This plan now also targets:

- Alignment with the ISO-21175 simulation interoperability vision
- Externalization of Phase 1 server/client capabilities for non-Foundry users

It is written to be:

- **Human-readable** for architecture and delivery planning
- **AI-readable** for stepwise code generation and testing

This README is a living document and should be updated as phases are completed and lessons are learned.

---

## 1. Scope and Goals

### 1.1 Primary feature goals

Implement a model library service that complies with the OpenAPI contract in:

- `model-library/open-api/model-libary-open-api-spec.yaml`

Required endpoints:

- `GET /v1/models`
- `GET /v1/models/{modelId}`
- `PUT /v1/models/{modelId}/run`
- `GET /v1/runs/{runId}` (status polling for accepted/pending runs)

### 1.2 Initial behavior goals

- Serve model metadata from a generic configuration source (JSON-based in early phases).
- Return model details with ports, parameters, message schemas, and initialization schema.
- Accept run requests with initialization + Kafka context.
- Validate requests and return expected response/error payloads.
- Keep execution runtime behind an abstract runner interface.

### 1.3 External Phase 1 product goals

- Deliver Phase 1 as a standalone integration surface, not an internal-only prototype.
- Allow external users to host model catalogs on the server without Foundry runtime dependencies.
- Allow external users to call hosted models through a distributable execution client.
- Keep public API behavior stable while internal Foundry modules continue to evolve.

### 1.4 ISO-21175 alignment goals (high level)

- Use OpenAPI-first contracts as a deterministic interoperability boundary.
- Preserve explicit simulation-context semantics (`simulationId`, `federationId`, `timeMode`) for federation integration.
- Keep transport payloads implementation-neutral so model hosting/execution can interoperate across organizations.
- Add conformance-focused validation and regression tests as a release gate.

### 1.5 SysML alignment goals (incremental)

For `model-library/open-api/irp-system.sysml`:

- `/v1/models` should include all DEVS models marked `DevsAtomicModel` and `DevsCoupledModel`.
- Model IDs should be stable and qualified (example: `irpsystem.irpmodel.Vehicle`).
- Model schemas should align with generated DTO/domain semantics.

---

## 2. Technology Baseline

### 2.1 Backend API server

- **Apache Pekko HTTP** for REST API implementation
- **Apache Pekko Actors/Streams** for async orchestration and execution lifecycle
- OpenAPI-first workflow (spec as contract source)

### 2.2 Frontend UI

- **Angular** as the server UI technology
- UI consumes backend API only (no direct execution/runtime coupling)

### 2.3 Execution client

- **Pekko HTTP** transport client
- Shared core logic for model discovery, payload preparation, validation, and run invocation

### 2.4 DTO and model contract strategy

- Reuse generated DTOs from `devs-codegen` where applicable (including mutable/immutable variants).
- Keep transport DTOs (OpenAPI-facing) separated from domain DTOs (SysML/codegen-facing) via explicit mappers.

### 2.5 Productization baseline (external users)

- Package server and client as versioned, standalone artifacts suitable for external distribution.
- Treat OpenAPI contract as a public API with semantic versioning and deprecation policy.
- Provide baseline security hooks (authn/authz integration points, TLS deployment assumptions, secret-safe logging).
- Provide operational defaults (health/readiness, structured logs, correlation IDs, timeout/retry policies).

### 2.6 API exploration and test UI baseline

- Host OpenAPI spec directly from server (for example `/openapi.yaml`).
- Host Swagger UI from server (for example `/swagger`) for interactive endpoint testing.
- Keep Swagger UI enabled in development and integration environments to support phase-by-phase validation.
- Treat Swagger UI as a supplemental operator/test UI; Angular remains the primary product UI.

### 2.7 Local server run command (Phase 1)

- Start server outside tests (from repository root):
  - `source ./setup-env.sh && /opt/homebrew/bin/mvn -pl model-library -DskipTests exec:java -Dexec.mainClass=iso.sim.server.ModelLibraryServerMain`
- Optional overrides:
  - `-Dmodel.library.host=0.0.0.0`
  - `-Dmodel.library.port=8090`
- Default endpoints after startup:
  - Swagger placeholder UI: `http://127.0.0.1:8090/swagger`
  - OpenAPI spec: `http://127.0.0.1:8090/openapi.yaml`

---

## 3. Architecture (Target Shape)

## 3.1 Components

1. **Model Library API Server (Pekko HTTP)**
   - Model catalog provider
   - Model detail provider
   - Run request validator
   - Runner factory + runner lifecycle manager
   - OpenAPI/Swagger hosting route for interactive API testing

2. **Execution Client (Pekko HTTP)**
   - List/get/run API client
   - Request builder from model metadata
   - Error mapping and retry policy

3. **Angular UI**
   - Model list page
   - Model details page
   - Run form + submission feedback

4. **External Integration Boundary**
   - Public contract module (OpenAPI + versioning policy)
   - Packaging/distribution metadata (server, client, UI)
   - Compatibility and conformance test harness

### 3.2 Data sources and inputs

- OpenAPI spec: endpoint + payload contract
- JSON model catalog (early phases)
- SysML source (`irp-system.sysml`) + generated DTO/domain model (later phases)
- ISO-21175 alignment checklist and interoperability test cases (phase-gated)

### 3.3 Execution abstraction

Define an abstract runner contract (name TBD) with:

- Input: resolved model definition + start run request
- Output: run acceptance result (`runId`, `status`, `statusUrl`, message)
- Implementations to evolve by phase (stub -> local runtime -> proxy integration)

### 3.4 Boundary and portability rules

- No Foundry-internal types exposed in public API payloads.
- External-server operation must work with configuration and plugins only.
- Execution client must operate as standalone SDK/CLI integration surface.
- Mapper boundaries must isolate OpenAPI DTOs from SysML/codegen domain DTO evolution.

---

## 4. Development Phases

Each phase has clear build goals and validation gates.

## Phase 1A — Contract and scaffolding

### Build goals

- Freeze/confirm the OpenAPI contract for model library endpoints.
- Define public API versioning/deprecation policy for external users.
- Create API module structure for Pekko HTTP server.
- Add server routes to host OpenAPI spec and Swagger UI.
- Create Angular UI shell project in model-library scope.
- Create Pekko HTTP execution client module skeleton.

### Initial decisions (recorded)

- Server and client implementation modules: `model-library`.
- Top-level packages: `iso.sim.server` (server) and `iso.sim.client` (client).
- UI implementation module: `model-library-ui`.
- UI top-level package: `iso.sim.ui`.
- JSON catalog draft files should live in each module's `resources` directory for now.
- `GET /v1/models` ordering preference: alphabetical by `modelId`.
- OpenAPI spec and sample JSON fixtures are editable drafts and may be updated to support server/client implementation needs.

### API versioning policy (Phase 1)

- Phase 1 publishes the public API under `/v1`.
- Non-breaking changes may be introduced within `v1`.
- Breaking changes require a new path version (for example `/v2`).
- Older versions must have a deprecation window before removal.

### Validation gates

- Spec parses and validates.
- Server routes compile with placeholder handlers.
- Swagger UI loads and points to hosted OpenAPI spec.
- Angular app builds and serves a placeholder page.
- Client compiles and can call a mock endpoint.
- Initial interoperability checklist documented (including ISO-21175 alignment targets).

## Phase 1B — Generic JSON-backed model catalog server

### Build goals

- Implement `GET /v1/models` and `GET /v1/models/{modelId}`.
- Load models from JSON configuration.
- Return OpenAPI-compliant payloads and errors.
- Ensure configuration and error messages are external-user readable and self-service.

### Validation gates

- Contract tests for list/get responses.
- 404 behavior validated for unknown model.
- Swagger "Try it out" validates list/get behavior against running server.
- Sample response parity with:
  - `model-library/irpsystem.irpmodel.Vehicle.get-model-response.json`
- Public API compatibility checks recorded for release notes.

### Current reality snapshot (after 1A/1B)

- Implemented and test-covered in server:
  - `GET /v1/models`
  - `GET /v1/models/{modelId}`
  - hosted docs endpoints (`/openapi.yaml`, `/swagger`)
- Server is runnable outside tests via `ModelLibraryServerMain`.
- Default server port is HOCON-driven (`model.library.server.port`) with default `8090`.
- `model-library-ui` is currently a Kotlin placeholder module, not a real Angular app yet.

## Phase 1C — Generic run endpoint and abstract runner

### Build goals

- Implement `PUT /v1/models/{modelId}/run`.
- Return `202 Accepted` when request validation passes and run startup is queued.
- Return a pollable run reference (`runId` + `statusUrl`) in the 202 response.
- Implement `GET /v1/runs/{runId}` for status polling.
- Validate `initializationParameters` against model `initializationSchema`.
- Validate required Kafka section.
- Delegate execution to abstract runner implementation (stub runner initially).
- Add explicit error taxonomy and codes for integration failures.

### Two-phase success model (agreed)

- Phase A success (synchronous API response): request is valid and accepted (`202`).
- Phase B success (asynchronous runtime milestone): runner/runtime reaches `ready` state.
- The second success is represented by state transition on `GET /v1/runs/{runId}` (not by a second HTTP response to PUT).

### Run lifecycle states for 1C contract

- `accepted`: request validated and accepted.
- `starting`: runner startup/provisioning in progress.
- `ready`: runtime notified that runner is execution-ready.
- `running`: execution active.
- Terminal states: `completed`, `failed`, `canceled`.

### 1C API behavior guidance

- `PUT /v1/models/{modelId}/run`:
  - success: `202` with body containing `runId`, `modelId`, `status`, `statusUrl`, optional `acceptedAt`, optional `message`.
  - include `Location` header with the run status URL (for example `/v1/runs/{runId}`).
- `GET /v1/runs/{runId}`:
  - returns current status payload for lifecycle polling.
  - returns `404` with `ErrorResponse` for unknown run IDs.
- Keep `ErrorResponse` shape stable (`code`, `message`) for all 4xx/5xx cases.

### Validation gates

- 202/400/404 responses verified against contract.
- Request parity with:
  - `model-library/irpsystem.irpmodel.Vehicle.put-run-request.json`
- `Location` header and 202 response fields verified (`runId`, `status`, `statusUrl`).
- Status polling endpoint validated (`GET /v1/runs/{runId}` for known + unknown IDs).
- Lifecycle status fields validated (`accepted`, `starting`, `ready`, `running`, terminal states).
- Interop-focused negative tests for malformed context and schema mismatch.

## Phase 1D — Execution client implementation (Pekko HTTP)

### Build goals

- Implement typed client operations for list/get/run.
- Add consistent timeout/retry/error mapping policy.
- Add model-aware request builder flow.
- Package client for external consumption (library first; CLI optional).

### Validation gates

- Client integration tests against local server instance.
- Negative tests for 400/404 handling.
- External integration quickstart validated end-to-end.

### Current implementation status (completed)

- `iso.sim.client.ModelLibraryClient` now exposes typed operations:
  - `listModels()`
  - `getModel(modelId)`
  - `runModel(modelId, request)`
  - `getRunStatus(runId)`
- Request/response JSON mapping is handled in-client using Jackson Kotlin module.
- Non-2xx responses are mapped to `ModelLibraryClientException` with:
  - `httpStatus`
  - `errorCode` (when server returns `ErrorResponse`)
  - `errorMessagePayload`
  - `rawBody`

### Timeout/retry/error policy (Phase 1D baseline)

- Config type: `ModelLibraryClientConfig`.
- `requestTimeoutMillis` default: `5000`.
- `maxGetRetries` default: `1`.
- Retry behavior:
  - Applied to idempotent GET operations only (`list/get/getRunStatus`).
  - Triggered on transport failure and HTTP 5xx responses.
  - No automatic retry on `runModel` (non-idempotent request).

### External quickstart (Kotlin/JVM)

```kotlin
val system = ActorSystem.create("model-library-client-quickstart")
val client = ModelLibraryClient(
    baseUrl = "http://127.0.0.1:8090",
    actorSystem = system,
    config = ModelLibraryClientConfig(
        requestTimeoutMillis = 5_000,
        maxGetRetries = 1
    )
)

val models = client.listModels().toCompletableFuture().get()
val vehicle = client.getModel("irpsystem.irpmodel.Vehicle").toCompletableFuture().get()

val runRequest = StartModelRunRequest(
    initializationParameters = mapOf("initialSpeed" to 0),
    kafka = KafkaConfigurationDto(
        bootstrapServers = "localhost:9092",
        topic = "vehicle-runs"
    )
)

val run = client.runModel(vehicle.modelId, runRequest).toCompletableFuture().get()
val status = client.getRunStatus(run.runId).toCompletableFuture().get()
```

Validation evidence for this phase is currently covered by:

- `iso.sim.client.ModelLibraryClientServerIntegrationTest` (happy path + 400/404 mapping)

## Phase 1E — Runtime execution integration

### Build goals

- Add a runtime execution boundary in `model-library` server that is implementation-neutral.
- Keep default runtime behavior as a deterministic stub (no real model launch yet).
- Keep run status persistence behind a store seam for future replacement.
- Preserve portability so server/client can move to another repository without Foundry runtime coupling.

### Validation gates

- `PUT /v1/models/{modelId}/run` still returns `202` with `runId` and `statusUrl`.
- `GET /v1/runs/{runId}` returns the persisted stub status for accepted runs.
- Runtime boundary invocation is covered by unit test(s) and client-server integration test(s).

### Current implementation status (minimal stub complete)

- `RunService` now delegates model start to `RunExecutor`.
- Default implementation: `StubRunExecutor` (intentionally no-op for Phase 1E stub).
- Run status persistence now uses `RunStatusStore` with `InMemoryRunStatusStore` default.
- Runtime selector is configured via HOCON:
  - `model.library.runtime.executor = "stub"`
- Unsupported executor values fail fast at startup to avoid silent misconfiguration.
- No dependency was introduced on other `devs-foundry-dsl` modules or DEVS Streaming Framework.

## Phase 1F — Angular UI implementation

### Build goals

- Model list page (`GET /v1/models`).
- Model detail page (`GET /v1/models/{modelId}`).
- Run form page (`PUT /v1/models/{modelId}/run`) with schema-informed UX.
- Add operator-focused diagnostics for request validation and run acceptance feedback.

### Validation gates

- UI e2e smoke tests for list/detail/run happy path.
- Error handling for invalid run payload and missing model.
- External demo flow validated without Foundry-only dependencies.

## Phase 2 — SysML-driven model catalog generation

### Build goals

- Generate model catalog from SysML (`irp-system.sysml`) and codegen outputs.
- Include atomic + coupled models based on metadata markers.
- Generate consistent schemas and parameter metadata.
- Preserve public API compatibility while swapping internal catalog source to SysML/codegen.

### Validation gates

- `/v1/models` includes expected IRP model set.
- `Vehicle` model response matches expected shape and semantics.
- No public contract drift introduced by codegen pipeline updates.

## Phase 4 — ISO-21175 interoperability hardening

### Build goals

- Formalize ISO-21175 alignment profile for supported interactions in this product increment.
- Add conformance scenarios that exercise model discovery, model definition retrieval, and run initiation across federation context.
- Document interoperability assumptions, unsupported areas, and extension points.

### Validation gates

- Conformance suite passes for declared ISO-21175-aligned behaviors.
- Traceability matrix links API operations and payload semantics to interoperability requirements.
- External integration guide includes ISO-21175-focused usage patterns and constraints.

---

## 5. Test Strategy by Layer

### 5.1 Server tests

- Route-level tests for HTTP status/payload contract compliance.
- JSON/schema validation tests for initialization payloads.
- Runner adapter contract tests (stub and real implementations).

### 5.2 Client tests

- Unit tests for request/response mapping and error handling.
- Integration tests against local API server.

### 5.3 UI tests

- Component tests for list/detail/run pages.
- E2E smoke tests covering top user workflows.

### 5.4 Contract regression tests

- Contract examples should include `Vehicle` get/run fixtures.
- CI should fail on contract drift between implementation and OpenAPI spec.

### 5.5 Interoperability and externalization tests

- End-to-end tests executed from an external-user perspective (no Foundry internals assumed).
- Backward-compatibility tests for public API versions.
- ISO-21175 alignment scenarios included in CI for declared supported scope.

---

## 6. AI Code Generation Guidance

Use this section to drive AI-assisted implementation safely and incrementally.

### 6.1 Prompt boundaries for AI coding

- Implement one phase at a time.
- Keep changes minimal and test-backed.
- Do not bypass failing tests.
- Keep OpenAPI response/error shapes exact.

### 6.2 Required AI outputs per phase

For each phase, AI should produce:

1. Code changes for only the targeted phase scope
2. Tests for new/changed behavior
3. Run instructions and executed verification evidence
4. Short phase completion notes and known gaps

### 6.3 AI verification checklist

- Endpoints match operation IDs and response schemas.
- 400/404 errors return `ErrorResponse` shape.
- Example payloads from model-library fixtures pass.
- New code compiles and tests are green for impacted modules.
- Public API compatibility constraints are preserved.
- External-user docs and packaging instructions stay consistent with implemented behavior.

---

## 7. Definition of Done (Current Increment)

For any completed phase, all must be true:

- Build is green for affected modules.
- Tests for changed behavior are green.
- OpenAPI contract behavior is preserved.
- Public artifact packaging and install/use instructions are validated.
- Interoperability checks pass for declared ISO-21175-supported scope.
- README updated with:
  - phase status
  - key decisions
  - deviations from plan
  - follow-up actions

---

## 8. Working Conventions for Ongoing Updates

When this document is updated after each phase, append:

1. **Phase status**: Not started / In progress / Complete
2. **What changed**: architecture or implementation decisions
3. **What was learned**: constraints, risks, simplifications
4. **Next-phase adjustments**: scope or validation updates
5. **Interoperability impact**: ISO-21175 alignment decisions, additions, or deferred items
6. **Externalization impact**: packaging/compatibility/supportability decisions

Keep this README as the authoritative model-library development plan unless superseded by a more specific phase document.