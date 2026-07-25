package com.trashmuppet.pixelbeat.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 1-bit palette. The product is strictly `#000000` / `#FFFFFF`.
 * Anything that needs a third colour is escalated to the design
 * bible — not silently widened here.
 *
 * Per `10_RENDERER.md` we do NOT have alpha / gradients / AA in the
 * rendered surface; the palette guarantees this by offering only two
 * values.
 */
object MonoPalette {
    val Background: Color = Color(0xFF000000)
    val Foreground: Color = Color(0xFFFFFFFF)

    /** Inverse surfaces — used for primary buttons. */
    val Surface: Color = Background
    val OnSurface: Color = Foreground

    val Outline: Color = Foreground
}

/**
 * Standard touch target size. Used everywhere a control accepts a
 * tap. Per `16_UI_BIBLE.md`.
 */
val TapTarget = 48.dp
