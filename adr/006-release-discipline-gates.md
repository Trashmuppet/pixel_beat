# ADR-006: Release Discipline Gates

| Field | Value |
|---|---|
| **Date** | 2026-07-25 |
| **Status** | Accepted |
| **Deciders** | Engineering, Release |
| **Consulted** | Security, CI |

## Context

[`03_BUILD_SEQUENCE.md`](../03_BUILD_SEQUENCE.md) Phase 6 demands
"performance budgets met, tests passing, documentation updated,
ADR created for architectural changes." We've shipped projects
where the CI surface drifted — a missing R8 rule for kotlinx.serialization
silently stripped `@Serializable` companions in `release` and
the Play upload rejected the upload-key AAB.

## Decision

The V1 release process locks in the following gates:

* **Keystore:** `keystore.properties` is **gitignored**; only
  `keystore.properties.example` ships in the repo. Real secret
  values live in CI secret stores. The build script reads
  `keystore.properties` if it exists; otherwise the release
  build falls through to debug signing (acceptable for
  early-stage iteration).
* **R8:** enabled on `release`. `app/proguard-rules.pro` keeps
  the AudioEngine JNI bridge, kotlinx.serialization runtime,
  and MediaCodec-friendly reflectively-loaded classes intact.
  Compose Compiler Reports enabled on `release` only.
* **AAB splits:** `bundle { abi{…} language{…} density{…} }` so
  Play delivers lean per-device artifacts.
* **Test gates:** `TimelineCompiler`, `SceneRuntime`, and
  `ExportDeterminism` test suites must pass before any release
  build is signed. CI runs `./gradlew :app:assembleDebug
  :core:timeline:test :scene-runtime:test :core:export:test`
  on every Pull Request.
* **BaselineProfile:** the `:baselineprofile` module ships a
  stub Macrobenchmark generator; real rules land with the first
  device-farm run. Composer Compiler Reports are reviewed for
  unstable class candidates before each minor release.

## Consequences

* Positive: Play upload rejects on the FIRST failed check, not on
  a manual QA pass.
* Positive: `keystore.properties.example` is the single
  onboarding artifact for whoever sets up signing — no docs
  detours.
* Negative: slow instrumentation tests can stall CI. Phase 6+
  follow-up runs `connectedAndroidTest` only on a tagged nightly
  build, not every commit.
* Out of scope: bundle signing split per ABI. Phase 6 ships the
  single upload key per Play track; per-track signing lands with
  the Play Store rollout manager.
