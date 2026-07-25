package com.trashmuppet.pixelbeat

import com.trashmuppet.pixelbeat.audio.AudioEngine
import com.trashmuppet.pixelbeat.core.export.AudioFramesSource
import com.trashmuppet.pixelbeat.core.model.MBeatProject
import com.trashmuppet.pixelbeat.core.timeline.TimelineCompiler

/**
 * Production `AudioFramesSource` for the export pipeline.
 *
 * Compiles the `MBeatProject` via `TimelineCompiler` (`07_TIMELINE_ENGINE.md`),
 * pre-schedules the first 4096 hit offsets onto `AudioEngine`, and
 * returns chunks of float32 samples at 48 kHz mono. The encoder pipeline
 * reads these chunks in lockstep with the scene-side frame advance.
 */
class DefaultAudioFramesSource(
    private val audioEngine: AudioEngine
) : AudioFramesSource {

    private var totalSamples: Long = 0L
    private var samplesRendered: Long = 0L
    private val offsets: LongArray = LongArray(MAX_PRE_SCHEDULE)

    override fun open(project: MBeatProject, sampleRate: Int) {
        require(sampleRate == 48_000) {
            "AudioEngine is locked to 48 kHz; sampleRate=$sampleRate unsupported."
        }
        val compiler = TimelineCompiler()
        val timeline = compiler.compile(project, sampleRate)
        audioEngine.init(sampleRate)
        val count = minOf(timeline.events.size, MAX_PRE_SCHEDULE)
        for (i in 0 until count) offsets[i] = timeline.events[i].tick
        // Vibrato-free first hit; remaining events are reactive above
        // the pre-scheduling horizon via additional `schedule` calls.
        audioEngine.loadProject(project, offsets)
        totalSamples = timeline.totalSamples.coerceAtLeast(0L)
        samplesRendered = 0L
    }

    override fun renderChunk(out: FloatArray): Int {
        require(out.isNotEmpty()) { "out must have capacity" }
        // `AudioEngine.renderOffline` currently allocates the result
        // itself; for the streaming contract we memcpy into `out` and
        // report the actual chunk size.
        val result = audioEngine.renderOffline(out.size)
        if (result.isEmpty()) return 0
        System.arraycopy(result, 0, out, 0, result.size)
        samplesRendered += result.size
        return result.size
    }

    override fun isExhausted(): Boolean =
        totalSamples > 0L && samplesRendered >= totalSamples

    override fun totalSamples(): Long = totalSamples

    override fun close() {
        audioEngine.stop()
    }

    companion object {
        private const val MAX_PRE_SCHEDULE = 4096
    }
}
