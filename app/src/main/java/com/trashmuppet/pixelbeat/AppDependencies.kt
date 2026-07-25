package com.trashmuppet.pixelbeat

import android.content.Context
import com.trashmuppet.pixelbeat.core.common.AppDispatchers
import com.trashmuppet.pixelbeat.core.common.DefaultAppDispatchers
import com.trashmuppet.pixelbeat.core.timeline.RealtimeTransport
import com.trashmuppet.pixelbeat.core.timeline.TestRealtimeTransport
import com.trashmuppet.pixelbeat.feature.export.MediaExporter
import com.trashmuppet.pixelbeat.storage.ProjectRepository
import com.trashmuppet.pixelbeat.storage.StorageProjectRepository

/**
 * Application-scoped dependency container.
 *
 * Phase 3 ships without DI frameworks — we hand-construct the
 * dependencies once in `MainActivity` and pass them through
 * CompositionLocals / arguments. Phase 6 will replace this with a
 * code-generated DI graph (per ADR-006).
 */
class AppDependencies(applicationContext: Context) {
    val dispatchers: AppDispatchers = DefaultAppDispatchers()
    val projectRepository: ProjectRepository =
        StorageProjectRepository(applicationContext, dispatchers)
    val transport: RealtimeTransport = TestRealtimeTransport()

    /**
     * Phase 3 ships a no-op exporter; Phase 5 wires WavEncoder /
     * GifEncoder / Mp4MediaCodecEncoder here.
     */
    val exporter: MediaExporter = object : MediaExporter {
        override suspend fun export(
            project: com.trashmuppet.pixelbeat.core.model.MBeatProject,
            outputFile: java.io.File,
            format: com.trashmuppet.pixelbeat.feature.export.ExportFormat,
            resolution: com.trashmuppet.pixelbeat.feature.export.ExportResolution,
            onProgress: suspend (Float, String) -> Unit
        ) {
            // Pretend progress so the UI bar moves while we wait for the
            // real Phase 5 implementation. Deterministic — three ticks.
            onProgress(0.0f, "Phase 5 placeholder")
            onProgress(1.0f, "Done")
            outputFile.parentFile?.mkdirs()
            outputFile.writeText("# placeholder export")
        }
    }
}
