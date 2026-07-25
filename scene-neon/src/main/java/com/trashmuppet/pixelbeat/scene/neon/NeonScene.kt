package com.trashmuppet.pixelbeat.scene.neon

import com.trashmuppet.pixelbeat.core.model.HitEvent
import com.trashmuppet.pixelbeat.core.model.MBeatProject
import com.trashmuppet.pixelbeat.core.model.Track
import com.trashmuppet.pixelbeat.scene.api.Scene
import com.trashmuppet.pixelbeat.scene.api.SceneId
import com.trashmuppet.pixelbeat.scene.api.SceneRenderState
import kotlin.random.Random

/**
 * Phase-8 Pro-tier "Neon City" scene.
 *
 * The scene cell for each track renders as a **shape specific to the
 * drum kind** (square / diamond / dot / plus / cross / grid) over a
 * seeded sparse grid background. Same `ProjectSeed` ⇒ identical
 * layout pixels.
 *
 * Determinism guarantees (ADR-004):
 *  - Same `ProjectSeed` ⇒ identical layout pixels.
 *  - Same input hit stream ⇒ identical `SceneRenderState` sequence.
 *  - No wall-clock timing anywhere in this module.
 *
 * Layout:
 *  - 32x32 monochrome pixel grid (matches warehouse for swap-compatibility).
 *  - Background: ~12 sparse grid dots per `ProjectSeed`, lit dim — pure-white.
 *  - On hit: a shape draws to the pixel buffer for one scene tick.
 */
class NeonScene : Scene {

    override val id = SceneId(packId = "neon", packVersion = 1, runtimeVersion = 1)

    private var project: MBeatProject? = null
    private var queuedHits: MutableList<HitEvent> = mutableListOf()
    private var tickCounter: Long = 0L
    private var trackShapes: Map<String, TrackHit> = emptyMap()
    private var backgroundPixels: ByteArray = ByteArray(WIDTH * HEIGHT / 8)

    /**
     * Replace the scene state with a fresh project. Background layout
     * and per-track shape anchors are derived deterministically from
     * `project.seed`.
     */
    override fun load(project: MBeatProject) {
        this.project = project
        this.queuedHits = mutableListOf()
        this.tickCounter = 0L

        val rng = Random(project.seed.value)
        backgroundPixels = computeBackground(rng)

        val trackIds = project.patterns
            .flatMap { it.tracks }
            .map { it.id }
            .distinct()

        trackShapes = trackIds.mapIndexed { idx, trackId ->
            val x = (rng.nextInt(WIDTH - 6) + idx) % (WIDTH - 4)
            val y = (rng.nextInt(HEIGHT - 6) + (idx * 3)) % (HEIGHT - 4)
            // Resolve DrumKind (from :core:model) to the private
            // TrackKindFallback enum so the local `toShape()` extension
            // binds correctly. Without the explicit type annotation the
            // elvis unifies `DrumKind?` with `TrackKindFallback` to `Any`,
            // which hides the extension receiver.
            val resolvedTrack = project.patterns
                .flatMap { it.tracks }
                .firstOrNull { it.id == trackId }
            val kind: TrackKindFallback = resolveKindFallback(resolvedTrack)
            val shape = kind.toShape()
            trackId to TrackHit(anchorX = x, anchorY = y, shape = shape)
        }.toMap()
    }

    override fun schedule(hits: List<HitEvent>) {
        queuedHits.addAll(hits)
    }

    override fun step(): SceneRenderState {
        val current = tickCounter++
        if (project == null) return emptyState()

        // Start from the deterministic background grid.
        val pixels = backgroundPixels.copyOf()

        // Light each pending hit's per-kind shape for this single frame.
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

    // ------------------------------------------------------------------
    // Drawing primitives — all 1-bit monochrome, MSB-first, byte-level.
    // ------------------------------------------------------------------

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

    private fun computeBackground(rng: Random): ByteArray {
        val pixels = ByteArray(WIDTH * ROW_BYTES)
        // 12 deterministic background dots.
        repeat(12) {
            val x = rng.nextInt(WIDTH)
            val y = rng.nextInt(HEIGHT)
            setPixel(pixels, x, y)
        }
        return pixels
    }

    private fun emptyState(): SceneRenderState = object : SceneRenderState {
        override val widthPx: Int = WIDTH
        override val heightPx: Int = HEIGHT
        override val pixels: ByteArray = ByteArray(HEIGHT * ROW_BYTES)
        override val rowBytes: Int = ROW_BYTES
        override val tick: Long = 0L
    }

    // ------------------------------------------------------------------
    // Inline shape enum + fallback for Upstream Track kinds we don't know.
    // ------------------------------------------------------------------

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
     * Resolve track-kind enumeration to scene-shape without coupling
     * this module to :core:model's full DrumKind pseudo-enum.
     * Falls back to GRID_3 if DrumKind isn't directly comparable.
     */
    private fun resolveKindFallback(track: Track?): TrackKindFallback {
        val raw = track?.kind?.name?.uppercase() ?: return TrackKindFallback.GENERIC
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
