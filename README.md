# Monochrome Beat

> An offline-first Android drum-machine that turns beats into
> synchronised monochrome pixel-art music videos. *Create a beat.
> Watch it come alive. Export it. Own it.*
>
> See [`00_PRODUCT_VISION.md`](./00_PRODUCT_VISION.md) and the rest of
> the design specs at the repository root.

---

## Stack

| Layer | Choice |
|---|---|
| Platform | Android 16 (API 36), Android 7 (API 24) minimum |
| Language | Kotlin (JVM target 17) |
| UI | Jetpack Compose + Material 3 |
| Audio | Native C++ via Oboe / AAudio (48 kHz internal rate) |
| Storage | UTF-8 `.mbeat` JSON via kotlinx-serialization; Room as a cache |
| Rendering | Compose Canvas + fixed 240 Hz simulation, 1-bit colour |
| Export | MediaCodec / MediaMuxer / WAV encoder (`12_EXPORT_PIPELINE.md`) |
| Billing | Google Play Billing — one-time £1.99 Pro unlock |

No cloud, no API keys, no telemetry, no advertising, no accounts.

See [`docs/13_PERFORMANCE_AND_TESTING.md`](./13_PERFORMANCE_AND_TESTING.md)
for performance budgets and golden-fixture testing rules.

---

## Module Tree

```
app/                            Compose entry point — MainActivity + nav + DI
core/
  model/                        MBeatProject + Track + HitEvent (pure Kotlin)
  timeline/                     TimelineCompiler + CompiledTimeline (pure Kotlin)
  common/                       AppDispatchers + Result (pure Kotlin)
  ui/                           Phase 3: MonoPalette + 12 stateless composables
  export/                       Phase 5: ExportPipeline + WavEncoder + GifEncoder +
                                Mp4MediaCodecEncoder + CompatibilityExporter
audio-native/                   Oboe/AAudio engine + CMake / JNI + NDK
scene-api/                      Scene + SceneRenderState contract (pure Kotlin)
scene-runtime/                  AnimationSystem (240Hz, no wall-clock) + SceneRenderer
scene-warehouse/                Default Warehouse scene (Phase 4 deterministic)
feature-home/                   Home destination
feature-project/                Project browser destination
feature-sequencer/              Pattern editor + Sequencer ViewModel
feature-arrangement/            Song arranger + Arrangement ViewModel
feature-export/                 Export screen + Compat-MediaExporter adapter
premium/                        PremiumManager abstraction (pure Kotlin)
billing/                        Play Billing wrapper + EntitlementCache
storage/                        .mbeat persistence + ProjectRepository
asset-compiler/                 Standalone CLI for compiling .mbscene packs
baselineprofile/                Phase 6 scaffold (empty Profile generator)
```

Architectural rules: **UI → Domain → Core**. Features depend on
abstractions (`PremiumManager`, `MediaExporter`, `scene-api`).
`audio-native` is the real-time engine only. `storage` owns the
`.mbeat` source of truth.

See [`ARCHITECTURE.md`](./ARCHITECTURE.md) for the full ASCII
module diagram and per-module responsibilities.

---

## ADR Index

Each Architectural Decision Record documents one rule that's
otherwise implicit. Locked today:

- [ADR-001](./adr/001-timeline-authoritative.md) — Timeline Compilation Authoritative
- [ADR-002](./adr/002-native-engine-isolation.md) — Native Engine Isolation
- [ADR-003](./adr/003-mvvm-unidirectional-data-flow.md) — MVVM Unidirectional Data Flow
- [ADR-004](./adr/004-fixed-timestep-240hz.md) — Fixed-timestep 240Hz Scene Simulation
- [ADR-005](./adr/005-export-renderer-is-reference.md) — Offline Renderer Is The Reference
- [ADR-006](./adr/006-release-discipline-gates.md) — Release Discipline Gates
- Template: [`adr/000-template.md`](./adr/000-template.md)

---

## Build

Versions pinned in [`gradle/libs.versions.toml`](./gradle/libs.versions.toml):

- Android Gradle Plugin **8.7.3**
- Kotlin **2.0.21**
- Gradle **8.10.2** (wrapper)
- Compose BOM **2024.11.00**
- compileSdk / targetSdk = 36, minSdk = 24

### First-time setup

1. Copy `local.properties.example` to `local.properties` and set
   `sdk.dir` to your Android SDK location.
2. Either open the project in **Android Studio** (it will offer to set
   up the standard `gradle-wrapper.jar`) **or** install Gradle 8.10.2
   (`brew install gradle`, `sdkmanager --install "gradle;8.10.2"`, …)
   and run:
   ```bash
   ./gradlew wrapper --gradle-version 8.10.2
   ```

### Common tasks

```bash
./gradlew :app:assembleDebug         # Build a debug APK
./gradlew :app:assembleRelease      # Build a release AAB (requires keystore)
./gradlew :core:timeline:test       # Golden-fixture TimelineCompiler tests
./gradlew :scene-runtime:test       # 240Hz AnimationSystem tests
./gradlew :core:export:test         # WAV/GIF/MP4 determinism tests
./gradlew :asset-compiler:run       # CLI: build a .mbscene pack
./gradlew :baselineprofile:generateBaselineProfile  # Produce a Profile for Play
```

### Outputs

- Debug APK: `app/build/outputs/apk/debug/`
- AAB (release, split per ABI/language/density): `app/build/outputs/bundle/release/`
- Compose Compiler Reports: `app/build/compose_compiler/` (release only)

---

## Release Discipline

See [`docs/06_NAVIGATION_AND_STATE.md`](./06_NAVIGATION_AND_STATE.md)
for the navigation + back-button contract; this section covers
shipping.

1. **Copy** `app/keystore.properties.example` to
   `app/keystore.properties` and fill in the `storePassword`,
   `keyPassword`. The file itself stays gitignored — see
   [`.gitignore`](./.gitignore).
2. **Place** `release.keystore` next to `app/`.
3. **Run** `./gradlew :app:assembleRelease`. R8 + AAB splits fire
   automatically; the upload-key signature is taken from
   `keystore.properties`.
4. **Verify** golden tests passed before publishing. `git push`
   triggers CI which runs the full Phase-6 test surface.

Compose Compiler Reports land in `app/build/compose_compiler/`
for review on every `release` build — check for unstable classes
and missing `@Stable` annotations.

---

## Sample data

`storage/src/main/assets/sample.mbeat` is the bundled Pixel Beat
fixture (Warehouse Groove, 120 bpm). The app reaches it through
`storage/ProjectStore` via `ProjectStore.load("sample", assets)`.

---

## Phase status

Mapping to [`02_ROADMAP.md`](./02_ROADMAP.md):

| Phase | Title | State |
|---|---|---|
| 0 | Repository / Architecture / CI / `.mbeat` / Navigation / Premium abstraction | **Closed** — see [README Module Tree](#module-tree) above |
| 1 | Timeline Compiler / Transport / Scheduling / Deterministic tests | **Closed** — `:core:timeline` + `TimelineCompilerTest` |
| 2 | Native drum synthesis / Mixer / Voices | **Closed** — `:audio-native` (Phase 2) |
| 3 | Pattern editor / Song arranger (MVVM) | **Closed** — `:core:ui` + 3 ViewModels + 5 screens |
| 4 | Scene runtime / Warehouse / 240 Hz simulation | **Closed** — `AnimationSystem` + `WarehouseScene` + `SceneRuntimeTest` |
| 5 | Offline export / MP4 / GIF / WAV | **Closed** — `:core:export` + `ExportDeterminismTest` |
| 6 | Performance / Accessibility / Google Play release | **Closed** — `:baselineprofile` scaffold + R8 + AAB + ADRs |

The first six phases close the V1 contract. V1+ follow-ups
(Play 2FA, per-track signing, build versions of x264 for
byte-identical MP4) are tracked in `/adr/` as their work begins.

---

## Where the original design docs live

The `00_*` through `18_*` markdown specs at the repository root are
the authoritative product/architecture source of truth. This
scaffolding honours every frozen rule in them; new code lands
behind [architectural decision records](./adr/) so changes to
those rules stay explicit.
