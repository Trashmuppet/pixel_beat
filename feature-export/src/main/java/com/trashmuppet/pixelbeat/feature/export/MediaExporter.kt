package com.trashmuppet.pixelbeat.feature.export

import com.trashmuppet.pixelbeat.core.model.MBeatProject
import java.io.File

/**
 * Contract for the actual export pipeline (Phase 5 wires the concrete
 * WavExporter / GifExporter / Mp4MediaCodecEncoder implementations).
 *
 * The ViewModel calls this off the main thread with a progress
 * callback the UI consumes via `ExportState.Rendering`.
 */
interface MediaExporter {
    suspend fun export(
        project: MBeatProject,
        outputFile: File,
        format: ExportFormat,
        resolution: ExportResolution,
        onProgress: suspend (Float, String) -> Unit
    )
}
