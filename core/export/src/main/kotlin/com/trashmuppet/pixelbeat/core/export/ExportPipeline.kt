package com.trashmuppet.pixelbeat.core.export

import com.trashmuppet.pixelbeat.core.common.Result
import com.trashmuppet.pixelbeat.core.common.runCatchingResult
import com.trashmuppet.pixelbeat.core.export.audio.WavEncoder
import com.trashmuppet.pixelbeat.core.export.video.GifEncoder
import com.trashmuppet.pixelbeat.core.export.video.Mp4MediaCodecEncoder
import com.trashmuppet.pixelbeat.core.model.MBeatProject
import com.trashmuppet.pixelbeat.feature.export.ExportFormat
import com.trashmuppet.pixelbeat.feature.export.ExportResolution
import com.trashmuppet.pixelbeat.scene.runtime.AnimationSystem
import kotlinx.coroutines.yield
import java.io.File

/**
 * Orchestrates the offline render.
 *
 * Streams audio chunks in lockstep with scene simulation ticks,
 * dispatching frame payloads to the encoder for the chosen format.
 *
 * Sub-progress contract:
 *   0.00 → 0.05  — preparation / encoder setup (`onProgress` is fired once).
 *   0.05 → 0.99  — encoding loop.
 *   1.00         — file written and flushed; caller can navigate away.
 */
class ExportPipeline(
    private val audioFrames: AudioFramesSource,
    private val animationSystem: AnimationSystem
) {
    /** Convenience: the scene owned by `animationSystem`. */
    private val scene get() = animationSystem.scene

    suspend fun run(
        project: MBeatProject,
        outputFile: File,
        format: ExportFormat,
        resolution: ExportResolution,
        fps: Int = 30,
        onProgress: suspend (Float, String) -> Unit
    ): Result<File> = runCatchingResult {
        audioFrames.open(project, 48_000)
        animationSystem.reset()
        scene.load(project)
        when (format) {
            ExportFormat.WAV -> runWav(project, outputFile, onProgress)
            ExportFormat.GIF -> runGif(outputFile, resolution, fps, onProgress)
            ExportFormat.MP4 -> runMp4(project, outputFile, resolution, fps, onProgress)
        }
    }.also { audioFrames.close() }

    // ------------------------------------------------------------------
    // WAV — collect samples in memory, write a single RIFF blob.
    //
    // The PCM format requires `data` chunk size be known up front;
    // the clean Phase-6 upgrade is to write a 0-size header, stream
    // chunks, then patch the data length at finish. We leave that
    // for follow-up; Phase 5 ships the bounded-memory version.
    // ------------------------------------------------------------------
    private suspend fun runWav(
        project: MBeatProject,
        outputFile: File,
        onProgress: suspend (Float, String) -> Unit
    ): File {
        val sampleRate = 48_000
        onProgress(0.05f, "Encoding WAV")
        val chunkSize = sampleRate / 4
        val chunk = FloatArray(chunkSize)
        val collected = ArrayList<Float>(chunkSize * 8)
        val total = audioFrames.totalSamples().coerceAtLeast(1L)
        var rendered = 0L
        while (!audioFrames.isExhausted()) {
            val n = audioFrames.renderChunk(chunk)
            if (n <= 0) break
            for (i in 0 until n) collected.add(chunk[i])
            rendered += n
            if (rendered % (sampleRate / 2) == 0L) {
                onProgress(
                    (0.05f + 0.94f * (rendered.toFloat() / total.toFloat())).coerceIn(0.05f, 0.99f),
                    "WAV: $rendered of $total samples"
                )
                yield()
            }
        }
        val arr = FloatArray(collected.size) { collected[it] }
        WavEncoder(sampleRate).encode(arr, outputFile)
        onProgress(1.0f, "WAV complete")
        return outputFile
    }

    // ------------------------------------------------------------------
    // GIF — 1-bit indexed palette, frame-by-frame via LZW clear-once.
    // ------------------------------------------------------------------
    private suspend fun runGif(
        outputFile: File,
        resolution: ExportResolution,
        fps: Int,
        onProgress: suspend (Float, String) -> Unit
    ): File {
        val width = evenUp(resolution.width)
        val height = evenUp(resolution.height)
        onProgress(0.05f, "Encoder ready ($width×$height GIF)")
        val gif = GifEncoder()
        gif.begin(outputFile, width, height)
        val frameMs = 1000 / fps.coerceAtLeast(1)
        val audioFramesPerFrame = 48_000 / fps.coerceAtLeast(1)
        val audioChunk = FloatArray(audioFramesPerFrame)
        val ticksPerFrame = audioFramesPerFrame / 200
        var frameIndex = 0
        var rendered = 0L
        val total = audioFrames.totalSamples().coerceAtLeast(1L)
        while (!audioFrames.isExhausted()) {
            audioFrames.renderChunk(audioChunk)
            repeat(ticksPerFrame) { animationSystem.advance(200, 48_000) }
            rendered += audioFramesPerFrame.toLong()
            val state = animationSystem.latestRenderState() ?: continue
            gif.writeFrame(state, frameMs)
            frameIndex += 1
            if (frameIndex % 4 == 0) {
                onProgress(
                    (0.05f + 0.94f * (rendered.toFloat() / total.toFloat())).coerceIn(0.05f, 0.99f),
                    "GIF: $frameIndex frames"
                )
                yield()
            }
        }
        gif.finish()
        onProgress(1.0f, "GIF complete")
        return outputFile
    }

    // ------------------------------------------------------------------
    // MP4 — MediaCodec + MediaMuxer with the AAC fallback rule (ADR-005).
    // ------------------------------------------------------------------
    private suspend fun runMp4(
        project: MBeatProject,
        outputFile: File,
        resolution: ExportResolution,
        fps: Int,
        onProgress: suspend (Float, String) -> Unit
    ): File {
        val width = evenUp(resolution.width)
        val height = evenUp(resolution.height)
        onProgress(0.05f, "MP4 encoder ready ($width×$height @ ${fps}fps)")
        val encoder = Mp4MediaCodecEncoder(width, height, fps)
        encoder.encode(project, audioFrames, outputFile) { p, s ->
            onProgress(p.coerceIn(0.05f, 0.99f), s)
        }
        onProgress(1.0f, "MP4 complete")
        return outputFile
    }

    private fun evenUp(px: Int): Int = if (px and 1 == 1) px + 1 else px
}
