package com.trashmuppet.pixelbeat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.trashmuppet.pixelbeat.ui.navigation.AppNavHost
import com.trashmuppet.pixelbeat.ui.theme.MonochromeBeatTheme

/**
 * Sole activity hosting the Compose graph.
 *
 * Builds `AppDependencies` once on the activity lifecycle and shares
 * them with the navigation graph. Each feature ViewModel is built
 * via a factory that consumes those dependencies so unit tests can
 * swap modules.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deps = AppDependencies(applicationContext)
        setContent {
            remember { deps }  // hoist identity, prevents spurious re-init
            MonochromeBeatTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    color = Color.Black
                ) {
                    AppNavHost(deps = deps)
                }
            }
        }
    }
}
