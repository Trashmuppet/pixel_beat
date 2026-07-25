# Scene System

## Purpose
Transform drum events into deterministic pixel-art animation.

## Architecture
Timeline → HitEvents → AnimationSystem → SceneRenderState → Renderer

## Scene Packs
Compiled `.mbscene` packs only.

Each pack declares:
- Pack ID
- Version
- Runtime compatibility
- Schema version
- Content hash

## Rules
- Declarative packs.
- No executable plug-ins.
- Fixed 240 Hz simulation.
- No automatic downloads.
- Offline only.
