


text accepted -> starting -> ready -> running -> completed \\
\\ -> failed \\ -> failed -> failed
starting/ready/running -> canceled

Use only these public status values:

```
text accepted, starting, ready, running, completed, failed, canceled
```

Do not use alternate values such as `complete`.

---

## Step 1 — Normalize run status handling

### Build goals

- Add a single lifecycle/status service responsible for all run status transitions.
- Stop letting individual executors directly invent or persist arbitrary status strings.
- Ensure all status values match the OpenAPI `RunStatus` enum.
- Add timestamp support for:
  - `acceptedAt`
  - `readyAt`
  - `startedAt`
  - `completedAt`
- Add or preserve a human-readable `message`.

### Implementation guidance

Create a component such as:

```
text RunLifecycleService
```

With methods similar to:

```
text markAccepted(context, message) markStarting(context, message) markReady(context, message) markRunning(context, message) markCompleted(context, message) markFailed(context, message) markCanceled(context, message)
```

Each method should update `RunStatusStore`.

### Validation gates

- Existing run endpoint tests still pass.
- Add tests proving only valid status values are persisted.
- Add tests for transition methods and timestamps.
- Confirm `completed` is used instead of `complete`.

---

## Step 2 — Introduce run resource handles

### Build goals

- Add a general abstraction for resources allocated by a run.
- Enable cleanup without knowing whether the run used a process, simulator object, container, Helm release, or remote job.

### Implementation guidance

Create an interface such as:

```
text RunHandle
```

Responsibilities:

```
text runId() isAlive() stop() close()
```

Create initial implementations:

```
text NoopRunHandle ProcessRunHandle CompositeRunHandle
```

`CompositeRunHandle` should hold multiple handles and stop/close all of them.

### Validation gates

- Unit test `NoopRunHandle`.
- Unit test `CompositeRunHandle` closes all child handles.
- Unit test `ProcessRunHandle` behavior with a short-lived local process if practical.
- No public API contract changes required.

---

## Step 3 — Add a run resource registry

### Build goals

- Track active run resources by `runId`.
- Allow lifecycle code to clean up resources on completion, failure, cancellation, and server shutdown.

### Implementation guidance

Create a component such as:

```
text RunResourceRegistry
```

Responsibilities:

```
text register(runId, RunHandle) get(runId) stop(runId) remove(runId) stopAll()
```

Use a thread-safe map internally.

### Validation gates

- Register/get/remove behavior is tested.
- `stop(runId)` closes the registered handle.
- `stopAll()` closes all handles and clears the registry.
- Add server shutdown integration later if not convenient in this step.

---

## Step 4 — Change executor contract to return a handle

### Build goals

- Change the execution boundary so starting a runtime returns a `RunHandle`.
- Keep executor responsibility focused on launch/provisioning only.
- Move status transitions out of executor implementations where practical.

### Implementation guidance

Evolve the executor contract from:

```
text start(context): void
```

to something equivalent to:

```
text start(context): RunHandle
```

or, if async startup is preferred:

```
text start(context): CompletionStage
```

For the first implementation, prefer the simpler synchronous handle-returning shape unless the existing codebase already uses async execution heavily.

Add a result object only if needed:

```
text RunStartResult
runId
handle
message
metadata
```

### Validation gates

- Stub executor returns `NoopRunHandle`.
- Existing run start flow still returns `202 Accepted`.
- Resource registry receives the returned handle.
- Tests prove executor failures transition the run to `failed`.

---

## Step 5 — Add a lifecycle manager/orchestrator

### Build goals

- Introduce a central component that coordinates:
  - status transition to `starting`
  - executor start
  - handle registration
  - readiness transition to `ready`
  - monitor startup
  - cleanup on failure

### Implementation guidance

Create a component such as:

```
text RunLifecycleManager
```

Initial behavior:

```
text startRun(context): markStarting call RunExecutor.start(context) register returned RunHandle markReady
```

At this step, `ready` may mean “executor returned a handle without error.” More robust readiness probes are added in the next step.

### Validation gates

- Starting a run moves status:
  - `accepted` -> `starting` -> `ready`
- If executor throws, status becomes `failed`.
- If executor throws after allocating resources, resources are cleaned up where possible.
- Existing API behavior remains `202 Accepted` for valid run requests.

---

## Step 6 — Add readiness probes

### Build goals

- Separate “runtime launched” from “runtime ready.”
- Support runtime-specific readiness checks.

### Implementation guidance

Create an abstraction such as:

```
text RunReadinessProbe
```

With behavior similar to:

```
text awaitReady(context, handle): RunReadinessResult
```

Create initial implementations:

```
text ImmediateReadinessProbe ProcessAliveReadinessProbe
```

Suggested behavior:

- `ImmediateReadinessProbe`: immediately returns ready.
- `ProcessAliveReadinessProbe`: waits briefly, confirms process is still alive, optionally supports timeout.

Later implementations can support:

```
text Docker health check Kubernetes deployment readiness Helm release readiness Remote service job readiness Kafka heartbeat readiness Log-line readiness
```

### Validation gates

- Lifecycle manager uses readiness probe before marking `ready`.
- Readiness success transitions to `ready`.
- Readiness timeout/failure transitions to `failed`.
- Readiness failure triggers resource cleanup.

---

## Step 7 — Add ISO-21175 Kafka message model

### Build goals

- Add minimal DTO/parsing support for ISO-21175 message envelopes.
- Support message-type based lifecycle transitions without coupling to model-specific payloads.

### Implementation guidance

Create a DTO such as:

```
text Iso21175Message
```

Required fields for lifecycle monitoring:

```
text simulationRunId messageId messageType senderId receiverId eventTime nextInternalTime payload wallClockTime
```

Only `messageType` and `simulationRunId` are required for lifecycle decisions, but preserve other fields where useful.

Recognized lifecycle message types:

```
text NextInternalTimeReport ModelTerminated ErrorReport
```

### Validation gates

- Parse valid JSON messages.
- Ignore unknown/additional properties.
- Reject or ignore messages that cannot be parsed.
- Unit tests cover:
  - `NextInternalTimeReport`
  - `ModelTerminated`
  - `ErrorReport`
  - unrelated message type
  - wrong `simulationRunId`

---

## Step 8 — Add Kafka run monitor

### Build goals

- Subscribe to the run’s configured Kafka topic.
- Filter messages by `simulationRunId`.
- Update lifecycle status based on ISO-21175 messages.

### Implementation guidance

Create a component such as:

```
text KafkaIsoRunMonitor
```

Behavior:

```
text startMonitoring(context, handle): create Kafka consumer from request kafka config subscribe to topic poll loop in background for each message: parse ISO message ignore if simulationRunId != runId if messageType == NextInternalTimeReport: markRunning if current status is ready if messageType == ModelTerminated: markCompleted cleanup handle stop monitor if messageType == ErrorReport with severity error/fatal: markFailed cleanup handle stop monitor
```

Consumer group guidance:

- Use request `kafka.consumerGroup` if provided.
- Otherwise derive one from the run id, for example:

```
text model-library-run-monitor-{runId}
```

### Validation gates

- Prefer unit tests using a fake/in-memory monitor adapter first.
- If test infrastructure supports Kafka/Testcontainers, add integration tests later.
- Tests prove:
  - first `NextInternalTimeReport` transitions `ready` -> `running`
  - `ModelTerminated` transitions to `completed`
  - fatal/error `ErrorReport` transitions to `failed`
  - wrong `simulationRunId` is ignored
  - cleanup is invoked on terminal state

---

## Step 9 — Integrate monitor with lifecycle manager

### Build goals

- Automatically start Kafka monitoring after readiness succeeds.
- Ensure monitor and runtime resources are both cleaned up.

### Implementation guidance

Update lifecycle flow:

```
text startRun(context): markStarting handle = executor.start(context) register handle readinessProbe.awaitReady(context, handle) markReady monitorHandle = runMonitor.startMonitoring(context, handle) register CompositeRunHandle(handle, monitorHandle)
```

The monitor itself should also be represented as a `RunHandle` or be included in a composite cleanup handle.

### Validation gates

- Run reaches `ready` before monitor-driven `running`.
- Monitor is not started if startup/readiness fails.
- Terminal monitor events trigger cleanup.
- Server shutdown stops active monitors and runtime handles.

---

## Step 10 — Add cancellation/cleanup API or internal operation

### Build goals

- Provide an explicit way to stop a run.
- Transition stopped runs to `canceled`.
- Clean up all registered resources.

### Implementation guidance

If public API change is acceptable, add one endpoint:

```
text POST /v1/runs/{runId}/cancel
```

or:

```
text DELETE /v1/runs/{runId}
```

If public API change is not desired yet, add only an internal service method:

```
text cancelRun(runId)
```

Behavior:

```
text if run is terminal: return current status else: stop resources markCanceled
```

### Validation gates

- Canceling `starting`, `ready`, or `running` stops resources and marks `canceled`.
- Canceling `completed`, `failed`, or already `canceled` does not restart cleanup.
- Unknown run id returns existing not-found behavior.
- OpenAPI is updated if a public endpoint is added.

---

## Step 11 — Adapt concrete runtime executors

### Build goals

- Refactor concrete executors to launch resources and return handles.
- Remove lifecycle policy from model-specific executor code.
- Keep model-specific logic limited to:
  - parameter mapping
  - command construction
  - simulator/container/service startup
  - resource-specific stop behavior

### Implementation guidance

For local process runtimes:

```
text start process start stdout/stderr drain if needed return ProcessRunHandle
```

For in-process simulator runtimes:

```
text instantiate simulator call startup return SimulatorRunHandle
```

For multiple resources in one run:

```
text return CompositeRunHandle
```

Do not mark a process as `running` just because it started. The lifecycle manager should mark `ready`; Kafka monitor should mark `running`.

### Validation gates

- Concrete executor returns handles.
- Concrete executor does not directly persist `running` or `completed`.
- Process exit failure can still transition to `failed` if process supervision is implemented.
- Multiple launched resources are cleaned up together.

---

## Step 12 — Add process supervision

### Build goals

- Detect local process exit before ISO `ModelTerminated`.
- Mark failed when a process exits unexpectedly.
- Avoid double terminal transitions if Kafka already marked completed.

### Implementation guidance

For `ProcessRunHandle` or a related supervisor:

```
text watch process in background if process exits non-zero before terminal status: markFailed cleanup if process exits zero before ModelTerminated: either markCompleted or markFailed depending on runtime policy
```

Recommended default:

- Non-zero exit before `ModelTerminated` -> `failed`
- Zero exit before `ModelTerminated` -> `completed` only if this runtime is configured to treat process exit as authoritative

### Validation gates

- Non-zero process exit marks `failed`.
- Terminal status is not overwritten.
- Cleanup is idempotent.

---

## Step 13 — Add operational safeguards

### Build goals

- Prevent resources from running forever without visibility.
- Add basic timeouts and diagnostics.

### Implementation guidance

Add configurable values:

```
text startupTimeout readinessTimeout monitorPollInterval runMaxDuration cleanupTimeout
```

Add diagnostic fields internally where useful:

```
text lastMessageType lastLogicalTime lastError updatedAt runtimeType runtimeResourceId
```

Keep public API backward-compatible unless the OpenAPI spec is intentionally updated.

### Validation gates

- Startup/readiness timeout marks `failed`.
- Cleanup timeout is logged and does not hang the server.
- Diagnostics do not expose secrets.
- Existing API clients remain compatible.

---

## Step 14 — Add server shutdown cleanup

### Build goals

- Stop all active run resources when the server shuts down.
- Avoid orphaned local processes, consumers, containers, or external jobs.

### Implementation guidance

On server shutdown:

```
text runResourceRegistry.stopAll()
```

If a run is not terminal, mark it as `canceled` or `failed` based on chosen policy.

Recommended policy:

```
text server shutdown cleanup -> canceled
```

### Validation gates

- Shutdown hook invokes registry cleanup.
- Cleanup is idempotent.
- Active handles are stopped.
- No terminal statuses are overwritten.

---

## Step 15 — Testing strategy

### Required unit tests

- `RunLifecycleService`
- `RunResourceRegistry`
- `NoopRunHandle`
- `CompositeRunHandle`
- `ProcessRunHandle` where practical
- `RunLifecycleManager`
- `RunReadinessProbe`
- ISO message parsing
- Kafka monitor transition logic using fake consumer/adapter

### Required integration tests

- Valid run request returns `202`.
- Status eventually reaches `ready` with stub/immediate readiness.
- Simulated ISO `NextInternalTimeReport` moves status to `running`.
- Simulated ISO `ModelTerminated` moves status to `completed`.
- Startup failure moves status to `failed`.
- Cleanup is invoked on terminal states.

### Required contract checks

- OpenAPI status enum remains:

```
text accepted, starting, ready, running, completed, failed, canceled
```

- Public response shapes remain backward-compatible unless deliberately versioned.
- Error responses remain:

```
text code message
```

---

## Implementation order summary

Execute these in order, one or two steps at a time:

1. Normalize run status handling.
2. Introduce `RunHandle`.
3. Add `RunResourceRegistry`.
4. Change executor contract to return a handle.
5. Add `RunLifecycleManager`.
6. Add readiness probes.
7. Add ISO-21175 message DTO/parser.
8. Add Kafka run monitor.
9. Integrate monitor with lifecycle manager.
10. Add cancellation/cleanup operation.
11. Adapt concrete runtime executors.
12. Add process supervision.
13. Add timeouts and diagnostics.
14. Add server shutdown cleanup.
15. Expand unit, integration, and contract tests.

## Design rule

Executors launch resources.  
Lifecycle services own statuses.  
Monitors observe progress.  
Handles clean up resources.