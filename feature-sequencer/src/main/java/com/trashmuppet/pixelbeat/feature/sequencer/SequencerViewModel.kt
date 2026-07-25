package com.trashmuppet.pixelbeat.feature.sequencer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
import com.trashmuppet.pixelbeat.core.timeline.TimelineCompiler
import com.trashmuppet.pixelbeat.storage.ProjectRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state of the sequencer screen.
 *
 * `project == null && isLoading == true` ⇒ initial load.
 * `project == null && error != null`     ⇒ load failed.
 * `project != null`                      ⇒ editing session ready.
 *
 * `playheadStep` is **derived** from [RealtimeTransport.positionFlow]
 * per ADR-001 — the UI never maintains its own tick counter.
 */
data class SequencerState(
    val project: MBeatProject? = null,
    val currentPatternIndex: Int = 0,
    val playheadStep: Int = 0,
    /**
     * Latest transport-emitted sample position. Per ADR-001 the tick
     * position is owned by the transport; [AnimatedPlayhead] reads this
     * verbatim and uses `withFrameNanos` to smoothly interpolate
     * visually between emissions without ever owning the tick itself.
     */
    val samplePosition: Long = 0L,
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
 *
 * Per ADR-001 the transport owns sample-accurate timing. This VM
 * subscribes to `transport.positionFlow()` and projects the sample
 * position into a step index for the playhead UI.
 */
@OptIn(FlowPreview::class)
class SequencerViewModel(
    private val dispatchers: AppDispatchers,
    private val repository: ProjectRepository,
    private val transport: RealtimeTransport,
    private val compiler: TimelineCompiler = TimelineCompiler(),
    initialProjectId: String?
) : ViewModel() {

    private val _state = MutableStateFlow(SequencerState())
    val state: StateFlow<SequencerState> = _state.asStateFlow()

    /**
     * Plays the project through the realtime transport. Per ADR-001 the
     * transport owns time; we just compile the timeline and start it.
     * Cancelling [playJob] effectively pauses playback.
     */
    private var playJob: Job? = null

    init {
        loadIfNeeded(initialProjectId)
        // Debounce save commits so a fast step-toggle burst doesn't
        // produce a serialised save per keystroke.
        viewModelScope.launch(dispatchers.io) {
            _state
                .map { it.project }
                .distinctUntilChanged()
                .debounce(SAVE_DEBOUNCE_MS)
                .collectLatest { project ->
                    if (project != null) {
                        repository.save(project)
                    }
                }
        }
    }

    private fun loadIfNeeded(id: String?) {
        val projectId = id ?: NEW_PROJECT_ID
        viewModelScope.launch(dispatchers.io) {
            when (val outcome = repository.load(projectId)) {
                is Result.Success -> _state.update {
                    it.copy(project = outcome.value, isLoading = false, error = null)
                }
                is Result.Failure -> {
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
    }

    fun setBpm(bpm: Float) {
        _state.update { current ->
            val project = current.project ?: return@update current
            current.copy(project = project.copy(bpm = bpm.coerceIn(30f, 300f)))
        }
    }

    fun setSwing(swing: SwingMode) {
        _state.update { current ->
            val project = current.project ?: return@update current
            current.copy(project = project.copy(swing = swing))
        }
    }

    fun selectPattern(index: Int) {
        _state.update { current ->
            val project = current.project ?: return@update current
            val safeIndex = index.coerceIn(0, project.patterns.size - 1)
            current.copy(currentPatternIndex = safeIndex)
        }
    }

    /**
     * Compile the project and start realtime playback. Per ADR-001 the
     * transport owns time; we just hand it the [CompiledTimeline] and
     * subscribe the step-derive collector to positionFlow().
     */
    fun play() {
        val project = _state.value.project ?: return
        val timeline = compiler.compile(project)
        transport.start(timeline)
        _state.update { it.copy(isPlaying = true) }
        // Drive the playhead step counter from the transport's
        // sample-accurate position flow. Single source of truth.
        val sampleRate = timeline.internalSampleRate.toFloat()
        val baseBeatSeconds = 60f / project.bpm.coerceAtLeast(1f)
        val sixteenthNoteSamples = (sampleRate * baseBeatSeconds / 4f).coerceAtLeast(1f)
        val totalSteps = project.patterns.firstOrNull()?.lengthSteps ?: 16
        playJob?.cancel()
        playJob = viewModelScope.launch(dispatchers.default) {
            transport.positionFlow().collect { samplePosition ->
                if (samplePosition < 0) return@collect
                val stepIndex = (samplePosition / sixteenthNoteSamples)
                    .toInt()
                    .mod(totalSteps)
                _state.update {
                    // Publish both the authoritative integer step AND
                    // the raw sample position so AnimatedPlayhead can
                    // interpolate visually with withFrameNanos (ADR-001).
                    it.copy(playheadStep = stepIndex, samplePosition = samplePosition)
                }
            }
        }
    }

    /**
     * Stop realtime playback. Per ADR-001 the transport stays the
     * authoritative clock — we just stop it. UI keeps its last
     * playheadStep so the user can resume cleanly.
     */
    fun pause() {
        playJob?.cancel()
        playJob = null
        transport.stop()
        _state.update { it.copy(isPlaying = false) }
    }

    /** Force an immediate save (used on BackHandler, scene-change, etc.). */
    fun commit() {
        val project = _state.value.project ?: return
        viewModelScope.launch(dispatchers.io) {
            repository.save(project).onFailure { err ->
                _state.update { it.copy(error = "Save failed: ${err.message}") }
            }
        }
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
        private const val SAVE_DEBOUNCE_MS = 800L

        // SharingStarted.Eagerly so the player sees isPlaying = true on first paint.
        private val playStarted = SharingStarted.Eagerly

        fun factory(dispatchers: AppDispatchers,
                    repository: ProjectRepository,
                    transport: RealtimeTransport,
                    compiler: TimelineCompiler = TimelineCompiler(),
                    initialProjectId: String?): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SequencerViewModel(
                        dispatchers,
                        repository,
                        transport,
                        compiler,
                        initialProjectId
                    ) as T
            }
    }
}
