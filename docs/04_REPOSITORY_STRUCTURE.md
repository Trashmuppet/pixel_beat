# Repository Structure

## Target Platform
- Android 16 (API 36)
- Kotlin
- Jetpack Compose
- Material 3

## Modules

```
app/
core/
  model/
  timeline/
  common/
audio-native/
scene-api/
scene-runtime/
scene-warehouse/
feature-home/
feature-project/
feature-sequencer/
feature-arrangement/
feature-export/
premium/
billing/
storage/
asset-compiler/
testing/
docs/
adr/
```

## Rules

- Feature modules depend on abstractions.
- Billing is isolated in `:billing`.
- Features query `PremiumManager`.
- `storage` owns `.mbeat` persistence.
- `scene-runtime` never owns musical timing.
- `audio-native` contains the real-time engine only.
- Room is an index/cache and never the source of truth.

## Dependency Direction

UI → Domain → Core

Never reverse dependencies.

## Build Target

Always compile against Android 16 (API 36).
