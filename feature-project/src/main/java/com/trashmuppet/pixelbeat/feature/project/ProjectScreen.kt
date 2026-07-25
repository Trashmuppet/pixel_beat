package com.trashmuppet.pixelbeat.feature.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.trashmuppet.pixelbeat.core.common.AppDispatchers
import com.trashmuppet.pixelbeat.core.common.DefaultAppDispatchers
import com.trashmuppet.pixelbeat.core.model.MBeatProject
import com.trashmuppet.pixelbeat.core.ui.MonoPalette
import com.trashmuppet.pixelbeat.core.ui.RecentProjectList
import com.trashmuppet.pixelbeat.core.ui.TapTarget
import com.trashmuppet.pixelbeat.storage.ProjectRepository
import kotlinx.coroutines.launch

/**
 * Project browser destination.
 *
 * Loads `ProjectRepository.listRecent()` (`14_STORAGE.md` rebuildable
 * cache) and renders via the shared `RecentProjectList` component.
 */
@Composable
fun ProjectScreen(
    repository: ProjectRepository,
    dispatchers: AppDispatchers = DefaultAppDispatchers(),
    onProjectSelected: (MBeatProject) -> Unit,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var recent by remember { mutableStateOf<List<MBeatProject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch(dispatchers.io) {
            repository.listRecent()
                .onSuccess { recent = it; loading = false }
                .onFailure { error = it.message; loading = false }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Recent Projects", color = MonoPalette.Foreground, modifier = Modifier.padding(top = 16.dp))
        Text(
            when {
                loading -> "Loading…"
                error != null -> "Error: $error"
                recent.isEmpty() -> "No projects yet — create one from Home."
                else -> "${recent.size} project(s)"
            },
            color = MonoPalette.Foreground
        )

        if (recent.isNotEmpty()) {
            RecentProjectList(projects = recent, onSelect = onProjectSelected)
        }

        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onBack()
            },
            modifier = Modifier.sizeIn(minHeight = TapTarget),
            colors = ButtonDefaults.buttonColors(
                containerColor = MonoPalette.Background,
                contentColor = MonoPalette.Foreground
            )
        ) { Text("Back") }
    }
}

// Older signature kept for AppNavHost backward compatibility.
@Composable
fun ProjectScreen(
    onProjectSelected: () -> Unit,
    onBack: () -> Unit
) {
    Text(
        text = "ProjectScreen: open the sample fixture.",
        color = MonoPalette.Foreground,
        modifier = Modifier.padding(top = 16.dp)
    )
}
