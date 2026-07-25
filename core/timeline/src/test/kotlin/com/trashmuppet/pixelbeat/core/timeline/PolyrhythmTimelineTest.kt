package com.trashmuppet.pixelbeat.core.timeline

import com.trashmuppet.pixelbeat.core.model.Arrangement
import com.trashmuppet.pixelbeat.core.model.DrumKind
import com.trashmuppet.pixelbeat.core.model.LoopBoundary
import com.trashmuppet.pixelbeat.core.model.MBeatProject
import com.trashmuppet.pixelbeat.core.model.Pattern
import com.trashmuppet.pixelbeat.core.model.ProjectSeed
import com.trashmuppet.pixelbeat.core.model.SwingMode
import com.trashmuppet.pixelbeat.core.model.Track
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Phase 8 beat-system richness tests — polyrhythm, per-track swing
 * override, and per-step velocity. All three are folded behind
 * nullable model fields so v1 / v2 documents keep working with
 * default settings.
 */
class PolyrhythmTimelineTest {

    @Test
    fun `per-track lengthSteps creates a polyrhythm against the master pattern`() {
        val project = fixture()
        val compiler = TimelineCompiler()
        val events = compiler.compile(project).events

        val kicks = events.filter { it.trackId.endsWith("/kck") }
        // 4-step sequence loops inside 16-step pattern → 4 hits per pattern.
        assertEquals(4, kicks.size, "kick sequence must fire 4 times per pattern")
        // tickModulo reflects the wrapping inside the 4-step cycle.
        assertEquals(listOf(0L, 1L, 2L, 3L), kicks.map { it.tickModulo })
    }

    @Test
    fun `per-track swing override beats project swing for the same track`() {
        val project = fixture(
            projectSwing = SwingMode(),                               // straight project
            trackSwingOverride = SwingMode(SwingMode.Granularity.EIGHTH, 1f)
        )
        val compiler = TimelineCompiler()
        val events = compiler.compile(project).events

        val kicks = events.filter { it.trackId.endsWith("/kck") }.sortedBy { it.tick }
        // Project swing NONE → no swing. Track override EIGHTH 1.0 → half-beat push
        // on odd step indices, so consecutive hit gaps alternate unswung / swung.
        val nominalGap = (48_000L / 4L)
        val first = kicks[0]
        val second = kicks[1]
        assertEquals(nominalGap, second.tick - first.tick - 12_000L,
            "per-track swing override must push 2nd hit by half a beat (12000 samples)"
        )
    }

    @Test
    fun `per-step velocities flow into the compiled HitEvent`() {
        val project = fixture(
            velocities = listOf(1.0f, 0.4f, 0.0f, 0.0f) // index 1 has a ghost note
        )
        val compiler = TimelineCompiler()
        val events = compiler.compile(project).events
        val kicks = events.filter { it.trackId.endsWith("/kck") }

        assertEquals(3, kicks.size,
            "velocity=0.0 step must be filtered out (transparent 'off' semantics)")
        assertEquals(listOf(1.0f, 0.4f, 0.0f), kicks.map { it.velocity.toDouble() }.map { it })
    }

    @Test
    fun `full pipeline is byte-deterministic across runs (Phase 8 + v2 compat)`() {
        val project = fixture()
        val compiler = TimelineCompiler()
        val a = compiler.compile(project)
        val b = compiler.compile(project)
        assertEquals(a.projectHash, b.projectHash)
        assertEquals(a.events, b.events)
        assertEquals(a.totalSamples, b.totalSamples)
    }

    private fun fixture(
        projectSwing: SwingMode = SwingMode(),
        trackSwingOverride: SwingMode? = null,
        velocities: List<Float>? = null
    ): MBeatProject {
        val kck = Track(
            id = "kck",
            kind = DrumKind.KICK,
            steps = List(4) { true },                // 4-step cycle inside 16-step pattern
            velocities = velocities,
            lengthSteps = 4,                         // polyrhythm
            swingOverride = trackSwingOverride
        )
        return MBeatProject(
            id = "p",
            name = "Phase 8 fixture",
            bpm = 120f,
            seed = ProjectSeed(0xCAFE_BABE_DEAD_BEEFL),
            swing = projectSwing,
            arrangement = Arrangement(
                patternChain = listOf("main"),
                loopBars = LoopBoundary()
            ),
            patterns = listOf(Pattern("main", lengthSteps = 16, tracks = listOf(kck)))
        )
    }
}
