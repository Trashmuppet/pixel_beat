package com.trashmuppet.pixelbeat.core.timeline

import com.trashmuppet.pixelbeat.core.model.HitEvent
import com.trashmuppet.pixelbeat.core.model.MBeatProject

/**
 * Compiles a frozen `MBeatProject` into an immutable, ordered stream of
 * musical events. Both real-time playback (`RealtimeTransport`) and
 * offline render (`OfflineRenderSession` from `12_EXPORT_PIPELINE.md`)
 * consume the exact same output.
 *
 * Invariants from `07_TIMELINE_ENGINE.md`:
 *  - Sample-accurate scheduling — no wall-clock or UI timing.
 *  - Identical input must produce identical output (export determinism).
 *  - Stable event ordering.
 *
 * Phase 0 implements the minimal track-step → HitEvent mapping. BPM,
 * swing, and arrangement chaining are explicitly stubs awaiting Phase 1.
 */
class TimelineCompiler {

    /**
     * Compile a project into ordered `HitEvent`s.
     *
     * `internalSampleRate` is the engine's internal rate (48 kHz from
     * `08_AUDIO_ENGINE.md`).
     */
    fun compile(
        project: MBeatProject,
        internalSampleRate: Int = DEFAULT_INTERNAL_SAMPLE_RATE
    ): List<HitEvent> {
        require(project.bpm in MIN_BPM..MAX_BPM) {
            "BPM ${project.bpm} out of bounds [$MIN_BPM, $MAX_BPM]"
        }
        require(project.swing in 0.0..1.0) {
            "Swing ${project.swing} out of bounds [0.0, 1.0]"
        }

        val beatsPerSecond = project.bpm / 60.0
        val samplesPerBeat = (internalSampleRate / beatsPerSecond).toLong()
        val swingOffsetSamples = (samplesPerBeat * project.swing).toLong()

        val out = ArrayList<HitEvent>(project.tracks.sumOf { it.steps.size })

        for (track in project.tracks) {
            for (step in track.steps) {
                require(step >= 0) { "Step index must be non-negative: $step" }
                // Even steps land on the beat; odd steps swing forward by swing%.
                val tick = samplesPerBeat * step +
                    if (step % 2 == 1) swingOffsetSamples else 0L
                out.add(HitEvent(tick = tick, trackId = track.id))
            }
        }

        out.sortBy { it.tick }
        return out
    }

    companion object {
        const val DEFAULT_INTERNAL_SAMPLE_RATE: Int = 48_000
        const val MIN_BPM: Int = 30
        const val MAX_BPM: Int = 300
    }
}
