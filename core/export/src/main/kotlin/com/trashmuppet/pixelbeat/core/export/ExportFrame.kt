package com.trashmuppet.pixelbeat.core.export

/**
 * Opaque 1-bit framebuffer snapshot for the offline encoder pipeline.
 *
 * Mirrors `SceneRenderState` (`scene-api`) but lives in `:core:export`
 * so the encoder surface doesn't pull UI/Compose dependencies. Same
 * convention as in there:
 *
 *  - `pixels`: row-major, MSB-first within each byte.
 *  - 1 = white (#FFFFFF), 0 = black (#000000).
 *  - `rowBytes = (widthPx + 7) / 8` (integer, never partial bits).
 *
 * Per `10_RENDERER.md` we never extend this representation with alpha,
 * gradients, or anti-aliasing — Monochrome Beat stays 1-bit.
 */
data class ExportFrame(
    val widthPx: Int,
    val heightPx: Int,
    val rowBytes: Int,
    val pixels: ByteArray,
    val tick: Long
) {
    init {
        require(widthPx > 0) { "widthPx must be positive" }
        require(heightPx > 0) { "heightPx must be positive" }
        require(rowBytes == (widthPx + 7) / 8) {
            "rowBytes=$rowBytes does not match widthPx=$widthPx (expected ${(widthPx + 7) / 8})"
        }
        require(pixels.size == heightPx * rowBytes) {
            "pixels.size=${pixels.size} != heightPx*rowBytes=${heightPx * rowBytes}"
        }
        require(tick >= 0L) { "tick must be non-negative" }
    }

    companion object {
        /** Helper that builds an always-black frame. */
        fun blank(widthPx: Int, heightPx: Int, tick: Long = 0L): ExportFrame {
            val rowBytes = (widthPx + 7) / 8
            return ExportFrame(
                widthPx = widthPx,
                heightPx = heightPx,
                rowBytes = rowBytes,
                pixels = ByteArray(heightPx * rowBytes),
                tick = tick
            )
        }
    }
}
