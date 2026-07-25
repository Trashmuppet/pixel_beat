package com.trashmuppet.pixelbeat.scene.warehouse

import com.trashmuppet.pixelbeat.core.model.HitEvent
import com.trashmuppet.pixelbeat.core.model.MBeatProject
import com.trashmuppet.pixelbeat.scene.api.Scene
import com.trashmuppet.pixelbeat.scene.api.SceneId
import com.trashmuppet.pixelbeat.scene.api.SceneRenderState

/**
 * Phase-0 placeholder for the Warehouse scene.
 *
 * Owns a 32×32 step-led simulation that flashes whenever an upcoming
 * HitEvent crosses within the look-ahead window. Phase 3 + 4 replace
 * this with the real animation system per `09_SCENE_SYSTEM.md`.
 *
 * Per `10_RENDERER.md` the scene MUST NOT do wall-clock I/O, MUST NOT
 * allocate during the render path, and MUST NOT mutate musical timing.
 */
class WarehouseScene : Scene {

    override val id = SceneId(
        packId = "warehouse",
        packVersion = 1,
        runtimeVersion = 1
    )

    private var queuedHits: List<HitEvent> = emptyList()

    /** Monotonic, deterministic tick counter incremented once per `step()`. */
    private var tickCounter: Long = 0L

    // Phase 0 simulation: track-0 row lights up on HitEvents within the
    // first 32 ticks; everything else stays black. Pure deterministic.
    override fun load(project: MBeatProject) {
        queuedHits = project.tracks.firstOrNull()?.steps?.map {
            HitEvent(tick = it.toLong(), trackId = project.tracks.first().id)
        } ?: emptyList()
    }

    override fun schedule(hits: List<HitEvent>) {
        queuedHits = (queuedHits + hits).sortedBy { it.tick }
    }

    override fun step(): SceneRenderState {
        val width = 32
        val height = 32
        val rowBytes = width / 8
        val pixels = ByteArray(height * rowBytes)

        // Slot one bit per row. For each queued hit, set the bit at
        // (tick % height). This keeps the simulation deterministic and
        // visually informative during Phase 0.
        for (hit in queuedHits) {
            val row = (hit.tick % height).toInt().coerceIn(0, height - 1)
            val byteIdx = row * rowBytes
            val mask = (1 shl (7 - (row and 7))).toByte()
            pixels[byteIdx] = (pixels[byteIdx].toInt() or mask.toInt()).toByte()
        }

        val currentTick = tickCounter++
        return object : SceneRenderState {
            override val widthPx: Int = width
            override val heightPx: Int = height
            override val pixels: ByteArray = pixels
            override val rowBytes: Int = rowBytes
            override val tick: Long = currentTick
        }
    }
}
