# Storage

## Source of Truth
Versioned UTF-8 `.mbeat` documents.

## Local Cache
Room database for:
- Recent projects
- Search index
- Metadata

Room is rebuildable and never authoritative.

## Project Layout
Projects/
Exports/
ScenePacks/

## Principles
- Atomic saves
- Explicit migrations
- Offline only
- No cloud sync
- Preserve unsupported documents unchanged.
