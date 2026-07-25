# ADR-004: Fixed-timestep 240Hz Scene Simulation

| Field | Value |
|---|---|
| **Date** | 2026-07-25 |
| **Status** | Accepted |
| **Deciders** | Engineering, Scene + Animation team |
| **Consulted** | Architecture |

## Context

[`09_SCENE_SYSTEM.md`](../09_SCENE_SYSTEM.md) requires the scene
to transform drum events into deterministic pixel-art animation.
Two failure modes threatened determinism:
- `Scene.step()` reading `System.currentTimeMillis()` to animate
  the kick bounce — different runs produce different bounce
  phases.
- Manual "tick every vsync" — Compose recomposition timing
  varies between devices.

## Decision

* The `AnimationSystem` advances only when the caller invokes
  `advance(audioFrames, audioSampleRate)`. The "now" is **derived
  from audio frame counts**, never from `System.currentTimeMillis()`
  or `nanoTime()`.
* Scene ticks per second are pinned at `SCENE_TICK_HZ = 240`
  ([`scene-runtime/AnimationSystem.kt`](../scene-runtime/src/main/java/com/trashmuppet/pixelbeat/scene/runtime/AnimationSystem.kt)).
* `Scene.step()` MUST be pure given its current `queuedHits`,
  `project` state, and the `currentTick` propagated by the
  `AnimationSystem`. No I/O, no allocation, no time.
* Compose-side UI redraw at 60 FPS reads the *latest* `state`
  through `collectAsStateWithLifecycle()` — the UI never drives
  the simulation.

## Consequences

* Positive: golden-fixture determinism. `SceneRuntimeTest` proves
  byte-equal pixels for the same input on every run.
* Positive: re-ordering offline render is a no-op — playback and
  export consume identical buffers.
* Negative: there's no "live preview while paused" because the
  pawn is "audio time" — when playback stops, the scene ticks
  stop too. We accept this because the alternative is wall-clock
  leaks that violate ADR-001.
* Out of scope: variable-rate scene simulation. AnimationSystem is
  hard-locked to 240 Hz for V1; creative scenes that want 120 Hz
  integer-scaling don't exist yet.
