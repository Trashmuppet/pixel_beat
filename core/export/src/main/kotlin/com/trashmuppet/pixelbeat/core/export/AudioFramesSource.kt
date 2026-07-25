package com.trashmuppet.pixelbeat.core.export

import com.trashmuppet.pixelbeat.core.model.MBeatProject

/**
 * Source of audio frames for the export pipeline.
 *
 * Decouples the encoder from how audio is actually produced. The
 * canonical implementation wraps the on-device audio engine
 * (`AudioEngine` in `:audio-native`); tests provide a synthesised
 * sine source so the encoder itself can be exercised without the
 * native lib loaded.
 *
 * Streaming contract (`12_EXPORT_PIPELINE.md`):
 *  - `renderChunk` MUST NEVER block on disk / network.
 *  - Output buffer length is the maximum chunk size; returns the
 *    number of frames actually written.
 *  - When `isExhausted()` becomes true, `renderChunk` returns 0.
 */
interface AudioFramesSource {
    fun open(project: MBeatProject, sampleRate: Int)
    fun renderChunk(out: FloatArray): Int
    fun isExhausted(): Boolean
    fun totalSamples(): Long
    fun close()
}
