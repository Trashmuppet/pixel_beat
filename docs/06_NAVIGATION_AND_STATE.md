# Navigation and State

> Phase 6 docs pass — `02_ROADMAP.md` Phase 6 requires that every Phase
> lock down its decisions in writing. This spec sits between
> [`05_TECH_STACK.md`](./05_TECH_STACK.md) and
> [`07_TIMELINE_ENGINE.md`](./07_TIMELINE_ENGINE.md) and lays out the
> single source of truth for movement and state in the Compose graph.

---

## Navigation Graph

Six top-level destinations, ordered exactly as listed in `16_UI_BIBLE.md`:

1. **Home** — entry point. Two primary affordances: Create Project, Browse All.
2. **Project** — recent `.mbeat` list and bundled sample browsers.
3. **Sequencer** — 16-step pattern editor with tempo / swing controls.
4. **Arrangement** — chained pattern slot editor with optional loop range.
5. **Export** — format / resolution picker + progress + success / error.
6. **Settings** — reserved; reaches Navigation Compose in a later phase.

`AppNavHost` instantiates three feature ViewModels (`Sequencer`,
`Arrangement`, `Export`) directly through `viewModel(factory = …)`.
The `factory(…)` arguments take `AppDependencies.dispatchers`,
`projectRepository`, `transport`, and (for Export) `MediaExporter`.

## State Strategy

We use **MVVM with `StateFlow` only**. ADR-003 codifies this:
no two-way data binding, no LiveData, no `RxJava`.

* **ViewModels** own the mutable UI state. Each exposes a single
  `StateFlow<UiState>` collected via `collectAsStateWithLifecycle()`.
* **Repositories** own external I/O. ViewModels never touch the
  filesystem or audio engine directly — they go through
  `ProjectRepository`, `AudioFramesSource`, `MediaExporter`, and
  `RealtimeTransport` abstractions.
* **`SavedStateHandle`** is used **only for navArgs** (e.g.
  `projectId`). Persistent edits flush to the repository on
  `commit()` — there is no "cross-process state" held in memory.

## BackHandler semantics

| Destination | Back behaviour |
|---|---|
| Home | System default (exits the app). |
| Project | `navController.popBackStack()`. |
| Sequencer | `navController.popBackStack()`; if `state.isPlaying`, pause first. |
| Arrangement | `navController.popBackStack()`. |
| Export | **Cancel in-flight** coroutine via `viewModelScope.cancel()`, then pop. |
| Settings | Pop. |

`ExportViewModel.startExport` keeps its `Job` so `BackHandler` can
`cancel()` it without losing the user's other navigation context.

## Permission Matrix

Per `01_PRODUCT_PILLARS.md` we deliberately ship no `INTERNET`
permission. Other permissions are scoped to where they are
technically required:

| Permission | Where | API ≤ 28 | API ≥ 29 (scoped) |
|---|---|---|---|
| `WRITE_EXTERNAL_STORAGE` | `ExportScreen` writes | Only | **Not needed** |
| `READ_MEDIA_AUDIO/VIDEO` | Export preview | n/a | Not needed for offline |
| `RECORD_AUDIO` | _none_ | n/a | n/a |

For API ≤ 28 the export pipeline writes to `getExternalFilesDir()`,
which does not require a runtime permission.

## State invariants

* `state.project == null && state.isLoading == true`   ⇒ loading view.
* `state.project == null && state.error != null`       ⇒ error view.
* `state.project != null`                              ⇒ editor ready.
* A `coroutineScope` survives configuration changes via
  `viewModelScope`; no manual `SavedStateHandle` plumbing for
  in-flight work.

## Accessibility hooks

Playbook for `Modifier.semantics` additions (delivered alongside):

* `Modifier.semantics { role = Role.Checkbox }` on `StepCell`.
* `Modifier.semantics { role = Role.Switch }` on toggle surfaces.
* `Modifier.semantics { stateDescription = "$value bpm" }` on
  tempo/swing sliders to make TalkBack value-changes explicit.
* `Modifier.semantics { testTag = "route/<routeName>" }` on the
  outermost screen `Column` so instrumented tests can target
  routes without strings coupled to UI layouts.
