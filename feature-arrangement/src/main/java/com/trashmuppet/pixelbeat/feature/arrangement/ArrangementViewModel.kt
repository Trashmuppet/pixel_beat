package com.trashmuppet.pixelbeat.feature.arrangement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.trashmuppet.pixelbeat.core.common.AppDispatchers
import com.trashmuppet.pixelbeat.core.common.Result
import com.trashmuppet.pixelbeat.core.model.LoopBoundary
import com.trashmuppet.pixelbeat.core.model.MBeatProject
import com.trashmuppet.pixelbeat.core.model.Pattern
import com.trashmuppet.pixelbeat.core.model.SwingMode
import com.trashmuppet.pixelbeat.storage.ProjectRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArrangementState(
    val project: MBeatProject? = null,
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val patternChain: List<String> get() = project?.arrangement?.patternChain ?: emptyList()
    val loop: LoopBoundary get() = project?.arrangement?.loopBars ?: LoopBoundary()
    val patternLookup: Map<String, Pattern> get() = project?.patterns?.associateBy { it.id } ?: emptyMap()
}

@OptIn(FlowPreview::class)
class ArrangementViewModel(
    private val dispatchers: AppDispatchers,
    private val repository: ProjectRepository,
    initialProjectId: String?
) : ViewModel() {

    private val _state = MutableStateFlow(ArrangementState())
    val state: StateFlow<ArrangementState> = _state.asStateFlow()

    init {
        load(initialProjectId)
        // Debounced save: catches state mutations from slider drags
        // (setSwing / setLoop) and quick toggles without producing
        // one save per frame.
        viewModelScope.launch(dispatchers.io) {
            _state
                .map { it.project }
                .distinctUntilChanged()
                .debounce(SAVE_DEBOUNCE_MS)
                .collectLatest { project ->
                    if (project != null) repository.save(project)
                }
        }
    }

    private fun load(id: String?) {
        viewModelScope.launch(dispatchers.io) {
            when (val outcome = repository.load(id ?: NEW_PROJECT_ID)) {
                is Result.Success -> _state.update {
                    it.copy(project = outcome.value, isLoading = false, error = null)
                }
                is Result.Failure -> _state.update {
                    it.copy(isLoading = false, error = outcome.error.message)
                }
            }
        }
    }

    fun reorderPattern(from: Int, to: Int) {
        _state.update { current ->
            val project = current.project ?: return@update current
            val chain = project.arrangement.patternChain.toMutableList()
            if (!(from in chain.indices && to in chain.indices)) return@update current
            val moved = chain.removeAt(from)
            chain.add(to, moved)
            current.copy(project = project.copy(
                arrangement = project.arrangement.copy(patternChain = chain)
            ))
        }
        commit()
    }

    fun addPattern() {
        _state.update { current ->
            val project = current.project ?: return@update current
            val source = project.patterns.firstOrNull() ?: return@update current
            val newPatternId = "p-${System.currentTimeMillis()}"
            val newPattern = Pattern(
                id = newPatternId,
                lengthSteps = source.lengthSteps,
                tracks = source.tracks.map { it.copy(steps = it.steps.map { false }) }
            )
            project.copy(
                patterns = project.patterns + newPattern,
                arrangement = project.arrangement.copy(
                    patternChain = project.arrangement.patternChain + newPatternId
                )
            ).let { current.copy(project = it) }
        }
        commit()
    }

    fun removePattern(index: Int) {
        _state.update { current ->
            val project = current.project ?: return@update current
            val chain = project.arrangement.patternChain
            if (chain.size <= 1 || index !in chain.indices) return@update current
            val removeId = chain[index]
            val newChain = chain.toMutableList().also { it.removeAt(index) }
            project.copy(
                patterns = project.patterns.filter { it.id != removeId },
                arrangement = project.arrangement.copy(patternChain = newChain)
            ).let { current.copy(project = it) }
        }
        commit()
    }

    /**
     * Persist the arrangement's loop bar range. Slider-driven so we
     * rely on the debounced collector in [init] for the write; we
     * only update state here.
     */
    fun setLoop(startBar: Int, endBar: Int) {
        _state.update { current ->
            val project = current.project ?: return@update current
            current.copy(project = project.copy(
                arrangement = project.arrangement.copy(
                    loopBars = LoopBoundary(startBar.coerceAtLeast(0), endBar.coerceAtLeast(startBar))
                )
            ))
        }
    }

    /**
     * Update swing mode. Mirrors [SequencerViewModel.setSwing]; both
     * screens can edit swing because each commits the full project
     * document. Slider-driven, relies on the debounced init collector.
     */
    fun setSwing(swing: SwingMode) {
        _state.update { current ->
            val project = current.project ?: return@update current
            current.copy(project = project.copy(swing = swing))
        }
    }

    fun commit() {
        val project = _state.value.project ?: return
        viewModelScope.launch(dispatchers.io) {
            repository.save(project).onFailure { err ->
                _state.update { it.copy(error = "Save failed: ${err.message}") }
            }
        }
    }

    companion object {
        private const val NEW_PROJECT_ID = "new"
        private const val SAVE_DEBOUNCE_MS = 800L

        fun factory(dispatchers: AppDispatchers,
                    repository: ProjectRepository,
                    initialProjectId: String?): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ArrangementViewModel(dispatchers, repository, initialProjectId) as T
            }
    }
}
