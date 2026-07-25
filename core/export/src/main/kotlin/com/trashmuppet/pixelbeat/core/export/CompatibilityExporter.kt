package com.trashmuppet.pixelbeat.core.export

import com.trashmuppet.pixelbeat.core.model.MBeatProject
import com.trashmuppet.pixelbeat.feature.export.ExportFormat
import com.trashmuppet.pixelbeat.feature.export.ExportResolution
import com.trashmuppet.pixelbeat.feature.export.MediaExporter
import java.io.File

/**
 * `MediaExporter` adapter that funnels every format through the
 * production `ExportPipeline`. Phase 5 replaces the no-op stub in
 * `AppDependencies`.
 */
class CompatibilityExporter(
    private val pipeline: ExportPipeline
) : MediaExporter {

    override suspend fun export(
        project: MBeatProject,
        outputFile: File,
        format: ExportFormat,
        resolution: ExportResolution,
        onProgress: suspend (Float, String) -> Unit
    ): Result<File> {
        val result = pipeline.run(
            project = project,
            outputFile = outputFile,
            format = format,
            resolution = resolution,
            fps = 30,
            onProgress = onProgress
        )
        return result
    }
}
