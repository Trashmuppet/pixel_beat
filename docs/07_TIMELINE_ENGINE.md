# Timeline Engine

## Purpose
The Timeline Engine is the authoritative source of musical events. It compiles a `.mbeat` project into an immutable event stream used by both real-time playback and offline export.

## Principles
- Export-first.
- Deterministic.
- Audio owns musical time.
- Renderer never schedules events.

## Pipeline
Project → TimelineCompiler → Ordered HitEvents → RealtimeTransport / OfflineRenderSession

## Responsibilities
- BPM and swing.
- Pattern chaining.
- Loop boundaries.
- Arrangement transitions.
- Stable event ordering.
- Deterministic project seed.

## Rules
- Sample-accurate scheduling.
- No wall-clock timing.
- No UI timers.
- Tested against golden fixtures.
- Target Android 16 (API 36).
