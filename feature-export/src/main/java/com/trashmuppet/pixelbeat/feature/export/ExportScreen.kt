package com.trashmuppet.pixelbeat.feature.export

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trashmuppet.pixelbeat.core.ui.ChipRow
import com.trashmuppet.pixelbeat.core.ui.ExportProgressBar
import com.trashmuppet.pixelbeat.core.ui.MonoPalette
import com.trashmuppet.pixelbeat.core.ui.TapTarget
import com.trashmuppet.pixelbeat.premium.PremiumState
import java.io.File

@Composable
fun ExportScreen(
    viewModel: ExportViewModel,
    onFinished: () -> Unit,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val state: ExportState by viewModel.state.collectAsStateWithLifecycle()
    val premiumState by viewModel.premiumManager.entitlementState.collectAsStateWithLifecycle()
    val isPro = premiumState is PremiumState.Pro

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MonoPalette.Background)
            .semantics { testTag = "route/export" }
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Export", color = MonoPalette.Foreground, fontSize = 18.sp)

        when (val s = state) {
            is ExportState.Idle -> Text("Loading project…", color = MonoPalette.Foreground)
            is ExportState.Error -> Text(
                "Failed to load project: ${s.cause.message ?: "Unknown error"}",
                color = MonoPalette.Foreground
            )
            is ExportState.Configuring -> {
                Text("${s.project.name}", color = MonoPalette.Foreground)
                ChipRow(
                    options = ExportFormat.entries,
                    selected = s.format,
                    label = { it.extension.uppercase() },
                    contentDesc = { "Format ${it.extension.uppercase()}" },
                    onSelect = { viewModel.selectFormat(it) }
                )
                if (s.format.isVideo) {
                    val allowedResolutions = ExportResolution.entries.filter { !it.requiresPro || isPro }
                    ChipRow(
                        options = allowedResolutions,
                        selected = s.resolution,
                        label = { "${it.width}×${it.height}" },
                        contentDesc = { "Resolution ${it.width} by ${it.height}" },
                        onSelect = { viewModel.selectResolution(it) }
                    )
                    if (!isPro) {
                        Text(
                            "FHD/4K available with Pro",
                            color = MonoPalette.Foreground,
                            fontSize = 11.sp
                        )
                    }
                }
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val out = File(context.filesDir, "Exports/${s.project.id}.${s.format.extension}")
                        viewModel.startExport(out)
                    },
                    modifier = Modifier.sizeIn(minHeight = TapTarget),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MonoPalette.Foreground,
                        contentColor = MonoPalette.Background
                    )
                ) { Text("Export ${s.format.extension.uppercase()}") }
            }
            is ExportState.Rendering -> {
                ExportProgressBar(progress = s.progress, statusLine = s.statusLine)
                Text("Rendering… do not close the app.", color = MonoPalette.Foreground, fontSize = 12.sp)
            }
            is ExportState.Success -> {
                Text("✓ Saved to ${s.path}", color = MonoPalette.Foreground)
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFinished()
                    },
                    modifier = Modifier.sizeIn(minHeight = TapTarget),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MonoPalette.Foreground,
                        contentColor = MonoPalette.Background
                    )
                ) { Text("Done") }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
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
}

// Backward-compatible stub.
@Composable
fun ExportScreen(
    onFinished: () -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(MonoPalette.Background)) {
        Text("…", color = MonoPalette.Foreground)
    }
}
