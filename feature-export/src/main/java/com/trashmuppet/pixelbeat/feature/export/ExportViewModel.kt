package com.trashmuppet.pixelbeat.feature.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.trashmuppet.pixelbeat.core.common.AppDispatchers
import com.trashmuppet.pixelbeat.core.common.Result
import com.trashmuppet.pixelbeat.core.model.MBeatProject
import com.trashmuppet.pixelbeat.premium.PremiumManager
import com.trashmuppet.pixelbeat.storage.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class ExportViewModel(
    private val dispatchers: AppDispatchers,
    private val repository: ProjectRepository,
    private val exporter: MediaExporter,
    val premiumManager: PremiumManager,
    initialProjectId: String?
) : ViewModel() {

    private val _state = MutableStateFlow<ExportState>(ExportState.Idle)
    val state: StateFlow<ExportState> = _state.asStateFlow()

    init { load(initialProjectId) }

    private fun load(id: String?) {
        viewModelScope.launch(dispatchers.io) {
            val targetId = id ?: "sample"
            when (val outcome = repository.load(targetId)) {
                is Result.Success -> _state.value = ExportState.Configuring(
                    project = outcome.value,
                    format = ExportFormat.WAV,
                    resolution = ExportResolution.SD_480
                )
                is Result.Failure -> _state.value = ExportState.Error(outcome.error)
            }
        }
    }

    fun selectFormat(format: ExportFormat) {
        _state.update { current ->
            when (current) {
                is ExportState.Configuring -> current.copy(format = format)
                else -> current
            }
        }
    }

    fun selectResolution(res: ExportResolution) {
        _state.update { current ->
            when (current) {
                is ExportState.Configuring -> current.copy(resolution = res)
                else -> current
            }
        }
    }

    fun startExport(outputFile: File) {
        val current = _state.value as? ExportState.Configuring ?: return
        viewModelScope.launch(dispatchers.io) {
            _state.value = ExportState.Rendering(progress = 0f, statusLine = "Starting...")
            try {
                exporter.export(
                    project = current.project,
                    outputFile = outputFile,
                    format = current.format,
                    resolution = current.resolution,
                    onProgress = ::publishProgress
                )
                _state.value = ExportState.Success(outputFile.absolutePath)
            } catch (t: Throwable) {
                _state.value = ExportState.Error(t)
            }
        }
    }

    /**
     * Called by the exporter-side progress callback to push a status
     * line so the UI updates while the render is in flight.
     */
    fun publishProgress(progress: Float, statusLine: String) {
        _state.value = ExportState.Rendering(progress = progress.coerceIn(0f, 1f), statusLine = statusLine)
    }

    /**
     * Reload the current project from disk. The previous
     * implementation called `load(null)` twice (with a dead `load(false)`),
     * which always constructed a fresh blank project. Fix: invoke
     * `load(currentId)` so a coroutine-cancelled export leaves the
     * editor in a deterministic state.
     */
    fun reset() {
        val currentId = currentProjectId()
        // Force a fresh load: pass null then the discovered id so we
        // skip any in-memory `_state` cached value.
        viewModelScope.launch(dispatchers.io) {
            _state.value = ExportState.Idle
            when (val outcome = repository.load(currentId ?: "sample")) {
                is Result.Success -> _state.value = ExportState.Configuring(
                    project = outcome.value,
                    format = ExportFormat.WAV,
                    resolution = ExportResolution.SD_480
                )
                is Result.Failure -> _state.value = ExportState.Error(outcome.error)
            }
        }
    }

    /** The id of the project the ViewModel is currently editing. */
    private fun currentProjectId(): String? = when (val s = _state.value) {
        is ExportState.Configuring -> s.project.id
        else -> null
    }

    companion object {
        fun factory(dispatchers: AppDispatchers,
                    repository: ProjectRepository,
                    exporter: MediaExporter,
                    premiumManager: PremiumManager,
                    initialProjectId: String?): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ExportViewModel(dispatchers, repository, exporter, premiumManager, initialProjectId) as T
            }
    }
}
