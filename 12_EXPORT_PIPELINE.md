# Export Pipeline

## Purpose
Generate reproducible media from a frozen `.mbeat` project.

## Supported Formats
- WAV
- MP4
- GIF

## Pipeline
.mbeat
→ TimelineCompiler
→ Offline Audio Renderer
→ 240 Hz Scene Simulation
→ Frame Renderer
→ Media Encoder

## Rules
- Export is the reference implementation.
- Streaming only; no unbounded buffering.
- Identical input produces identical output.
- Works entirely offline.

## Performance Targets
- Stable 720p export.
- Memory within engineering budget.
