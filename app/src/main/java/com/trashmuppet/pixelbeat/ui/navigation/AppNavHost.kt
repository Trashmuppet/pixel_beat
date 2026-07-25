package com.trashmuppet.pixelbeat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.trashmuppet.pixelbeat.AppDependencies
import com.trashmuppet.pixelbeat.feature.arrangement.ArrangementScreen
import com.trashmuppet.pixelbeat.feature.arrangement.ArrangementViewModel
import com.trashmuppet.pixelbeat.feature.export.ExportScreen
import com.trashmuppet.pixelbeat.feature.export.ExportViewModel
import com.trashmuppet.pixelbeat.feature.home.HomeScreen
import com.trashmuppet.pixelbeat.feature.project.ProjectScreen
import com.trashmuppet.pixelbeat.feature.sequencer.SequencerScreen
import com.trashmuppet.pixelbeat.feature.sequencer.SequencerViewModel

/**
 * Navigation routes — `16_UI_BIBLE.md`.
 */
object Routes {
    const val HOME = "home"
    const val PROJECT = "project"
    const val SEQUENCER = "sequencer"
    const val ARRANGEMENT = "arrangement"
    const val EXPORT = "export"
}

@Composable
fun AppNavHost(deps: AppDependencies) {
    val navController = rememberNavController()
    val context = LocalContext.current
    // Once-per-graph construct — Compose route composition keeps deps identical.
    val factories = remember(deps) { AppViewModelFactories(deps) }

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                recent = emptyList(),   // wired via a parent-side controller when needed
                onNewProject = { navController.navigate(Routes.SEQUENCER) },
                onOpenProject = { navController.navigate(Routes.PROJECT) }
            )
        }

        composable(Routes.PROJECT) {
            ProjectScreen(
                repository = deps.projectRepository,
                onProjectSelected = { navController.navigate(Routes.SEQUENCER) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SEQUENCER) {
            val vm: SequencerViewModel = viewModel(
                factory = SequencerViewModel.factory(
                    dispatchers = deps.dispatchers,
                    repository = deps.projectRepository,
                    transport = deps.transport,
                    initialProjectId = null
                )
            )
            SequencerScreen(
                viewModel = vm,
                onContinueToArrangement = { navController.navigate(Routes.ARRANGEMENT) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ARRANGEMENT) {
            val vm: ArrangementViewModel = viewModel(
                factory = ArrangementViewModel.factory(
                    dispatchers = deps.dispatchers,
                    repository = deps.projectRepository,
                    initialProjectId = null
                )
            )
            ArrangementScreen(
                viewModel = vm,
                onContinueToExport = { navController.navigate(Routes.EXPORT) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.EXPORT) {
            val vm: ExportViewModel = viewModel(
                factory = ExportViewModel.factory(
                    dispatchers = deps.dispatchers,
                    repository = deps.projectRepository,
                    exporter = deps.exporter,
                    initialProjectId = null
                )
            )
            ExportScreen(
                viewModel = vm,
                onFinished = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * Holds the factories the navigation graph needs to construct
 * ViewModels per route. Reserved for use when the project graduates
 * to a real DI framework (Phase 6).
 */
private class AppViewModelFactories(val deps: AppDependencies)
