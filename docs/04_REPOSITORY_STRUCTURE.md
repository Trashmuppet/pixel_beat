# Repository Structure

## Target Platform
- Android 16 (API 36)
- Kotlin
- Jetpack Compose
- Material 3

## Modules

```
app/
core/
  model/        # MBeatProject, Pattern, Track, HitEvent (schemas grow here)
  timeline/     # TimelineCompiler, CompiledTimeline, RealtimeTransport (ADR-001)
  common/       # AppDispatchers, Result<T>
  ui/           # MonoPalette, TapTarget, 12 stateless Composables (Phase 3)
  export/       # ExportPipeline, WavEncoder, GifEncoder, Mp4MediaCodecEncoder
audio-native/   # C++ DSP via Oboe/AAudio, Mixer, SPSC HitQueue, 6 procedural voices
scene-api/      # Scene / SceneRenderState contract; ScenePackRegistry (reflective)
scene-runtime/  # AnimationSystem (240 Hz fixed-step), Compose Canvas renderer
scene-warehouse/# Phase 4 reference scene (32×32 seed-derived cells)
scene-neon/     # Pro-tier scene: drum-kind pulse shapes on a lit backdrop
scene-void/     # Pro-tier scene: drum-kind hit shapes on absolute black
asset-compiler/ # Standalone Kotlin/JVM CLI → `.mbscene` packs (Phase 7)
baselineprofile/# Phase 6 macrobenchmark + BaselineProfile stub
testing/        # Placeholder for instrumented tests (Phase 6 hooks)
feature-home/         # HomeScreen + recent projects + Scene Pack grid
feature-project/      # ProjectScreen browser with recent `.mbeat` list
feature-sequencer/    # SequencerScreen + SequencerViewModel (ADR-001 playhead)
feature-arrangement/  # ArrangementScreen + ArrangementViewModel (pattern chain)
feature-export/       # ExportScreen + ExportViewModel + MediaExporter façade
premium/        # Pure-Kotlin PremiumManager interface (no Android deps)
billing/        # GooglePlayPremiumManager (Play Billing wrapper + offline cache)
storage/        # .mbeat persistence + CachingProjectRepository (Room cache)
docs/           # This directory — source-of-truth design + product specs
adr/            # Architectural decision records (ADR-001..006)
```

## Rules

- Feature modules depend on abstractions.
- Billing is isolated in `:billing`.
- Features query `PremiumManager`.
- `storage` owns `.mbeat` persistence (filesystem authoritative) and a
  **rebuildable Room cache** sourced from it per `14_STORAGE.md`.
- `scene-runtime` never owns musical timing.
- `audio-native` contains the real-time engine only — no UI / storage
  / scene dependencies leak through the JNI boundary (ADR-002).
- Room is rebuildable and never the source of truth — deleting
  `pixelbeat_cache.db` must produce a transparent cache miss with
  recovery via the filesystem walk.

## Dependency Direction

UI → Domain → Core

Never reverse dependencies.

## Module Promotions

The following modules were added in Phase ≥ 1 and are tracked here so
they don't drift again:

| Phase | Modules added |
|---|---|
| Phase 3 | `:core:ui` |
| Phase 4 | `:scene-api`, `:scene-runtime`, `:scene-warehouse` |
| Phase 5 | `:core:export` |
| Phase 6 | `:baselineprofile`, `:testing` |
| Phase 7 | `:premium`, `:billing`, `:asset-compiler`, `:scene-neon`, `:scene-void` |

## Build Target

Always compile against Android 16 (API 36).
