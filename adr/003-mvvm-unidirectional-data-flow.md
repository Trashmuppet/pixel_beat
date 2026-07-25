# ADR-003: MVVM Unidirectional Data Flow

| Field | Value |
|---|---|
| **Date** | 2026-07-25 |
| **Status** | Accepted |
| **Deciders** | Engineering, UI team |
| **Consulted** | Architecture |

## Context

Compose + Kotlin Coroutines make it easy to fall back on LiveData,
two-way `MutableState` bindings, `SharedFlow` hot streams, and
imperative repo calls scattered through composables. We've seen
testability collapse on previous products where every feature
screen re-implemented its own "this widget → this state" graph.

## Decision

* Every ViewModel exposes a single `MutableStateFlow<UiState>`.
* UI subscribes via `collectAsStateWithLifecycle()`. Hot
  Compose, cold Flow.
* Mutations on the ViewModel go through `viewModelScope` to a
  Repository / Service abstraction. **Never** to a Composable's
  local state.
* `SavedStateHandle` is reserved for navArgs (currently just
  `projectId`). Persistent application state lives in the
  `:storage` repository.
* Forbidden: LiveData, two-way `MutableState` bindings in features,
  `asStateFlow()` of a `MutableSharedFlow` for primitive types.

## Consequences

* Positive: every feature screen has one observable (`state.value`)
  and one entry-point class — tests stay trivial to write.
* Positive: rotation only re-binds the Flow — no `onRestoreInstance`
  plumbing spreads across screens.
* Negative: a learner's first Compose piece in `feature-*` feels
  verbose (every binding wrapped in `semantics` + `state`).
  Mitigated by `:core:ui` carrying reuseable composables.
* Out of scope: command/event buses. We deliberately do NOT
  introduce a `CommandBus` or `EffectChannel` — coroutines + repo
  calls cover the surface.
