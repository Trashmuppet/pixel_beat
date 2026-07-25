package com.trashmuppet.pixelbeat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.trashmuppet.pixelbeat.ui.navigation.AppNavHost
import com.trashmuppet.pixelbeat.ui.theme.MonochromeBeatTheme

/**
 * Sole activity hosting the Compose graph.
 *
 * Sets edge-to-edge true-black system bars to honour the 1-bit visual
 * language documented in `10_RENDERER.md` and `16_UI_BIBLE.md`.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MonochromeBeatTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    color = Color.Black
                ) {
                    AppNavHost()
                }
            }
        }
    }
}
