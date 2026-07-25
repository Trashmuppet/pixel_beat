package com.trashmuppet.pixelbeat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 1-bit palette: only #000000 (background) and #FFFFFF (foreground).
 *
 * Per `10_RENDERER.md` there is no alpha, no gradient, no anti-aliasing.
 * Material 3 controls inherit our monochrome palette; accent is reserved
 * strictly for system status feedback only (`16_UI_BIBLE.md`).
 */
private val Black = Color(0xFF000000)
private val White = Color(0xFFFFFFFF)

private val MonochromeColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    secondary = White,
    onSecondary = Black,
    background = Black,
    onBackground = White,
    surface = Black,
    onSurface = White,
    error = White,
    onError = Black,
    outline = White,
    surfaceVariant = Black,
    onSurfaceVariant = White
)

// Light scheme is structurally identical — the product is monotone by
// definition. Kept here so the `darkTheme` toggle below still composes.
private val MonochromeColorSchemeLight = lightColorScheme(
    primary = Black,
    onPrimary = White,
    secondary = Black,
    onSecondary = White,
    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    error = Black,
    onError = White,
    outline = Black,
    surfaceVariant = White,
    onSurfaceVariant = Black
)

@Composable
fun MonochromeBeatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MonochromeColorScheme else MonochromeColorSchemeLight
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MonochromeBeatTypography,
        content = content
    )
}
