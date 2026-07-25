# Pixel Beat Repository Audit Roadmap
## Release Quality Implementation Roadmap

**Status:** Post-Audit Implementation Plan

The repository architecture is strong and should remain frozen. The focus now shifts from designing the application to implementing it while preserving the documented architecture.

---

# Phase 1 — Repository Clean-up (Highest Priority)

## Objective

Bring the repository to production quality before feature development.

### Tasks

- Remove duplicate module declarations from `settings.gradle.kts`.
- Standardise documentation filenames.
- Move documentation into a consistent `/docs` structure.
- Verify Gradle Version Catalog usage.
- Verify Android 16 (API 36) target across every module.
- Verify Java/Kotlin toolchain versions.
- Confirm baseline profile module configuration.
- Confirm release signing placeholders.

### Deliverables

- Clean repository structure
- Zero duplicate Gradle configuration
- Consistent documentation hierarchy

---

# Phase 2 — Core Domain

## Objective

Build the application's immutable business model.

### Implement

core:model

- Project
- Track
- Pattern
- Step
- Arrangement
- HitEvent
- Tempo
- Swing
- ProjectSeed
- ExportSettings

### Deliverables

Complete immutable domain model.

---

# Phase 3 — Project Format

## Objective

Implement the portable project system.

### Implement

storage/

- .mbeat serializer
- Versioned schema
- Migration framework
- Atomic save
- Atomic load
- Backup strategy

Room

- Recent projects
- Search index
- Metadata cache

Room must never become the source of truth.

### Deliverables

Complete document persistence.

---

# Phase 4 — Timeline Compiler

## Objective

Create the deterministic musical timeline.

### Implement

core:timeline

- TimelineCompiler
- TimelineEvent
- Pattern compiler
- Swing processor
- Arrangement compiler
- Loop processor
- Tempo map

### Requirements

- Sample accurate
- Deterministic
- Export-first
- Golden fixture testing

---

# Phase 5 — Native Audio Engine

## Objective

Implement professional low-latency playback.

### Implement

audio-native/

- Oboe
- AAudio
- Native callback
- Drum voices
- Mixer
- Limiter
- Master output

### Requirements

- Allocation free callback
- Lock free callback
- Zero UI interaction
- Zero storage interaction

---

# Phase 6 — Scene Runtime

## Objective

Create deterministic visual simulation.

### Implement

scene-runtime/

- AnimationSystem
- SceneGraph
- Entity system
- Particle system
- Event dispatcher
- Fixed 240 Hz simulation

Renderer must never own timing.

---

# Phase 7 — Renderer

## Objective

Render deterministic pixel graphics.

### Implement

- Compose renderer
- Integer scaling
- Camera
- Sprite batching
- Dirty region rendering
- 1-bit validation

### Rules

Only

- Black
- White

Never

- Greys
- Transparency
- Anti-aliasing
- Gradients

---

# Phase 8 — Sequencer UI

## Objective

Build the complete music creation interface.

### Implement

feature-sequencer

- 6 × 16 grid
- Playhead
- Tempo
- Swing
- Pattern switching
- Track mute
- Solo
- Copy
- Paste
- Clear

---

# Phase 9 — Song Arrangement

## Objective

Implement complete song construction.

### Features

- Sections
- Looping
- Pattern chaining
- Duplicate
- Insert
- Delete
- Reorder

---

# Phase 10 — Export System

## Objective

Implement deterministic export.

### Formats

- WAV
- MP4
- GIF

Pipeline

.mbeat

↓

Timeline Compiler

↓

Offline Audio Renderer

↓

240 Hz Simulation

↓

Frame Renderer

↓

MediaCodec

↓

MediaMuxer

### Requirements

Entirely offline.

---

# Phase 11 — Premium System

## Objective

Complete monetisation.

### Google Play

Target SDK

Android 16 (API 36)

Business Model

One-time Pro Unlock

Price

£1.99

### Rules

No subscriptions.

No adverts.

No accounts.

No cloud entitlement.

Offline entitlement cache.

PremiumManager owns feature access.

Billing module owns Google Play interaction.

---

# Phase 12 — Testing

## Unit Tests

- Domain
- Timeline
- Storage
- Renderer
- Audio

## Integration Tests

- Playback
- Export
- Billing

## Golden Tests

- Frame hashes
- Audio checksum
- Timeline ordering

## Performance Tests

- Startup
- Export
- Memory
- Audio latency
- 60 FPS preview

---

# Phase 13 — Optimisation

## Optimise

- Startup time
- Rendering
- Memory
- Audio
- Export throughput

Meet every Engineering Principle performance budget.

---

# Phase 14 — Release Hardening

## Verify

- Android 16 compatibility
- Play Console compliance
- Billing
- Accessibility
- Baseline Profiles
- R8 optimisation
- ProGuard
- Crash handling
- Export validation

---

# Phase 15 — Google Play Release

## Final Verification

✓ Engineering Principles

✓ ADR compliance

✓ Compatibility Policy

✓ Definition of Done

✓ Android 16 target

✓ Offline-first

✓ Deterministic export

✓ 1-bit renderer

✓ Portable .mbeat documents

✓ .mbscene compatibility

✓ Google Play Billing

✓ £1.99 Pro Unlock

---

# Success Definition

Monochrome Beat is complete only when it delivers:

- Fast beat creation.
- Deterministic audio.
- Deterministic pixel-art animation.
- Fully offline workflow.
- Portable project ownership.
- Professional Android performance.
- Google Play release quality.
