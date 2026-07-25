# Architecture

> Phase 6 docs pass — top-level companion to the design specs at the
> repo root. This file replaces years of stale `ARCHITECTURE.md` and
> consolidates what `04_REPOSITORY_STRUCTURE.md`, `05_TECH_STACK.md`
> and the ADRs in [`/adr/`](./adr/) say in one place.

---

## Module dependency map

```
                ┌────────────────────────┐
                │         :app           │  Compose navhost, MainActivity,
                │                        │  AppDependencies, baselineprofile
                └──────────┬─────────────┘
                           │
        ┌──────────────────┴──────────────────┐
        │                                     │
┌───────▼─────────────┐              ┌─────────▼─────────────┐
│  :feature-* (5)     │              │  :audio-native        │
│   home             │              │   Oboe / AAudio       │
│   project          │              │   C++ DSP, Mixer,     │
│   sequencer        │              │   HitQueue SPSC       │
│   arrangement      │              │   6 procedural voices │
│   export           │              └─────────┬─────────────┘
└───────┬─────────────┘                        │
        │                                       │
┌───────▼─────────────┐              ┌─────────▼─────────────┐
│  :premium          │              │  :scene-{api,        │
│  :billing          │              │       runtime,       │
│   (Google Play     │              │       warehouse}     │
│    Billing wrapper)│              └─────────┬─────────────┘
└───────┬─────────────┘                        │
        │                                       │
        └──────────────────┬──────────────────┘
                           │
        ┌──────────────────┴──────────────────┐
        │                                     │
┌───────▼─────────────┐              ┌─────────▼─────────────┐
│  :core:ui          │              │  :core:export         │
│   MonoPalette      │              │   WavEncoder /        │
│   12 stateless     │              │   GifEncoder (1-bit   │
│   composables      │              │   LZW) /              │
└───────┬─────────────┘              │   Mp4MediaCodecEncoder│
        │                            └─────────┬─────────────┘
┌───────▼─────────────┐                        │
│  :core:{model,     │  ◀─────────  (export reads .mbeat projects)
│        timeline}   │                        │
│  :core:common      │                        │
└─────────────────────┘
```

## Module responsibilities

### `:app`
The Application + the single `MainActivity` that hosts a Compose
`NavHost`. Constructs `AppDependencies(applicationContext)` once
on the activity lifecycle. Phase 6 target: replace the hand-rolled
container with a code-generated DI graph (ADR-006).

### `:feature-*` (home / project / sequencer / arrangement / export)
Each owns a screen-side ViewModel + Composable. Drives UI state
through `StateFlow<UiState>`, never owning business logic. Imports
`:core:ui` for the 1-bit palette + reusable composables, and
abstractions (`:premium`, `:core:export.MediaExporter`,
`:core:timeline.CompiledTimeline`) rather than implementation.

### `:audio-native`
Native C++ engine (Oboe / AAudio + 6 procedural drum voices +
Mixer + lock-free SPSC HitQueue). The audio callback is
allocation-free and lock-free. **No** UI, storage, or scene
dependencies leak into the native side; the Kotlin façade takes MBeatProject values and
translates to JNI.

### `:scene-api` / `:scene-runtime` / `:scene-warehouse`
The scene-system module triple. `:scene-api` exports the immutable
`SceneRenderState` + `Scene` contract. `:scene-runtime` owns the
Compose Canvas renderer (integer scaling, `FilterQuality.None`)
plus the fixed-steptime `AnimationSystem` (no wall-clock anywhere).
`:scene-warehouse` is the Phase 4 default scene implementation,
seed-derived and deterministic.

### `:premium` / `:billing`
The marker that distinguishes paying vs free users. `:premium` is the
pure-Kotlin `PremiumManager` interface; `:billing` wraps Google Play
Billing and owns the on-device entitlement cache. Project files
never query entitlement state — only features do.

### `:storage`
UTF-8 `.mbeat` filesystem persistence + atomic save/load +
`ProjectRepository` interface + list-recent walk. Room DB layer will
land as a rebuildable cache (per `14_STORAGE.md`) but is not yet
wired.

### `:core:ui`
Phase 3 introduction. Backs all `*:feature` modules with the shared
`MonoPalette` and the 12 stateless composables documented in
[`18_COMPONENT_LIBRARY-1.md`](./18_COMPONENT_LIBRARY-1.md).

### `:core:export`
Phase 5 introduction. Hosts the offline-render orchestrator
(`ExportPipeline`), the three encoders (`WavEncoder`,
`GifEncoder`, `Mp4MediaCodecEncoder`), and the
`CompatibilityExporter` adapter that satisfies the
`:feature-export` `MediaExporter` contract.

### `:core:*` foundation

- **`:core:model`** — versioned UTF-8 `.mbeat` data classes
  (`MBeatProject`, `Pattern`, `Track`, `Arrangement` + `LoopBoundary`,
  value classes `ProjectSeed`, `SwingMode`, `DrumKind`).
  Serialized via kotlinx-serialization; `@Serializable` keeps it
  syntactically round-trippable through ProGuard (ADR-006 keep rules).
- **`:core:timeline`** — `TimelineCompiler` produces deterministic
  `CompiledTimeline`. Authoritative time source per ADR-001.
- **`:core:common`** — `AppDispatchers`, `Result<…>` types. Pure
  utilities used project-wide.

### `:baselineprofile`
Phase 6 macrobenchmark host. Stub generator emits an empty
`Profile`; real rules land with the first device-farm run.

### `:asset-compiler`
Standalone Kotlin/JVM CLI tool that converts artist-authored PNG
sprites + JSON animation manifests into deterministic `.mbscene`
packs per `11_ASSET_COMPILER.md` validation rules. Independent of
the Android app layer so artists can compile packs offline.

## Architectural decisions

The freeze of architectural decisions lives in
[`/adr/`](./adr/) (ADR-001 through ADR-006). Each decision covers a
single architectural rule that would otherwise be implicit.

| ADR | Title | Owns |
|---|---|---|
| [ADR-001](./adr/001-timeline-authoritative.md) | Timeline Compilation Authoritative | UI never owns musical time |
| [ADR-002](./adr/002-native-engine-isolation.md) | Native Engine Isolation | audio-native is engine-only |
| [ADR-003](./adr/003-mvvm-unidirectional-data-flow.md) | MVVM Unidirectional Data Flow | StateFlow + Repository pattern |
| [ADR-004](./adr/004-fixed-timestep-240hz.md) | Fixed-timestep 240Hz Scene | scene never drives musical time |
| [ADR-005](./adr/005-export-renderer-is-reference.md) | Offline Renderer Is The Reference | identical input ⇒ identical output (byte for WAV/GIF) |
| [ADR-006](./adr/006-release-discipline-gates.md) | Release Discipline Gates | keystore / R8 / AAB / CI |

## Cross-cutting principles

1. **Offline first.** No `INTERNET` permission. No cloud. No
   telemetry.
2. **1-bit visual language.** Only `#000000` and `#FFFFFF`. MonoPalette
   enforces; integer scaling; `FilterQuality.None` on Canvas.
3. **UI → Domain → Core.** Features depend on abstractions. Never
   reverse.
4. **Sample-accurate timing.** Engine sample-rate is 48 kHz; scene
   tick-rate is 240 Hz. Hit events carry a `Long tick` field; the
   timeline sorts by tick then trackId.
5. **Determinism by default.** Same `.mbeat` yields the same audio
   (byte-equal WAV), the same scene frames (byte-equal GIF), and
   visually-identical MP4 (with the per-SoC caveat in ADR-005).
