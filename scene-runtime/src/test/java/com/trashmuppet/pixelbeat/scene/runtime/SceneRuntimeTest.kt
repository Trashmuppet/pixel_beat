package com.trashmuppet.pixelbeat.scene.runtime

import com.trashmuppet.pixelbeat.core.model.Arrangement
import com.trashmuppet.pixelbeat.core.model.DrumKind
import com.trashmuppet.pixelbeat.core.model.HitEvent
import com.trashmuppet.pixelbeat.core.model.LoopBoundary
import com.trashmuppet.pixelbeat.core.model.MBeatProject
import com.trashmuppet.pixelbeat.core.model.Pattern
import com.trashmuppet.pixelbeat.core.model.ProjectSeed
import com.trashmuppet.pixelbeat.core.model.SwingMode
import com.trashmuppet.pixelbeat.core.model.Track
import com.trashmuppet.pixelbeat.scene.warehouse.WarehouseScene
import com.trashmuppet.pixelbeat.scene.neon.NeonScene
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.security.MessageDigest

/**
 * Golden-fixture determinism tests for `:scene-runtime`.
 *
 * Phase 4 rule: identical inputs on identical seeds MUST yield byte
 * identical `SceneRenderState`. Verifies this for both the live
 * `WarehouseScene` (`09_SCENE_SYSTEM.md` reference impl) and the
 * `AnimationSystem` driver.
 */
class SceneRuntimeTest {

    @Test
    fun `seed-driven layout yields identical pixels across runs`() {
        val project = fixture()
        val stateA = render(project)
        val stateB = render(project)
        assertArrayEquals(
            stateA.pixels,
            stateB.pixels,
            "two equal runs must produce identical pixels"
        )
        assertEquals(stateA.tick, stateB.tick, "tick counter must match across runs")
    }

    @Test
    fun `240 Hz advance consumes exactly 240 ticks for 1 second`() {
        val project = fixture()
        val scene = WarehouseScene().apply { load(project) }
        val sim = AnimationSystem(scene)
        sim.advance(audioFrames = 48_000, audioSampleRate = 48_000)
        assertEquals(240L, sim.currentTick(), "1 second of audio at 48kHz = 240 scene ticks at 240Hz")
        assertEquals(0, sim.pendingCount(), "all pending hits should have been delivered by 1s mark")
    }

    @Test
    fun `hits injected at sample-aligned ticks light their cells`() {
        val project = fixture()
        val scene = WarehouseScene().apply { load(project) }
        val sim = AnimationSystem(scene)
        // Hit at sample-200 (= scene tick 1) → kick lands on its cell.
        sim.enqueueHit(HitEvent(tick = 200, trackId = "blank/kick"))
        sim.advance(audioFrames = 200, audioSampleRate = 48_000)
        val state = sim.advance(audioFrames = 200, audioSampleRate = 48_000)
        // At minimum, the framebuffer must contain at least one lit pixel.
        val anyWhitePixel = state.pixels.any { byte -> (0..7).any { bit -> ((byte.toInt() ushr (7 - bit)) and 1) == 1 } }
        assertTrue(anyWhitePixel, "after a kick hit, warehouse scene should show at least one lit pixel")
    }

    @Test
    fun `sha256 of fixture matches golden file`() {
        val project = fixture()
        val state = render(project)
        val actual = sha256Hex(state.pixels)
        // Resolve golden resource from the test classpath.
        val resource = javaClass.classLoader.getResource("golden/warehouse-1sec-sha256.txt")
            ?: error("Golden file missing — run sha256_of_fixture_is_stable test first on a real device")
        val expected = File(resource.toURI()).readText().lines()
            .firstOrNull { it.isNotBlank() && !it.startsWith("#") }
            ?.trim() ?: error("Golden file has no hash value")
        if (expected == "REPLACE_WITH_CI_HASH") {
            println("golden-warehouse-1sec-sha256=$actual")
            println("Replace REPLACE_WITH_CI_HASH in golden/warehouse-1sec-sha256.txt with the value above.")
            return // Skip assertion — CI run hasn't populated the golden yet.
        }
        assertEquals(expected, actual, "Golden SHA-256 mismatch — CI hardware has changed")
    }

    @Test
    fun `sha256 of fixture is stable across runs`() {
        val project = fixture()
        val stateA = render(project)
        val stateB = render(project)
        val shaA = sha256Hex(stateA.pixels)
        val shaB = sha256Hex(stateB.pixels)
        assertEquals(shaA, shaB, "byte hash must be deterministic across runs")
        // Anchor: dump a reference hash so a real CI run can persist it as a golden
        // file under src/test/resources/golden/ via a follow-up commit.
        println("golden-warehouse-1sec-sha256=$shaA")
    }

    private fun render(project: MBeatProject) =
        WarehouseScene().apply { load(project) }.let { scene ->
            val sim = AnimationSystem(scene)
            // Three hits per active cell, sample-aligned to scene ticks for exact lighting.
            sim.enqueueHit(HitEvent(tick = 200,   trackId = "blank/kick"))
            sim.enqueueHit(HitEvent(tick = 400,   trackId = "blank/snare"))
            sim.enqueueHit(HitEvent(tick = 600,   trackId = "blank/hat"))
            sim.enqueueHit(HitEvent(tick = 800,   trackId = "blank/kick"))
            sim.advance(audioFrames = 1000, audioSampleRate = 48_000)
        }

    @Test
    fun `NeonScene is byte-deterministic across runs`() {
        val project = fixture()
        val stateA = runNeonOnce(project)
        val stateB = runNeonOnce(project)
        assertArrayEquals(
            stateA.pixels, stateB.pixels,
            "Two equal NeonScene runs must produce identical pixel arrays"
        )
        assertEquals(stateA.tick, stateB.tick, "tick counters must match")
        assertTrue(
            stateA.pixels.any { byte -> (0..7).any { ((byte.toInt() ushr (7 - it)) and 1) == 1 } },
            "NeonScene background or hit shapes should render at least one lit pixel"
        )
    }

    private fun runNeonOnce(project: MBeatProject): SceneRenderState =
        NeonScene().apply { load(project) }.let { scene ->
            val sim = AnimationSystem(scene)
            sim.enqueueHit(HitEvent(tick = 200, trackId = "blank/kick"))
            sim.enqueueHit(HitEvent(tick = 400, trackId = "blank/snare"))
            sim.enqueueHit(HitEvent(tick = 600, trackId = "blank/hat"))
            sim.advance(audioFrames = 1000, audioSampleRate = 48_000)
        }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun fixture(): MBeatProject {
        val tracks = listOf(
            Track(
                id = "kick",
                kind = DrumKind.KICK,
                steps = List(16) { it % 4 == 0 },
                volumeDb = 0f, panCb = 0f, mute = false
            ),
            Track(
                id = "snare",
                kind = DrumKind.SNARE,
                steps = List(16) { it % 8 == 4 },
                volumeDb = 0f, panCb = 0f, mute = false
            ),
            Track(
                id = "hat",
                kind = DrumKind.CLOSED_HAT,
                steps = List(16) { true },
                volumeDb = -3f, panCb = 0f, mute = false
            )
        )
        return MBeatProject(
            id = "blank",
            name = "Fixture",
            bpm = 120f,
            seed = ProjectSeed(0xC0FFEE_BEEFCAFEL),
            swing = SwingMode(),
            arrangement = Arrangement(
                patternChain = listOf("main"),
                loopBars = LoopBoundary()
            ),
            patterns = listOf(Pattern(id = "main", lengthSteps = 16, tracks = tracks))
        )
    }
}
