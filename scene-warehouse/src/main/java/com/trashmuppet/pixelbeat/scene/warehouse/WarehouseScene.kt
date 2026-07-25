package com.trashmuppet.pixelbeat.scene.warehouse

import com.trashmuppet.pixelbeat.core.model.HitEvent
import com.trashmuppet.pixelbeat.core.model.MBeatProject
import com.trashmuppet.pixelbeat.core.model.ProjectSeed
import com.trashmuppet.pixelbeat.scene.api.Scene
import com.trashmuppet.pixelbeat.scene.api.SceneId
import com.trashmuppet.pixelbeat.scene.api.SceneRenderState
import kotlin.random.Random

/**
 * Phase-4 Warehouse scene.
 *
 * Uses `ProjectSeed` (`09_SCENE_SYSTEM.md`) to derive a deterministic
 * per-track cell layout. Each track has its own anchor cell of a
 * fixed 4×4 size — when a hit arrives for that track during
 * `AnimationSystem.step`, the cell lights up for one simulation
 * frame.
 *
 * Determinism guarantees:
 *  - Same `ProjectSeed` ⇒ identical layout pixels.
 *  - Same input hit stream ⇒ identical `SceneRenderState` sequence.
 *  - No wall-clock timing anywhere in this module.
 *
 * The scene is the reference implementation of `Scene` for Phase 4.
 * Future `.mbscene` packs can replace this class without touching
 * `:scene-runtime` or `:app`.
 */
class WarehouseScene : Scene {

    override val id = SceneId(packId = "warehouse", packVersion = 2, runtimeVersion = 1)

    private var project: MBeatProject? = null
    private var queuedHits: MutableList<HitEvent> = mutableListOf()
    private var tickCounter: Long = 0L
    private var cellLayouts: Map<String, CellLayout> = emptyMap()

    /**
     * Replace the scene state with a fresh project. Layout cells are
     * derived deterministically from `project.seed` keyed by `trackId`.
     */
    override fun load(project: MBeatProject) {
        this.project = project
        this.queuedHits = mutableListOf()
        this.tickCounter = 0L
        val rng = Random(project.seed.value)
        val trackIds = project.patterns
            .flatMap { it.tracks }
            .map { it.id }
            .distinct()
        val kMax = WIDTH / 4 - 1
        cellLayouts = trackIds.mapIndexed { idx, trackId ->
            val x = (rng.nextInt(kMax) + idx).coerceIn(0, WIDTH - 4)
            val y = (rng.nextInt(kMax) + (idx * 3)).coerceIn(0, HEIGHT - 4)
            trackId to CellLayout(x = x, y = y, w = 4, h = 4)
        }.toMap()
    }

    override fun schedule(hits: List<HitEvent>) {
        queuedHits.addAll(hits)
    }

    override fun step(): SceneRenderState {
        val projects = project ?: return emptyState()
        val pixels = ByteArray(HEIGHT * ROW_BYTES)

        // Light each track's cell for any queued hit.
        val trackIdMinLength = "x/x/".length
        for (hit in queuedHits) {
            val trackId = if (hit.trackId.length > trackIdMinLength) {
                hit.trackId.substringAfterLast('/')
            } else hit.trackId
            val layout = cellLayouts[trackId] ?: continue
            setRect(pixels, layout)
        }
        // Lights are 1-frame only — re-render each step uses fresh queue.
        queuedHits.clear()

        // Frame counter for downstream SceneRenderer consumers.
        val currentTick = tickCounter++

        return object : SceneRenderState {
            override val widthPx: Int = WIDTH
            override val heightPx: Int = HEIGHT
            override val pixels: ByteArray = pixels
            override val rowBytes: Int = ROW_BYTES
            override val tick: Long = currentTick
        }
    }

    private fun setRect(pixels: ByteArray, rect: CellLayout) {
        val yEnd = (rect.y + rect.h).coerceAtMost(HEIGHT)
        val xEnd = (rect.x + rect.w).coerceAtMost(WIDTH)
        for (yy in rect.y until yEnd) {
            for (xx in rect.x until xEnd) {
                val byteIdx = yy * ROW_BYTES + (xx ushr 3)
                val mask = (1 shl (7 - (xx and 7))).toByte()
                pixels[byteIdx] = (pixels[byteIdx].toInt() or mask.toInt()).toByte()
            }
        }
    }

    private fun emptyState(): SceneRenderState = object : SceneRenderState {
        override val widthPx: Int = WIDTH
        override val heightPx: Int = HEIGHT
        override val pixels: ByteArray = ByteArray(HEIGHT * ROW_BYTES)
        override val rowBytes: Int = ROW_BYTES
        override val tick: Long = 0L
    }

    private data class CellLayout(val x: Int, val y: Int, val w: Int, val h: Int)

    companion object {
        const val WIDTH: Int = 32
        const val HEIGHT: Int = 32
        const val ROW_BYTES: Int = WIDTH / 8

        /**
         * Convenience constructor for callers that just want a seeded
         * blank scene (e.g. unit tests).
         */
        fun blank(seed: ProjectSeed = ProjectSeed(1234L)): WarehouseScene {
            val scene = WarehouseScene()
            scene.load(
                MBeatProject(
                    id = "blank",
                    name = "Blank",
                    bpm = 120f,
                    seed = seed,
                    swing = com.trashmuppet.pixelbeat.core.model.SwingMode(),
                    arrangement = com.trashmuppet.pixelbeat.core.model.Arrangement(
                        patternChain = listOf("main"),
                        loopBars = com.trashmuppet.pixelbeat.core.model.LoopBoundary()
                    ),
                    patterns = listOf(
                        com.trashmuppet.pixelbeat.core.model.Pattern(
                            id = "main",
                            lengthSteps = 16,
                            tracks = emptyList()
                        )
                    )
                )
            )
            return scene
        }
    }
}
