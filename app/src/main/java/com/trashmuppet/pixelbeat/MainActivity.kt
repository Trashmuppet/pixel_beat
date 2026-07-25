package com.trashmuppet.pixelbeat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.metrics.performance.JankStats
import com.trashmuppet.pixelbeat.ui.navigation.AppNavHost
import com.trashmuppet.pixelbeat.ui.theme.MonochromeBeatTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.util.Locale

/**
 * Sole activity hosting the Compose graph.
 *
 * Builds `AppDependencies` once on the activity lifecycle and shares
 * them with the navigation graph. Phase 8 wires a `JankStats` HUD in
 * debug builds so engineers spot frame-time regressions as they
 * navigate.
 */
class MainActivity : ComponentActivity() {

    private var jankStats: JankStats? = null
    private var totalFrames by mutableIntStateOf(0)
    private var jankyFrames by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deps = AppDependencies(applicationContext)

        // Phase 8 — JankStats frame-timing tracker (DEBUG builds only).
        // The HUD is rendered in the Compose tree below.
        if (BuildConfig.DEBUG) {
            jankStats = JankStats.createAndTrack(
                window,
                Dispatchers.Default.asExecutor()
            ) { frameData ->
                totalFrames++
                if (frameData.isJank) {
                    jankyFrames++
                }
            }
        }

        setContent {
            MonochromeBeatTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    color = Color.Black
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppNavHost(deps = deps)

                        // Phase 8 — JankStats debug HUD.
                        // Shows janky/total frames and drop %.
                        // Turns red at ≥ 5% missed frames (perf budget).
                        if (BuildConfig.DEBUG) {
                            val frames = totalFrames
                            val janky = jankyFrames
                            val droppedPercent =
                                if (frames > 0) (janky.toFloat() / frames) * 100f else 0f
                            val hudColor =
                                if (droppedPercent >= 5f) Color.Red else Color.Green

                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Jank: %d / %d (%.1f%%)",
                                    janky, frames, droppedPercent
                                ),
                                color = hudColor,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        jankStats?.isTrackingEnabled = false
    }
}
