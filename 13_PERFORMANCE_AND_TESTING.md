# Performance and Testing

> Phase 6 docs pass — successor to
> [`12_EXPORT_PIPELINE.md`](./12_EXPORT_PIPELINE.md) and predecessor
> to [`14_STORAGE.md`](./14_STORAGE.md). Locks down the numerical
> contracts that the engineering org budgets the rest of the project
> against.

---

## Performance Budgets

Numbers below are the engineering targets. Anything that creeps past
the budget is an ADR-level decision (raise one, don't tune silently).

| Surface | Budget |
|---|---|
| `TimelineCompiler.compile` (64-bar, 16-track project) | **< 50 ms** cold |
| `WarehouseScene.load` (any project) | **< 5 ms** cold |
| `AnimationSystem.advance` (1 second of audio) | **< 100 ms** wall |
| Cold app launch to Home interactive | **< 1 s** |
| Export 1 minute of 720p MP4 | **< 20 s** wall (SoC-dependent) |
| Export 1 minute of 1-bit GIF | **< 8 s** wall |
| `JankStats` drop threshold (any screen) | **< 5%** missed frames |

The export budgets vary dramatically across hardware because the
H.264 bitstream produced by hardware encoders is SoC-specific
(ADR-005). We don't promise byte-identical MP4 across SoCs — only
visually identical. The wall-clock budget is the contract.

## Tools

* **`Macrobenchmark`** (Phase 6+): cold-start + frame-time benchmarks
  via `androidx.benchmark:benchmark-macro-junit4`. Initial benchmark
  adds cold-launch and a frame-time trace over the Sequencer screen.
* **`BaselineProfile`** (`baselineprofile/` module): hands the
  macrobenchmark rules to `ProfileInstaller`. Phase 6 ships a stub —
  real rules land once the team collects a representative workload.
* **`JankStats`**: Compose-side frame-time HUD added in DEBUG profiles
  only so engineers can spot regressions as they navigate. No HUD in
  RELEASE.
* **Compose Compiler Reports**: enabled on `:app`'s `release` build
  (`enableComposeCompilerReports` in `app/build.gradle.kts`).
  Output goes to `app/build/compose_compiler/` for review.

## Golden Tests (determinism)

Per `12_EXPORT_PIPELINE.md` "identical input → identical output" is
the contract — phased as **byte-identical** vs **visually-identical**:

| Format | Determinism class | Test asserts |
|---|---|---|
| WAV | byte-identical | SHA-256 of output equal across runs |
| GIF | byte-identical | file bytes equal across runs |
| MP4 | visually-identical | frame count + per-frame grayscale MD5 equal across runs |
| `.mbeat` (JSON via kotlinx.serialization) | byte-identical | SHA-256 of `MBeatProject.serializer().encode(…)` equal across runs |
| `SceneRenderState` | byte-identical | `AnimationSystem.advance()` over a fixed hit stream produces identical `pixels: ByteArray` across runs |

The MP4 visual-only rule is recorded in **ADR-005**. Hardware H.264
encoders on different SoCs produce bit-identical PES-compliant
streams only when config is fully pinned (Surface format, GOP
structure, encoder-specific rate-control). Until Phase 6+ tuning
locks these down, we accept per-SoC variance and verify the visible
output via YUV sum + SHA-256.

**Golden fixture sources:**
* `core/export/src/test/resources/golden/<format>-<fixture>-sha256.txt` —
  promoted once the team runs `:core:export:test` on a real device.

## Test harnesses

* `:core:timeline` + `:scene-runtime` + `:core:export` use **JUnit 5
  Platform** + `kotlin-test` for JVM unit tests. `tasks.withType<Test> {
  useJUnitPlatform() }` is set per module.
* These tests use **HashMap-backed fixtures** so a developer can read
  them on a phone screen — explicit `MBeatProject` literals over
  foreign-keyed SQL.
* `testing/` placeholder module (Phase 0 docs, not yet wired to
  instrumented tests).

## Continuous integration

Phase 6+ ties the assembly to GitHub Actions:
* `./gradlew :app:assembleDebug :core:timeline:test :scene-runtime:test :core:export:test`
* `./gradlew lintDebug test` (lint + JVM tests in one go)
* `./gradlew :baselineprofile:connectedAndroidTest` — runs once a
  device farm is in CI.

## What we DO NOT do

* No flaky tests. If a test is flaky, we either fix the underlying
  race or quarantine it with a TODO pointing to the fix. ADR pending?
  Then no QA-grade CI gate.
* No snapshot tests of Composable previews. They're expensive and
  brittle — accessibility / role checks are preferred.
