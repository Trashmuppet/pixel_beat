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

---

## Module Tree

```
app/                            Compose entry point — MainActivity + nav
core/
  model/                        MBeatProject + Track + HitEvent (pure Kotlin)
  timeline/                     TimelineCompiler (pure Kotlin, deterministic)
  common/                       AppDispatchers + Result (pure Kotlin)
audio-native/                   Oboe/AAudio engine facade + CMake / JNI stub
scene-api/                      Scene + SceneRenderState contract (pure Kotlin)
scene-runtime/                  Compose Canvas 1-bit renderer
scene-warehouse/                Default Warehouse scene
feature-home/                   Home destination
feature-project/                Project browser destination
feature-sequencer/              Pattern editor destination
feature-arrangement/            Song arranger destination
feature-export/                 Format selector + export progress
premium/                        PremiumManager abstraction (pure Kotlin)
billing/                        Play Billing wrapper + entitlement cache
storage/                        .mbeat persistence + sample asset
asset-compiler/                 Standalone CLI for compiling .mbscene packs
```

Architectural rules: **UI → Domain → Core**. Features depend on
abstractions (`PremiumManager`, `scene-api`). `audio-native` is the
real-time engine only. `storage` owns the `.mbeat` source of truth.

---

## Build

Versions pinned in [`gradle/libs.versions.toml`](./gradle/libs.versions.toml):

- Android Gradle Plugin **8.7.3**
- Kotlin **2.0.21**
- Gradle **8.10.2** (wrapper)
- Compose BOM **2024.11.00**

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
./gradlew :app:test                   # All JVM + instrumented tests
./gradlew :core:timeline:test         # Just the timeline compiler tests
./gradlew :asset-compiler:run         # Build and run the asset compiler
```

### Outputs

- Debug APK: `app/build/outputs/apk/debug/`
- AAB (release): `app/build/outputs/bundle/release/` (Phase 6)

---

## Sample data

`storage/src/main/assets/sample.mbeat` is the bundled Pixel Beat
fixture (Warehouse Groove, 120 bpm). The app reaches it through
`storage/ProjectStore` via `ProjectStore.load("sample", assets)`.

---

## Where the original design docs live

The `00_*` through `18_*` markdown specs in the repo root are the
authoritative product/architecture source of truth. This scaffolding
honours every frozen rule in them; new code lands behind
[architectural decision records](./adr/) so changes to those rules
stay explicit.
