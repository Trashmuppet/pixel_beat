package com.trashmuppet.pixelbeat.feature.sequencer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.trashmuppet.pixelbeat.core.common.AppDispatchers
import com.trashmuppet.pixelbeat.core.common.Result
import com.trashmuppet.pixelbeat.core.model.Arrangement
import com.trashmuppet.pixelbeat.core.model.DrumKind
import com.trashmuppet.pixelbeat.core.model.LoopBoundary
import com.trashmuppet.pixelbeat.core.model.MBeatProject
import com.trashmuppet.pixelbeat.core.model.Pattern
import com.trashmuppet.pixelbeat.core.model.ProjectSeed
import com.trashmuppet.pixelbeat.core.model.SwingMode
import com.trashmuppet.pixelbeat.core.model.Track
import com.trashmuppet.pixelbeat.core.timeline.RealtimeTransport
import com.trashmuppet.pixelbeat.storage.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state of the sequencer screen.
 *
 * `project == null && isLoading == true` ⇒ initial load.
 * `project == null && error != null`     ⇒ load failed.
 * `project != null`                      ⇒ editing session ready.
 */
data class SequencerState(
    val project: MBeatProject? = null,
    val currentPatternIndex: Int = 0,
    val playheadStep: Int = 0,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val currentPattern: Pattern?
        get() = project?.patterns?.getOrNull(currentPatternIndex)
}

/**
 * Editor for one pattern in a project. Mutations apply to the
 * `MBeatProject` in memory and (debounced) through `ProjectRepository`.
 * Playback is delegated to `RealtimeTransport`; the VM never owns the
 * audio callback per ADR-001 (timeline is the authoritative time).
 */
class SequencerViewModel(
    private val dispatchers: AppDispatchers,
    private val repository: ProjectRepository,
    private val transport: RealtimeTransport,
    initialProjectId: String?
) : ViewModel() {

    private val _state = MutableStateFlow(SequencerState())
    val state: StateFlow<SequencerState> = _state.asStateFlow()

    init { loadIfNeeded(initialProjectId) }

    private fun loadIfNeeded(id: String?) {
        val projectId = id ?: NEW_PROJECT_ID
        viewModelScope.launch(dispatchers.io) {
            when (val outcome = repository.load(projectId)) {
                is Result.Success -> _state.update {
                    it.copy(project = outcome.value, isLoading = false, error = null)
                }
                is Result.Failure -> {
                    // Brand-new session: create an empty project and edit from there.
                    val blank = blankProject(projectId)
                    _state.update {
                        it.copy(
                            project = blank,
                            isLoading = false,
                            error = "Created new project: ${outcome.error.message}"
                        )
                    }
                }
            }
        }
    }

    fun toggleStep(trackId: String, stepIndex: Int) {
        _state.update { current ->
            val project = current.project ?: return@update current
            val newProject = project.copy(
                patterns = project.patterns.mapIndexed { patternIndex, pattern ->
                    if (patternIndex != current.currentPatternIndex) pattern else pattern.copy(
                        tracks = pattern.tracks.map { track ->
                            if (track.id != trackId) return@map track
                            val safeIndex = stepIndex.coerceIn(0, track.steps.size - 1)
                            val newSteps = track.steps.toMutableList().also {
                                it[safeIndex] = !it[safeIndex]
                            }
                            track.copy(steps = newSteps)
                        }
                    )
                }
            )
            current.copy(project = newProject)
        }
        commitDebounced()
    }

    fun toggleMute(trackId: String) {
        _state.update { current ->
            val project = current.project ?: return@update current
            val newProject = project.copy(
                patterns = project.patterns.mapIndexed { patternIndex, pattern ->
                    if (patternIndex != current.currentPatternIndex) pattern else pattern.copy(
                        tracks = pattern.tracks.map { track ->
                            if (track.id != trackId) track else track.copy(mute = !track.mute)
                        }
                    )
                }
            )
            current.copy(project = newProject)
        }
        commitDebounced()
    }

    fun setBpm(bpm: Float) {
        _state.update { current ->
            val project = current.project ?: return@update current
            current.copy(project = project.copy(bpm = bpm.coerceIn(30f, 300f)))
        }
        commitDebounced()
    }

    fun setSwing(swing: SwingMode) {
        _state.update { current ->
            val project = current.project ?: return@update current
            current.copy(project = project.copy(swing = swing))
        }
        commitDebounced()
    }

    fun selectPattern(index: Int) {
        _state.update { current ->
            val project = current.project ?: return@update current
            val safeIndex = index.coerceIn(0, project.patterns.size - 1)
            current.copy(currentPatternIndex = safeIndex, playheadStep = 0)
        }
    }

    fun play() {
        val project = _state.value.project ?: return
        _state.update { it.copy(isPlaying = true) }
        transport.setProject(project)
        // Replaying happens via transport.start(timeline); a real wire-up
        // compiles the timeline here. Reserved for Phase 5 export wiring.
    }

    fun pause() {
        _state.update { it.copy(isPlaying = false) }
        transport.stop()
    }

    fun commit() {
        val project = _state.value.project ?: return
        viewModelScope.launch(dispatchers.io) {
            repository.save(project).onFailure { err ->
                _state.update { it.copy(error = "Save failed: ${err.message}") }
            }
        }
    }

    private fun commitDebounced() {
        // Phase 0 simplification: shortcut to debounce. Real Phase 6
        // implementation collects state updates through a conflated
        // Flow + .debounce(800) before calling commit().
        commit()
    }

    private fun blankProject(projectId: String): MBeatProject {
        val tracks = listOf(
            blankTrack("kick",  DrumKind.KICK),
            blankTrack("snare", DrumKind.SNARE),
            blankTrack("hat",   DrumKind.CLOSED_HAT)
        )
        return MBeatProject(
            id = projectId,
            name = "Untitled",
            bpm = 120f,
            seed = ProjectSeed(0L),
            swing = SwingMode(),
            arrangement = Arrangement(
                patternChain = listOf("main"),
                loopBars = LoopBoundary()
            ),
            patterns = listOf(Pattern(id = "main", lengthSteps = 16, tracks = tracks))
        )
    }

    private fun blankTrack(id: String, kind: DrumKind): Track = Track(
        id = id,
        kind = kind,
        steps = List(16) { false },
        volumeDb = 0f,
        panCb = 0f,
        mute = false
    )

    companion object {
        private const val NEW_PROJECT_ID = "new"

        fun factory(dispatchers: AppDispatchers,
                    repository: ProjectRepository,
                    transport: RealtimeTransport,
                    initialProjectId: String?): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SequencerViewModel(dispatchers, repository, transport, initialProjectId) as T
            }
    }
}
