package com.trashmuppet.pixelbeat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.trashmuppet.pixelbeat.feature.arrangement.ArrangementScreen
import com.trashmuppet.pixelbeat.feature.export.ExportScreen
import com.trashmuppet.pixelbeat.feature.home.HomeScreen
import com.trashmuppet.pixelbeat.feature.project.ProjectScreen
import com.trashmuppet.pixelbeat.feature.sequencer.SequencerScreen

/**
 * Navigation routes for the six top-level destinations in `16_UI_BIBLE.md`:
 * Home → Project → Sequencer → Arrangement → Export → Settings.
 *
 * Phase 0 wires Home → Project → Sequencer → Arrangement → Export.
 * Settings is reserved for a later phase and currently surfaces as
 * Home's overflow affordance (placeholder, no behaviour).
 */
object Routes {
    const val HOME = "home"
    const val PROJECT = "project"
    const val SEQUENCER = "sequencer"
    const val ARRANGEMENT = "arrangement"
    const val EXPORT = "export"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNewProject = { navController.navigate(Routes.SEQUENCER) },
                onOpenProject = { navController.navigate(Routes.PROJECT) }
            )
        }
        composable(Routes.PROJECT) {
            ProjectScreen(
                onProjectSelected = { navController.navigate(Routes.SEQUENCER) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SEQUENCER) {
            SequencerScreen(
                onContinueToArrangement = { navController.navigate(Routes.ARRANGEMENT) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.ARRANGEMENT) {
            ArrangementScreen(
                onContinueToExport = { navController.navigate(Routes.EXPORT) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.EXPORT) {
            ExportScreen(
                onFinished = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
