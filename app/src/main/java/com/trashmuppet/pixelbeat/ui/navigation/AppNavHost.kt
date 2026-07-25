package com.trashmuppet.pixelbeat.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trashmuppet.pixelbeat.AppDependencies
import com.trashmuppet.pixelbeat.core.ui.MonoPalette
import com.trashmuppet.pixelbeat.core.ui.TapTarget
import com.trashmuppet.pixelbeat.feature.arrangement.ArrangementScreen
import com.trashmuppet.pixelbeat.feature.arrangement.ArrangementViewModel
import com.trashmuppet.pixelbeat.feature.export.ExportScreen
import com.trashmuppet.pixelbeat.feature.export.ExportViewModel
import com.trashmuppet.pixelbeat.feature.home.HomeScreen
import com.trashmuppet.pixelbeat.feature.project.ProjectScreen
import com.trashmuppet.pixelbeat.feature.sequencer.SequencerScreen
import com.trashmuppet.pixelbeat.feature.sequencer.SequencerViewModel
import com.trashmuppet.pixelbeat.premium.PremiumState
import com.trashmuppet.pixelbeat.scene.api.ScenePackRegistry
import kotlinx.coroutines.launch

/**
 * Navigation routes — `16_UI_BIBLE.md`.
 *
 * `projectId` is threaded through route arguments (`project/{projectId}`
 * → `sequencer/{projectId}` → `arrangement/{projectId}` →
 * `export/{projectId}`) so each ViewModel can reload the same
 * `.mbeat` independent of screen lifecycle.
 */
object Routes {
    const val HOME = "home"

    private const val PROJECT_ID_ARG = "projectId"
    const val PROJECT = "project"
    const val SEQUENCER = "sequencer/{$PROJECT_ID_ARG}"
    const val ARRANGEMENT = "arrangement/{$PROJECT_ID_ARG}"
    const val EXPORT = "export/{$PROJECT_ID_ARG}"
    const val SETTINGS = "settings"

    const val NEW_PROJECT = "new"

    fun sequencer(projectId: String) = "sequencer/$projectId"
    fun arrangement(projectId: String) = "arrangement/$projectId"
    fun export(projectId: String) = "export/$projectId"

    val PROJECT_ID_KEY = PROJECT_ID_ARG
}

@Composable
fun AppNavHost(deps: AppDependencies) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val factories = remember(deps) { AppViewModelFactories(deps) }
    val scope = rememberCoroutineScope()
    val premiumState by deps.premiumManager.entitlementState.collectAsStateWithLifecycle()
    val isPro = premiumState is PremiumState.Pro

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                recent = emptyList(),
                scenePacks = ScenePackRegistry.all,
                isPro = isPro,
                onNewProject = {
                    navController.navigate(Routes.sequencer(Routes.NEW_PROJECT))
                },
                onOpenProject = { navController.navigate(Routes.PROJECT) },
                onUnlockPro = { scope.launch { deps.premiumManager.launchPurchaseFlow(context) } }
            )
        }

        composable(Routes.PROJECT) {
            ProjectScreen(
                repository = deps.projectRepository,
                onProjectSelected = { project ->
                    navController.navigate(Routes.sequencer(project.id))
                },
                onBack = { navController.popBackStack() },
                onSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = Routes.SEQUENCER,
            arguments = listOf(navArgument(Routes.PROJECT_ID_KEY) { type = NavType.StringType })
        ) { entry ->
            val projectId = entry.arguments?.getString(Routes.PROJECT_ID_KEY)
            val vm: SequencerViewModel = viewModel(
                factory = SequencerViewModel.factory(
                    dispatchers = deps.dispatchers,
                    repository = deps.projectRepository,
                    transport = deps.transport,
                    initialProjectId = projectId
                )
            )
            SequencerScreen(
                viewModel = vm,
                onContinueToArrangement = {
                    val activeId = projectId ?: Routes.NEW_PROJECT
                    navController.navigate(Routes.arrangement(activeId))
                },
                onBack = {
                    if (vm.state.value.isPlaying) vm.pause()
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.ARRANGEMENT,
            arguments = listOf(navArgument(Routes.PROJECT_ID_KEY) { type = NavType.StringType })
        ) { entry ->
            val projectId = entry.arguments?.getString(Routes.PROJECT_ID_KEY)
            val vm: ArrangementViewModel = viewModel(
                factory = ArrangementViewModel.factory(
                    dispatchers = deps.dispatchers,
                    repository = deps.projectRepository,
                    initialProjectId = projectId
                )
            )
            ArrangementScreen(
                viewModel = vm,
                onContinueToExport = {
                    val activeId = projectId ?: Routes.NEW_PROJECT
                    navController.navigate(Routes.export(activeId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.EXPORT,
            arguments = listOf(navArgument(Routes.PROJECT_ID_KEY) { type = NavType.StringType })
        ) { entry ->
            val projectId = entry.arguments?.getString(Routes.PROJECT_ID_KEY)
            val vm: ExportViewModel = viewModel(
                factory = ExportViewModel.factory(
                    dispatchers = deps.dispatchers,
                    repository = deps.projectRepository,
                    exporter = deps.exporter,
                    premiumManager = deps.premiumManager,
                    initialProjectId = projectId
                )
            )
            ExportScreen(
                viewModel = vm,
                onFinished = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsPlaceholder(onBack = { navController.popBackStack() })
        }
    }

    @Suppress("UNUSED_VARIABLE") val unusedScope = scope // reserved for future Snackbar / share intents
}

/**
 * Phase 6 reserved destination — `16_UI_BIBLE.md` confirms Settings as
 * one of the six top-level destinations. Left empty until the
 * settings surface (preference storage, gesture toggles, etc.) ships.
 */
@Composable
private fun SettingsPlaceholder(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MonoPalette.Background)
            .semantics { testTag = "route/settings" }
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Settings", color = MonoPalette.Foreground, fontSize = 18.sp)
        Text("Reserved for Phase 6+", color = MonoPalette.Foreground, fontSize = 12.sp)
        Button(
            onClick = onBack,
            modifier = Modifier.sizeIn(minHeight = TapTarget),
            colors = ButtonDefaults.buttonColors(
                containerColor = MonoPalette.Background,
                contentColor = MonoPalette.Foreground
            )
        ) {
            Text("Back")
        }
    }
}

/**
 * Holds the factories the navigation graph needs to construct
 * ViewModels per route. Reserved for use when the project graduates
 * to a real DI framework (Phase 6).
 */
private class AppViewModelFactories(val deps: AppDependencies)
