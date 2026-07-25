# ADR-002: Native Engine Isolation

| Field | Value |
|---|---|
| **Date** | 2026-07-25 |
| **Status** | Accepted |
| **Deciders** | Engineering, Native team |
| **Consulted** | Audio DSP owner |

## Context

[`08_AUDIO_ENGINE.md`](../docs/08_AUDIO_ENGINE.md) requires the audio
callback to be allocation-free and lock-free. The native module
sits at the bottom of the architecture but historically leaked UI /
storage dependencies into the JNI thread through shared globals and
lazy singletons — we lost the determinism guarantee that
"identical input ⇒ identical output" depends on.

## Decision

The `:audio-native` module imports **only** from `:core:model` and
`:core:common`. **Never** from `:scene-*`, `:storage`, `:feature-*`,
or Android `Context`. All parameters crossing the JNI boundary are
passed as primitives (`jlong`, `jfloat`, `jint`) and pre-allocated
arrays.

Windows of opportunity for broken isolation:
* `nativeSchedule` taking a `SceneRenderState` parameter — would
  pull Compose into the audio callback. We forbid this; the
  scheduler accepts only `(voiceIndex, tick, velocity)`.
* `renderOffline` writing to a `java.io.File` — would deadlock if
  the export thread holds a write lock. We pass `jfloatArray` and
  let the caller commit to disk on its own thread.

## Consequences

* Positive: a future port to a real-time embedded target (wear,
  Auto, TV) only needs to swap the JNI surface — UI never leaks
  in.
* Positive: rule enforced by R8 keep rules in
  [`app/proguard-rules.pro`](../app/proguard-rules.pro) means we
  can't accidentally `import` a banned symbol at link time.
* Negative: the production `AudioFramesSource` lives in `:app`
  because it depends on `AudioEngine` + `TimelineCompiler`. This
  is fine — `:core:export` only depends on `audio-native`-**shaped**
  primitives, not on `:audio-native` itself.
