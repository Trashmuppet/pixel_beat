package com.trashmuppet.pixelbeat.core.timeline

import com.trashmuppet.pixelbeat.core.model.HitEvent
import com.trashmuppet.pixelbeat.core.model.MBeatProject
import com.trashmuppet.pixelbeat.core.model.Pattern
import com.trashmuppet.pixelbeat.core.model.SwingMode
import com.trashmuppet.pixelbeat.core.model.Track
import kotlin.math.roundToLong

/**
 * Compiles a frozen `MBeatProject` into an immutable, ordered
 * `CompiledTimeline`. Both real-time transport (Phase 2) and offline
 * render (Phase 5) consume this same output — which is the whole
 * point of `07_TIMELINE_ENGINE.md` ("Export-first").
 *
 * Properties:
 *  - **Deterministic.** Same input → identical bytes — verified by
 *    golden-fixture tests in `src/test`.
 *  - **Sample-accurate.** Every event has a `tick: Long` at the
 *    audio engine's internal sample rate (48 kHz).
 *  - **No wall-clock timing.** Pure function of inputs.
 *  - **Stable event ordering.** Sort order is `(tick, trackId)` so a
 *    renderer can iterate in playback order without reordering.
 *
 * @see RealtimeTransport
 * @see OfflineRenderSession
 */
class TimelineCompiler {

    /**
     * Compile `project` to a `CompiledTimeline`.
     *
     * @param internalSampleRate sample rate of the downstream audio
     *   engine. Defaults to 48 kHz per `08_AUDIO_ENGINE.md`.
     */
    fun compile(
        project: MBeatProject,
        internalSampleRate: Int = DEFAULT_SAMPLE_RATE
    ): CompiledTimeline {
        require(internalSampleRate > 0) { "sampleRate must be positive" }
        val beatsPerMinute = project.bpm.toDouble()
        require(beatsPerMinute > 0.0) { "project.bpm must be positive" }

        val samplesPerBeat = (internalSampleRate * 60.0 / beatsPerMinute).roundToLong()
        require(samplesPerBeat > 0) { "samplesPerBeat $samplesPerBeat too small" }

        val patternsById = project.patterns.associateBy { it.id }
        val out = ArrayList<HitEvent>(1024)
        var patternStartBar = 0

        val chain = if (project.arrangement.loopBars.isLooping) {
            // Loop: emit `patternChain` for the loop range, end-exclusive.
            val firstBar = project.arrangement.loopBars.startBar
            val lastBarExclusive = project.arrangement.loopBars.endBar
            val loopBarCount = (lastBarExclusive - firstBar).coerceAtLeast(1)
            repeat(loopBarCount) { barIndexWithinLoop ->
                val barIndex = firstBar + barIndexWithinLoop
                val patternId = project.arrangement.patternChain[barIndexWithinLoop % project.arrangement.patternChain.size]
                compilePattern(
                    projectId = project.id,
                    pattern = patternsById.getValue(patternId),
                    barStartSample = sampleOffsetForBar(barIndex, samplesPerBeat),
                    barStep = 0,
                    samplesPerBeat = samplesPerBeat,
                    swing = project.swing,
                    out = out
                )
            }
            patternStartBar = lastBarExclusive
        } else {
            patternStartBar = 0
            emptyList<Int>()
        }

        // After optional loop: emit patternChain once in order, in full,
        // each contributing its `lengthSteps / 4` bars worth of beats.
        for ((barIndexAbsolute, patternId) in project.arrangement.patternChain.withIndex()) {
            val pattern = patternsById.getValue(patternId)
            val barStartSample = sampleOffsetForBar(
                startBar = patternStartBar + barIndexAbsolute,
                samplesPerBeat = samplesPerBeat
            )
            compilePattern(
                projectId = project.id,
                pattern = pattern,
                barStartSample = barStartSample,
                barStep = 0,
                samplesPerBeat = samplesPerBeat,
                swing = project.swing,
                out = out
            )
        }

        // Stable secondary sort by trackId keeps render ordering reproducible.
        out.sortWith(compareBy({ it.tick }, { it.trackId }))

        val timelineEnd = computeTimelineEndSample(
            project = project,
            patternsById = patternsById,
            samplesPerBeat = samplesPerBeat,
            startBar = patternStartBar
        )

        return CompiledTimeline(
            project = project,
            events = out,
            totalSamples = timelineEnd,
            internalSampleRate = internalSampleRate,
            projectHash = ProjectHasher.contentHashOf(project)
        )
    }

    /**
     * Compile every track of a pattern into `out`, applying swing and
     * the `steps[]` boolean array. Exposed `internal` so tests can
     * spot-check granularity without spinning a full timeline.
     */
    internal fun compilePattern(
        projectId: String,
        pattern: Pattern,
        barStartSample: Long,
        @Suppress("UNUSED_PARAMETER") barStep: Int, // reserved for future beat-time steps vs steps
        samplesPerBeat: Long,
        swing: SwingMode,
        out: MutableList<HitEvent>
    ) {
        for (track in pattern.tracks) {
            if (track.mute) continue
            compileTrack(
                projectId = projectId,
                track = track,
                patternId = pattern.id,
                barStartSample = barStartSample,
                patternLengthSteps = pattern.lengthSteps,
                samplesPerBeat = samplesPerBeat,
                swing = swing,
                out = out
            )
        }
    }

    private fun compileTrack(
        projectId: String,
        track: Track,
        patternId: String,
        barStartSample: Long,
        patternLengthSteps: Int,
        samplesPerBeat: Long,
        swing: SwingMode,
        out: MutableList<HitEvent>
    ) {
        // Phase 8: per-track swing override → fall back to project swing.
        val effectiveSwing = track.swingOverride ?: swing
        val stepsPerBeat = when (effectiveSwing.granularity) {
            SwingMode.Granularity.NONE -> 4             // 16th = 4 / beat
            SwingMode.Granularity.EIGHTH -> 2           // 8th
            SwingMode.Granularity.SIXTEENTH -> 1       // 16th
        }
        val swingFactor = effectiveSwing.amount.coerceIn(0f, 1f)
        val swingOffsetInSamples = (samplesPerBeat.toDouble() * 0.5 * swingFactor).roundToLong()

        // Phase 8: polyrhythm — wrap the track sequence to a per-track cycle size.
        // When track.lengthSteps is null, inherits the pattern length so the loop
        // counts `lengthSteps` from the start of each pattern, identical to v1.
        val cycleSize = track.lengthSteps ?: patternLengthSteps

        for (stepIndex in 0 until patternLengthSteps) {
            val trackStepIndex = stepIndex % cycleSize
            // Defensive wrap in case cycleSize > steps.size (legacy import where
            // lengthSteps was bumped but the steps list was left at the old size).
            val safeIndex = trackStepIndex % track.steps.size
            val active = track.steps[safeIndex]
            if (!active) continue

            val velocity = track.velocities?.getOrNull(safeIndex) ?: 1.0f

            val nominalStep = barStartSample + (samplesPerBeat * stepIndex / stepsPerBeat)
            val swingApplied = if (stepIndex % 2 == 1 && effectiveSwing.granularity != SwingMode.Granularity.NONE) {
                nominalStep + swingOffsetInSamples
            } else {
                nominalStep
            }

            out += HitEvent(
                tick = swingApplied,
                trackId = "$projectId/$patternId/${track.id}",
                velocity = velocity,
                tickModulo = trackStepIndex.toLong()
            )
        }
    }

    private fun sampleOffsetForBar(startBar: Int, samplesPerBeat: Long): Long =
        startBar.toLong() * samplesPerBeat * BEATS_PER_BAR

    private fun computeTimelineEndSample(
        project: MBeatProject,
        patternsById: Map<String, Pattern>,
        samplesPerBeat: Long,
        startBar: Int
    ): Long {
        var totalBars = startBar
        for (patternId in project.arrangement.patternChain) {
            totalBars += patternsById.getValue(patternId).lengthSteps / STEPS_PER_BEAT
        }
        return totalBars.toLong() * samplesPerBeat * BEATS_PER_BAR
    }

    companion object {
        const val DEFAULT_SAMPLE_RATE: Int = 48_000
        const val BEATS_PER_BAR: Long = 4L
        const val STEPS_PER_BEAT: Int = 4
    }
}
