# Time Semantics

## Purpose

This document defines how model time is expressed so that:

1. Models can be composed automatically.
2. Invalid combinations can be rejected before execution.
3. A coordinator can convert time values between models safely.

The core design decision is to separate:

- **Pacing** (how fast execution progresses relative to wall clock), from
- **Semantics** (what a model's logical time values mean).

---

## Conceptual Model

Each model has a local logical time axis `t_local`.
Coordination occurs on a canonical global logical axis `t_global` (seconds).

Local/global conversion:

- `t_global = originOffset + (unitSeconds * t_local)`
- `t_local = (t_global - originOffset) / unitSeconds`

Where `unitSeconds` and `originOffset` are exact rationals.

---

## Contract

## `TimeMode`

`TimeMode` describes runtime pacing:

- `real-time`
- `scaled-real-time`
- `virtual-time`

`scaled-real-time` requires `realTimeFactor > 0`.

## `TimeSemantics`

`TimeSemantics` describes logical-time meaning and conversion constraints:

- `timeDomain`: `discrete | continuous`
- `valueEncoding`: `int64 | decimal-string | float64`
- `unitSeconds`: rational seconds per 1 local time unit
- `quantum`: rational minimum local step (required for `discrete`)
- `originOffset`: rational offset from local zero to global zero
- `conversionPolicy`: `exact | approximate`
- `maxAbsErrorSeconds`: required for `approximate`
- `roundingMode`: required for `approximate` (`floor | ceiling | half-up`)
- `infinityPolicy`: `max-finite | string-sentinel`

## Invariants

- Rational denominators must be `>= 1`.
- `quantum` is mandatory when `timeDomain=discrete`.
- `maxAbsErrorSeconds > 0` and `roundingMode` are mandatory when `conversionPolicy=approximate`.
- `TimeMode.mode=scaled-real-time` requires `realTimeFactor`.

---

## OpenAPI Reflection

The OpenAPI contract now encodes these semantics and invariants in:

- `TimeModeType`, `TimeDomain`, `TimeValueEncoding`, `TimeConversionPolicy`, `TimeRoundingMode`, `TimeInfinityPolicy`, `RationalTime`, `TimeSemantics`, `TimeMode`
  - `model-library-server-java/src/main/resources/open-api/model-libary-open-api-spec.yaml:334`
- `CurrentSimulationTime.value` as encoded string plus `timeSemantics`
  - `model-library-server-java/src/main/resources/open-api/model-libary-open-api-spec.yaml:545`

Note: the published ISO 21175-2 message schema is not changed by this design. Time semantics are carried in model/run control-plane contracts (catalog/run API), not in the ISO wire schema.

---

## Java DTO Reflection

## DTO Types

- `TimeMode` (`REAL_TIME`, `SCALED_REAL_TIME`, `VIRTUAL_TIME`)
  - `model-library-server-java/src/main/java/iso/sim/server/dto/run/TimeMode.java:8`
- `TimeModeDto` (`mode`, `timeSemantics`, optional `realTimeFactor` with validation)
  - `model-library-server-java/src/main/java/iso/sim/server/dto/run/TimeModeDto.java:9`
- `TimeSemanticsDto` (all semantic fields + constructor invariants)
  - `model-library-server-java/src/main/java/iso/sim/server/dto/run/TimeSemanticsDto.java:9`
- `RationalTimeDto`
  - `model-library-server-java/src/main/java/iso/sim/server/dto/run/RationalTimeDto.java:8`
- Enums: `TimeDomain`, `TimeValueEncoding`, `TimeConversionPolicy`, `TimeRoundingMode`, `TimeInfinityPolicy`
  - `model-library-server-java/src/main/java/iso/sim/server/dto/run/TimeDomain.java:8`
  - `model-library-server-java/src/main/java/iso/sim/server/dto/run/TimeValueEncoding.java:8`
  - `model-library-server-java/src/main/java/iso/sim/server/dto/run/TimeConversionPolicy.java:8`
  - `model-library-server-java/src/main/java/iso/sim/server/dto/run/TimeRoundingMode.java:8`
  - `model-library-server-java/src/main/java/iso/sim/server/dto/run/TimeInfinityPolicy.java:8`

## Current Simulation Time DTO

Run-status time now uses:

- `value: String` (encoded logical time)
- `timeSemantics: TimeSemanticsDto`

In:

- `model-library-server-java/src/main/java/iso/sim/server/dto/run/CurrentSimulationTimeDto.java:9`

---

## Model Server Capabilities (Current)

The model server currently supports:

1. **Accepting and serving semantic declarations** through model metadata and run requests (`timeMode.timeSemantics`).
2. **Schema-level and DTO-level constraints** for required time invariants.
3. **Precision-safe logical-time monitoring**:
   - Parses runtime times as `BigDecimal`,
   - Rejects time rollback,
   - Stores encoded logical time string plus semantics in run status.
   - `model-library-server-java/src/main/java/iso/sim/server/service/KafkaIsoRunMonitor.java:167`
4. **UI support** for configuring and displaying semantics fields.
   - `model-library-ui/src/app/models/run-config-dialog/run-config-dialog.component.ts:65`
   - `model-library-ui/src/app/services/model.service.ts:30`

Current limitation:

- The server does **not** yet perform full cross-model compatibility checking or scheduling as a federation coordinator. It carries the semantics and exposes enough metadata for a coordinator to do so.

---

## Coordinator Conversion and Coordination Strategy

This section defines how a coordinator can execute compliant models together even when modes/domains differ.

## 1) Build canonical mapping

For each model `M`:

- Read `unitSeconds`, `originOffset`, `timeDomain`, `quantum`, `conversionPolicy`, `roundingMode`, `maxAbsErrorSeconds`.
- Construct exact local/global transforms:
  - `toGlobal(M, t_local)`
  - `toLocal(M, t_global)`

Use rational arithmetic (or decimal with explicit error accounting), not binary floating-point.

## 2) Validate compatibility at init

Before execution:

- Validate all per-model invariants.
- Verify conversion feasibility for each intended interaction path:
  - If target model requires `exact`, converted local times must be exactly representable.
  - If target allows `approximate`, rounding must use declared `roundingMode` and absolute error must be `<= maxAbsErrorSeconds`.
- Reject federation startup when any required path fails.

## 3) Coordinate event times

During execution:

1. Convert each model’s candidate next time to `t_global`.
2. Select minimum eligible `t_global`.
3. For each model receiving an event at that global time, convert `t_global -> t_local`.
4. Apply domain policy:
   - `continuous`: direct converted value.
   - `discrete`: quantize to `quantum` using policy rules.
5. Enforce exact/approximate guarantees; fail fast on violation.

## 4) Handle mismatched domains

Example:

- Model A: discrete ticks of `1 ms`
- Model B: continuous seconds

`A -> B`:

- Exact mapping always possible (tick to real value).

`B -> A`:

- Exact only when converted time lands on A’s tick lattice.
- Otherwise:
  - reject if A requires `exact`,
  - quantize and bound error if A allows `approximate`.

## 5) Handle mismatched pacing modes

Pacing mode does not change logical-time meaning:

- `virtual-time`: no wall-clock throttling
- `real-time`: factor = 1
- `scaled-real-time`: factor = declared value

Coordinator should:

- Keep one logical-time ordering on `t_global`.
- Apply per-model wall-clock gating only at dispatch time.
- Prevent causality violations by not committing global time beyond what constrained (real/scaled) participants can legally process.

---

## Recommended ISO 21175-2 Message Schema Changes

These are recommended future changes to the published ISO message schema to better support safe automated coordination.

## 1) Add optional `timeMode` to common message envelope

Add an optional field in the common envelope:

- `timeMode` (same semantic shape as OpenAPI control-plane contract)

Rationale:

- Allows participants to publish the exact logical-time semantics used for the current run on-wire.
- Removes ambiguity when control-plane and data-plane are implemented by different systems.

Handshake rule for this profile:

- `SimulationInit` may omit `timeMode`.
- `SimulationInit` may optionally include `timeConversionPolicy` (`COMMON_TIME | TIME_CONVERTING`) as run-level coordinator intent.
- In common-time mode, messages may omit conversion metadata.
- In conversion-time mode, coordinator policy can require `timeMode` from subordinate models (typically on `NextInternalTimeReport`) before time conversion-dependent scheduling.
- Coordinator runs conversion checks before using conversion-time dispatch behavior.

## 2) Broaden logical time encoding beyond JSON number

Current logical time fields (`eventTime`, `nextInternalTime`) should support:

- number (existing)
- string (new, for exact integer/decimal encoding)

Rationale:

- Prevents precision loss for large integers and exact decimal schedules.
- Preserves interoperability where exact conversion is required.

## 3) Add explicit infinity representation metadata

Add optional metadata for infinity handling:

- `infinityPolicy` (`max-finite | string-sentinel`)
- optional sentinel string when `string-sentinel` is used

Rationale:

- Avoids implementation-specific assumptions around DEVS infinity semantics.

## 4) Add conversion policy metadata

Include optional conversion constraints:

- `conversionPolicy` (`exact | approximate`)
- `maxAbsErrorSeconds`
- `roundingMode`

Rationale:

- Lets a coordinator enforce model-declared constraints directly from wire contract.

## 5) Standardized compatibility diagnostics (optional message/profile)

Define a message or payload profile for compatibility reporting during init:

- incompatible field pairs
- violated constraints
- required vs actual conversion behavior

Rationale:

- Enables deterministic, machine-readable startup failure handling.

## 6) Backward-compatibility approach for standard evolution

Recommended standard evolution path:

1. Add all fields as optional.
2. Keep existing numeric logical-time semantics valid.
3. Publish conformance profiles:
   - baseline profile (legacy behavior)
   - enhanced-time profile (new semantics required)

This allows incremental adoption while preserving existing implementations.

---

## Coordinator Conformance Checklist

Use this checklist to validate a coordinator implementation against this time-semantics model.

## A. Input Contract Validation

- [ ] Reject missing `timeMode` or `timeSemantics` in model/run contract.
- [ ] Reject invalid rationals (`denominator < 1`).
- [ ] Enforce `quantum` when `timeDomain=discrete`.
- [ ] Enforce `realTimeFactor` when `mode=scaled-real-time`.
- [ ] Enforce `maxAbsErrorSeconds` and `roundingMode` when `conversionPolicy=approximate`.

## B. Arithmetic and Representation

- [ ] Use exact rational/decimal math for conversion (no implicit binary float conversion path).
- [ ] Preserve encoded time values without silent precision loss.
- [ ] Support global/local transforms using `unitSeconds` and `originOffset`.
- [ ] Implement deterministic quantization for discrete domains.

## C. Compatibility Gate at Initialization

- [ ] Allow `SimulationInit` without `timeMode`.
- [ ] In conversion-time mode, require model `timeMode` before conversion-dependent dispatch.
- [ ] Perform pairwise or pathwise conversion feasibility checks for all coupled interactions.
- [ ] Enforce `exact` policy strictly.
- [ ] Enforce `approximate` policy with declared `roundingMode` and error bound.
- [ ] Produce machine-readable rejection diagnostics on incompatibility.
- [ ] Block run start on any violation.

## D. Runtime Scheduling and Dispatch

- [ ] Convert all candidate event times to canonical global time before ordering.
- [ ] Select next event by canonical global-time ordering.
- [ ] Convert dispatch time back to each target model local time with policy enforcement.
- [ ] Prevent rollback in observed logical time per run/model stream.
- [ ] Preserve causality across mixed pacing modes.

## E. Observability and Auditability

- [ ] Emit current logical time with associated semantics in status/monitoring.
- [ ] Log each approximate conversion with applied rounding and error.
- [ ] Log and surface policy violations as explicit errors.
- [ ] Version and expose the active time-semantics profile used by the coordinator.

## F. Interoperability Readiness Tests

- [ ] Discrete↔continuous interoperability tests (exact and approximate scenarios).
- [ ] Mixed encoding tests (`int64`, `decimal-string`, `float64`).
- [ ] Large-value precision tests beyond IEEE-754 exact integer range.
- [ ] Infinity-handling tests for selected policy.
- [ ] Repeatability tests showing deterministic scheduling under identical inputs.

---

## Operational Guidance

1. Treat `TimeSemantics` as authoritative for interoperability.
2. Treat `valueEncoding` as transport/representation detail, not semantic meaning.
3. Prefer `exact` conversion for safety-critical paths.
4. Use `approximate` only with explicit, domain-approved error budgets.
5. Keep ISO wire schema stable; evolve semantics in control-plane contracts and coordinator policy.
