package com.trashmuppet.pixelbeat.scene.void

import com.trashmuppet.pixelbeat.core.model.HitEvent
import com.trashmuppet.pixelbeat.core.model.MBeatProject
import com.trashmuppet.pixelbeat.core.model.Track
import com.trashmuppet.pixelbeat.scene.api.Scene
import com.trashmuppet.pixelbeat.scene.api.SceneId
import com.trashmuppet.pixelbeat.scene.api.SceneRenderState
import kotlin.random.Random

/**
 * Phase-8 Pro-tier "The Void" scene.
 *
 * Absolute-black field with only the hit shapes drawn for the
 * current frame — emphasises the per-track drum-kind silhouette
 * without any background clutter.
 *
 * Same shape vocabulary as NeonScene so the two Pro packs feel
 * like a coherent visual family: square / diamond / dot / plus /
 * cross / grid keyed off the track's drum kind via
 * [resolveKindFallback].
 *
 * Determinism guarantees (ADR-004):
 *  - Same `ProjectSeed` ⇒ identical anchor positions.
 *  - Same input hit stream ⇒ identical `SceneRenderState` sequence.
 *  - No wall-clock timing anywhere in this module.
 */
class VoidScene : Scene {

    override val id = SceneId(packId = "void", packVersion = 1, runtimeVersion = 1)

    private var project: MBeatProject? = null
    private var queuedHits: MutableList<HitEvent> = mutableListOf()
    private var tickCounter: Long = 0L
    private var trackShapes: Map<String, TrackHit> = emptyMap()

    /**
     * Replace the scene state with a fresh project. Per-track
     * anchors are derived deterministically from `project.seed`.
     * Unlike NeonScene there's no background — the field stays
     * pure black between hits.
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

        trackShapes = trackIds.mapIndexed { idx, trackId ->
            val x = (rng.nextInt(WIDTH - 6) + idx) % (WIDTH - 4)
            val y = (rng.nextInt(HEIGHT - 6) + (idx * 3)) % (HEIGHT - 4)
            val kind = project.patterns
                .flatMap { it.tracks }
                .firstOrNull { it.id == trackId }
                ?.kind
            val fallback = resolveKindFallback(kind)
            trackId to TrackHit(anchorX = x, anchorY = y, shape = fallback.toShape())
        }.toMap()
    }

    override fun schedule(hits: List<HitEvent>) {
        queuedHits.addAll(hits)
    }

    override fun step(): SceneRenderState {
        val current = tickCounter++
        if (project == null) return emptyState()

        // Pure black field — no background dots, only the hit shapes.
        val pixels = ByteArray(HEIGHT * ROW_BYTES)

        for (hit in queuedHits) {
            val lookupId = hit.trackId.substringAfterLast('/')
            val track = trackShapes[lookupId] ?: continue
            drawShape(pixels, track.anchorX + 2, track.anchorY + 2, track.shape)
        }
        queuedHits.clear()

        return object : SceneRenderState {
            override val widthPx: Int = WIDTH
            override val heightPx: Int = HEIGHT
            override val pixels: ByteArray = pixels
            override val rowBytes: Int = ROW_BYTES
            override val tick: Long = current
        }
    }

    private fun setPixel(pixels: ByteArray, x: Int, y: Int) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return
        val byteIdx = y * ROW_BYTES + (x ushr 3)
        val mask = (1 shl (7 - (x and 7))).toByte()
        pixels[byteIdx] = (pixels[byteIdx].toInt() or mask.toInt()).toByte()
    }

    private fun drawShape(pixels: ByteArray, cx: Int, cy: Int, shape: Shape) {
        when (shape) {
            Shape.SQUARE_4 ->
                for (dy in 0 until 4) for (dx in 0 until 4) setPixel(pixels, cx + dx, cy + dy)
            Shape.DIAMOND_5 -> for (dy in -2..2) for (dx in -2..2)
                if (Math.abs(dx) + Math.abs(dy) <= 2) setPixel(pixels, cx + dx, cy + dy)
            Shape.DOT_1 -> setPixel(pixels, cx, cy)
            Shape.PLUS_3 -> {
                setPixel(pixels, cx, cy - 1); setPixel(pixels, cx, cy); setPixel(pixels, cx, cy + 1)
                setPixel(pixels, cx - 1, cy); setPixel(pixels, cx + 1, cy)
            }
            Shape.CROSS_5 -> {
                setPixel(pixels, cx, cy); setPixel(pixels, cx - 2, cy); setPixel(pixels, cx + 2, cy)
                setPixel(pixels, cx, cy - 2); setPixel(pixels, cx, cy + 2)
            }
            Shape.GRID_3 -> for (dy in -1..1) for (dx in -1..1)
                if (dx == 0 || dy == 0) setPixel(pixels, cx + dx, cy + dy)
        }
    }

    private fun emptyState(): SceneRenderState = object : SceneRenderState {
        override val widthPx: Int = WIDTH
        override val heightPx: Int = HEIGHT
        override val pixels: ByteArray = ByteArray(HEIGHT * ROW_BYTES)
        override val rowBytes: Int = ROW_BYTES
        override val tick: Long = 0L
    }

    private enum class Shape { SQUARE_4, DIAMOND_5, DOT_1, PLUS_3, CROSS_5, GRID_3 }
    private enum class TrackKindFallback { GENERIC,
        KICK_LIKE, SNARE_LIKE, HAT_LIKE, TOM_LIKE, CYMBAL_LIKE
    }

    private data class TrackHit(val anchorX: Int, val anchorY: Int, val shape: Shape)

    private fun TrackKindFallback.toShape(): Shape = when (this) {
        TrackKindFallback.KICK_LIKE -> Shape.SQUARE_4
        TrackKindFallback.SNARE_LIKE -> Shape.DIAMOND_5
        TrackKindFallback.HAT_LIKE -> Shape.DOT_1
        TrackKindFallback.TOM_LIKE -> Shape.PLUS_3
        TrackKindFallback.CYMBAL_LIKE -> Shape.CROSS_5
        TrackKindFallback.GENERIC -> Shape.GRID_3
    }

    /**
     * Resolve a track's drum kind to a scene-shape without coupling
     * this module to :core:model's full DrumKind pseudo-enum.
     * Falls back to GRID_3 for unknown kinds.
     */
    private fun resolveKindFallback(trackKind: Any?): TrackKindFallback {
        val raw = trackKind?.toString()?.uppercase() ?: return TrackKindFallback.GENERIC
        return when {
            "KICK" in raw -> TrackKindFallback.KICK_LIKE
            "SNARE" in raw -> TrackKindFallback.SNARE_LIKE
            "HAT" in raw -> TrackKindFallback.HAT_LIKE
            "TOM" in raw -> TrackKindFallback.TOM_LIKE
            "CYMBAL" in raw || "CRASH" in raw -> TrackKindFallback.CYMBAL_LIKE
            else -> TrackKindFallback.GENERIC
        }
    }

    companion object {
        const val WIDTH: Int = 32
        const val HEIGHT: Int = 32
        const val ROW_BYTES: Int = WIDTH / 8
    }
}
