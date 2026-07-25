package com.trashmuppet.pixelbeat.feature.export

import com.trashmuppet.pixelbeat.core.model.MBeatProject
import java.io.File

/**
 * Output formats supported by `MediaExporter` (Phase 5 implementation).
 */
enum class ExportFormat(val extension: String) {
    WAV("wav"),
    MP4("mp4"),
    GIF("gif");

    val isVideo: Boolean get() = this == MP4 || this == GIF
}

/**
 * Output resolution. SD_480 / HD_720 / FHD_1080 are integer-scaled per
 * `10_RENDERER.md`. GIF uses the width only and the integer-scaled
 * height, since `12_EXPORT_PIPELINE.md` exports 1-bit GIFs.
 */
enum class ExportResolution(val width: Int, val height: Int) {
    SD_480(854, 480),
    HD_720(1280, 720),
    FHD_1080(1920, 1080);

    val isExportableToGif: Boolean get() = true
}

/**
 * UI state of Export screen. Sealed so the screen can hard-match every
 * branch in a `when` (the renderer cannot accidentally fall through).
 */
sealed class ExportState {
    /** No project loaded yet. */
    data object Idle : ExportState()

    /** User is picking format / resolution. */
    data class Configuring(
        val project: MBeatProject,
        val format: ExportFormat,
        val resolution: ExportResolution
    ) : ExportState()

    /** Render in progress. */
    data class Rendering(val progress: Float, val statusLine: String) : ExportState()

    /** Render finished — `path` is the absolute file. */
    data class Success(val path: String) : ExportState()

    /** Render failed. */
    data class Error(val cause: Throwable) : ExportState()
}
