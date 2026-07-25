package com.trashmuppet.pixelbeat.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trashmuppet.pixelbeat.core.model.MBeatProject
import com.trashmuppet.pixelbeat.core.ui.ExportProgressBar
import com.trashmuppet.pixelbeat.core.ui.MonoPalette
import com.trashmuppet.pixelbeat.core.ui.ProjectCard
import com.trashmuppet.pixelbeat.core.ui.TapTarget

/**
 * Home destination — two primary affordances per `16_UI_BIBLE.md`
 * (Create / Open). Recent projects listed under "Open" so the user
 * can resume work without re-creating a beat.
 */
@Composable
fun HomeScreen(
    recent: List<MBeatProject> = emptyList(),
    onNewProject: () -> Unit,
    onOpenProject: () -> Unit,
    onProjectSelected: (MBeatProject) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Monochrome Beat",
            color = MonoPalette.Foreground,
            fontSize = 24.sp
        )
        Text(
            text = "Create a beat. Watch it come alive.",
            color = MonoPalette.Foreground,
            fontSize = 14.sp
        )

        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onNewProject()
            },
            modifier = Modifier.sizeIn(minHeight = TapTarget),
            colors = ButtonDefaults.buttonColors(
                containerColor = MonoPalette.Foreground,
                contentColor = MonoPalette.Background
            )
        ) { Text("Create Project") }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Recent", color = MonoPalette.Foreground, fontSize = 14.sp)

        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onOpenProject()
            },
            modifier = Modifier.sizeIn(minHeight = TapTarget),
            colors = ButtonDefaults.buttonColors(
                containerColor = MonoPalette.Background,
                contentColor = MonoPalette.Foreground
            )
        ) { Text("Browse All") }

        recent.take(3).forEach { project ->
            ProjectCard(
                project = project,
                onClick = { onProjectSelected(project) },
                onLongPress = { /* delete later */ }
            )
        }

        // Keep the export progress bar import path live so
        // children of HomeScreen never silently drop the symbol.
        ExportProgressBar(progress = 0f, statusLine = "")
    }
}
