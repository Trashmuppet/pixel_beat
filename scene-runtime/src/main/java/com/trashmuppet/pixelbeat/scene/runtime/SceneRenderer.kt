package com.trashmuppet.pixelbeat.scene.runtime

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.trashmuppet.pixelbeat.scene.api.SceneRenderState

/**
 * Stateless renderer that paints a `SceneRenderState` onto Compose
 * `Canvas` honouring the 1-bit visual language documented in
 * `10_RENDERER.md`:
 *  - Only #000000 / #FFFFFF, no alpha, no gradient, no AA.
 *  - Integer pixel scaling (`FilterQuality.None`, integer translation).
 *  - Read-only access to the snapshot — never mutates simulation state.
 */
@Composable
fun ScenePreview(
    state: SceneRenderState,
    modifier: Modifier = Modifier
) {
    val bitmap: ImageBitmap = remember(state) { state.toImageBitmap() }

    Canvas(modifier = modifier) {
        val scaleX = size.width.toInt() / state.widthPx
        val scaleY = size.height.toInt() / state.heightPx
        val scale = maxOf(1, minOf(scaleX, scaleY)) // integer-only scaling
        val drawnW = state.widthPx * scale
        val drawnH = state.heightPx * scale
        // Centre the raster within the canvas without sub-pixel offsets.
        val dx = ((size.width - drawnW) / 2f).toInt()
        val dy = ((size.height - drawnH) / 2f).toInt()

        drawImage(
            image = bitmap,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(state.widthPx, state.heightPx),
            dstOffset = IntOffset(dx, dy),
            dstSize = IntSize(drawnW, drawnH),
            filterQuality = FilterQuality.None
        )
    }
}

/**
 * Decode the monochrome 1-bit framebuffer into an ARGB `ImageBitmap` so
 * Compose can `drawImage` it. Convention: 1 = #FFFFFF, 0 = #000000,
 * MSB-first within each row byte.
 */
private fun SceneRenderState.toImageBitmap(): ImageBitmap {
    val argb = IntArray(widthPx * heightPx)
    for (y in 0 until heightPx) {
        for (x in 0 until widthPx) {
            val byte = pixels[y * rowBytes + (x ushr 3)]
            val bit = (byte.toInt() ushr (7 - (x and 7))) and 1
            argb[y * widthPx + x] =
                if (bit == 1) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        }
    }
    val bmp = android.graphics.Bitmap.createBitmap(
        widthPx,
        heightPx,
        android.graphics.Bitmap.Config.ARGB_8888
    )
    bmp.setPixels(argb, 0, widthPx, 0, 0, widthPx, heightPx)
    return bmp.asImageBitmap()
}
