# ADR-001: Timeline Compilation Authoritative

| Field | Value |
|---|---|
| **Date** | 2026-07-25 |
| **Status** | Accepted |
| **Deciders** | Engineering |
| **Consulted** | Audio team, real-time transport owner |

## Context

[`07_TIMELINE_ENGINE.md`](../docs/07_TIMELINE_ENGINE.md) defines the
TimelineCompiler as the authoritative source of musical events.
The product rule "audio owns musical time" creates a temptation
to let UI components (Playhead, Arrangement transitions) compute
their own time. We've seen three product bugs in similar projects
where a 5 ms tick drift on the playhead caused export-audio to
mismatch live playback by exactly one tick. We want a single
source of tick position.

## Decision

The `TimelineCompiler` is the **only** module that produces a
`CompiledTimeline.events` list. Both `RealtimeTransport` (live
playback) and `OfflineRenderSession` (export) consume the same
output. UI components read the current playhead position through
`RealtimeTransport.positionFlow` — they MUST NOT maintain their own
playhead counter.

## Consequences

* Positive: identical audio between preview and export by
  construction. No more "preview sounded right, export was late."
* Positive: timeline hashing (`ProjectHasher.contentHashOf`) gives
  us a deterministic golden-fixture cross-check for tests.
* Negative: every screen that wants a playhead must subscribe via
  Flow rather than reading a function call. We compensate with
  `collectAsStateWithLifecycle()` from Compose.
* Out of scope: real-time UI animations that animate the playhead
  visually still use `withFrameNanos` for the *visual smoothing*
  — only the *tick position* is owned by the transport.
