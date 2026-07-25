# Renderer

## Purpose
Render immutable SceneRenderState into 1-bit frames.

## Visual Rules
- #000000
- #FFFFFF
- No alpha
- No gradients
- No anti-aliasing
- Integer scaling

## Output
- Live preview
- MP4 export
- GIF export

## Rules
- Renderer never changes simulation state.
- Reads SceneRenderState only.
- 60 FPS preview.
- 30/60 FPS export.
- Android 16 (API 36) target.
