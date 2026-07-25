package com.trashmuppet.pixelbeat.scene.api

/**
 * Opaque, immutable frame snapshot produced by a `Scene` and consumed
 * (read-only) by `SceneRenderer`.
 *
 * `10_RENDERER.md` states: "Renderer never changes simulation state.
 * Reads SceneRenderState only." Implementations MUST be immutable or
 * produced from a frozen projection; the renderer must never mutate
 * them.
 *
 * The default integer-raster representation matches the 1-bit visual
 * language: only #000000 and #FFFFFF, integer scaling, no AA.
 */
interface SceneRenderState {
    /** Width in pixels at native 1-bit resolution. */
    val widthPx: Int

    /** Height in pixels at native 1-bit resolution. */
    val heightPx: Int

    /**
     * Monochrome pixel snapshot.
     *
     * `pixels[i]` is the linearity-pack bitmap byte for row `i / rowBytes`,
     * column `i % rowBytes * 8 + bit`. Convention: 1 = white (#FFFFFF),
     * 0 = black (#000000). Length = `heightPx * rowBytes`.
     */
    val pixels: ByteArray

    /** Bytes per pixel row. */
    val rowBytes: Int

    /** Simulation tick index (frames elapsed since scene start). */
    val tick: Long

    companion object {
        fun empty(widthPx: Int = 0, heightPx: Int = 0): SceneRenderState =
            EmptySceneRenderState(widthPx, heightPx)
    }
}

private class EmptySceneRenderState(
    override val widthPx: Int,
    override val heightPx: Int
) : SceneRenderState {
    override val pixels: ByteArray = ByteArray(0)
    override val rowBytes: Int = 0
    override val tick: Long = 0L
}
