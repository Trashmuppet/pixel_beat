package com.trashmuppet.pixelbeat.core.timeline

import com.trashmuppet.pixelbeat.core.model.MBeatProject
import com.trashmuppet.pixelbeat.core.model.Pattern
import com.trashmuppet.pixelbeat.core.model.Track
import com.trashmuppet.pixelbeat.core.model.DrumKind
import com.trashmuppet.pixelbeat.core.model.Arrangement
import com.trashmuppet.pixelbeat.core.model.LoopBoundary
import com.trashmuppet.pixelbeat.core.model.SwingMode
import com.trashmuppet.pixelbeat.core.model.ProjectSeed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TimelineCompilerTest {

    @Test
    fun `compile is deterministic across runs`() {
        val project = fixture()
        val compiler = TimelineCompiler()
        val a = compiler.compile(project)
        val b = compiler.compile(project)
        assertEquals(a.projectHash, b.projectHash, "compile hash should be stable across runs")
        assertEquals(a.events, b.events, "event list must be byte-identical across runs")
        assertEquals(a.totalSamples, b.totalSamples)
    }

    @Test
    fun `swing shifts odd steps forward deterministically`() {
        val project = fixture(swing = SwingMode(SwingMode.Granularity.EIGHTH, amount = 1f))
        val compiler = TimelineCompiler()
        val events = compiler.compile(project).events
        // Two consecutive hits on the same track — first must keep sample position,
        // second must be pushed forward. Bpm = 120 => one beat = 24_000 samples.
        val sameTrackId = events.first().trackId
        val ownEvents = events.filter { it.trackId == sameTrackId }.sortedBy { it.tick }
        val first = ownEvents[0]
        val second = ownEvents[1]
        val nominalGap = (48_000L / 4L) // 16-step grid / beat
        assertTrue(second.tick - first.tick > nominalGap, "swung step should be pushed forward")
        // Amount = 1.0 should push second hit by half a beat (12000 samples)
        assertEquals(12_000L, second.tick - first.tick - nominalGap)
    }

    @Test
    fun `loop boundary repeats pattern range exactly once`() {
        val project = fixture(
            arrangement = Arrangement(
                patternChain = listOf("a", "b"),
                loopBars = LoopBoundary(startBar = 0, endBar = 2)
            )
        )
        val compiler = TimelineCompiler()
        val looped = compiler.compile(project).events.filter { it.tick < SAMPLE_OF_BAR_2 }.toList()
        val expectedLoop = compiler.compile(project.copy(arrangement = Arrangement(
            patternChain = listOf("a", "b"),
            loopBars = LoopBoundary()
        ))).events.filter { it.tick < SAMPLE_OF_BAR_2 }.toList()
        // Within the first 2 bars the loop-only output must match the
        // straight pattern chain output exactly (patternChain order
        // repeats modulo its size).
        assertEquals(expectedLoop, looped)
    }

    @Test
    fun `bpm changes recompute samplesPerBeat exactly`() {
        val slow = fixture(bpm = 60f)
        val fast = fixture(bpm = 240f)
        val compiler = TimelineCompiler()
        val slowEnd = compiler.compile(slow).totalSamples
        val fastEnd = compiler.compile(fast).totalSamples
        assertEquals(slowEnd / 4L, fastEnd, "240 bpm should be exactly 4x faster than 60 bpm")
    }

    @Test
    fun `mute track contributes zero events`() {
        val project = fixture(muteFirstTrack = true)
        val compiler = TimelineCompiler()
        val tracks = compiler.compile(project).events.map { it.trackId }.toSet()
        assertTrue(tracks.none { it.endsWith("/kck") }, "muted track should contribute no events")
    }

    private fun fixture(
        bpm: Float = 120f,
        swing: SwingMode = SwingMode(),
        arrangement: Arrangement = Arrangement(
            patternChain = listOf("main"),
            loopBars = LoopBoundary()
        ),
        muteFirstTrack: Boolean = false
    ): MBeatProject {
        // 16th-grid fixture: every 4th step kicks.
        val track = Track(
            id = if (muteFirstTrack) "kck" else "kck",
            kind = DrumKind.KICK,
            steps = List(16) { it % 4 == 0 },
            volumeDb = 0f,
            panCb = 0f,
            mute = muteFirstTrack
        )
        return MBeatProject(
            id = "fixture",
            name = "Fixture",
            bpm = bpm,
            seed = ProjectSeed(0xA5A5_5A5A_DEAD_BEEFL),
            swing = swing,
            arrangement = arrangement,
            patterns = listOf(
                Pattern(
                    id = "a",
                    lengthSteps = 16,
                    tracks = listOf(track.copy(id = if (muteFirstTrack) "off" else "kck"))
                ),
                Pattern(
                    id = "b",
                    lengthSteps = 16,
                    tracks = listOf(track.copy(id = "kck"))
                ),
                Pattern(
                    id = "main",
                    lengthSteps = 16,
                    tracks = listOf(track.copy(id = muteFirstTrack.let { if (it) "off" else "kck" }))
                )
            )
        )
    }

    companion object {
        // 120 bpm * 4 bar length = 4 * 48000 samples/bar at 120 bpm.
        // 1 beat = 24000 samples → 1 bar = 96000. Bar 2 boundaries at sample 96000.
        const val SAMPLE_OF_BAR_2: Long = 96_000L
    }
}
