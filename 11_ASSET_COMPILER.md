# Asset Compiler

## Purpose
Convert artist-authored source assets into validated `.mbscene` packages.

## Inputs
- PNG sprites
- JSON manifests
- Animation definitions

## Outputs
- Compiled `.mbscene`
- Manifest
- Content hash

## Validation
- 1-bit colours only (#000000/#FFFFFF)
- Integer dimensions
- One-pixel outlines
- No alpha
- No anti-aliasing
- Runtime compatibility metadata

## Rules
Runtime never loads raw artwork.
Compilation must be deterministic.
Target Android 16 (API 36).
