package com.trashmuppet.pixelbeat.feature.arrangement

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trashmuppet.pixelbeat.core.ui.MonoPalette
import com.trashmuppet.pixelbeat.core.ui.SwingControl
import com.trashmuppet.pixelbeat.core.ui.TapTarget

@Composable
fun ArrangementScreen(
    viewModel: ArrangementViewModel,
    onContinueToExport: () -> Unit,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MonoPalette.Background)
            .semantics { testTag = "route/arrangement" }
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Arrangement", color = MonoPalette.Foreground, fontSize = 18.sp)

        when {
            state.isLoading -> Text("Loading…", color = MonoPalette.Foreground)
            state.project == null -> Text(
                text = state.error ?: "No arrangement.",
                color = MonoPalette.Foreground
            )
            else -> {
                Text(
                    "Loop bars ${state.loop.startBar}..${state.loop.endBar}",
                    color = MonoPalette.Foreground,
                    fontSize = 14.sp
                )

                // Pattern slots — drag-reorder is wired through
                // viewModel.reorderPattern; Phase 3 keeps it tap-driven
                // (one tap on the up/down side of each slot).
                state.patternChain.forEachIndexed { index, patternId ->
                    ArrangementSlot(
                        index = index,
                        total = state.patternChain.size,
                        label = patternId,
                        onMoveUp = if (index > 0) ({ viewModel.reorderPattern(index, index - 1) }) else null,
                        onMoveDown = if (index < state.patternChain.size - 1) ({ viewModel.reorderPattern(index, index + 1) }) else null,
                        onRemove = if (state.patternChain.size > 1) ({ viewModel.removePattern(index) }) else null
                    )
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.addPattern()
                    },
                    modifier = Modifier.sizeIn(minHeight = TapTarget),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MonoPalette.Foreground,
                        contentColor = MonoPalette.Background
                    )
                ) { Text("Add Pattern") }

                SwingControl(
                    swing = state.project?.swing ?: com.trashmuppet.pixelbeat.core.model.SwingMode(),
                    onSwingChange = { /* arrangement owns structure; swing belongs to sequencer */ }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onContinueToExport()
                },
                modifier = Modifier.sizeIn(minHeight = TapTarget),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MonoPalette.Foreground,
                    contentColor = MonoPalette.Background
                )
            ) { Text("Export") }

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

@Composable
private fun ArrangementSlot(
    index: Int,
    total: Int,
    label: String,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onRemove: (() -> Unit)?
) {
    val haptic = LocalHapticFeedback.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MonoPalette.Background,
            contentColor = MonoPalette.Foreground
        ),
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = TapTarget)
            .semantics { contentDescription = "Arrangement slot $index $label" }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("$index $label")
            Box(modifier = Modifier.sizeIn(minWidth = 36.dp, minHeight = 36.dp)) {}
            Text(
                "↑",
                modifier = Modifier
                    .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
                    .clickable(enabled = onMoveUp != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMoveUp?.invoke()
                    }
            )
            Text(
                "↓",
                modifier = Modifier
                    .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
                    .clickable(enabled = onMoveDown != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMoveDown?.invoke()
                    }
            )
            Text(
                "✕",
                modifier = Modifier
                    .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
                    .clickable(enabled = onRemove != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRemove?.invoke()
                    }
            )
        }
    }
    @Suppress("UNUSED_VARIABLE") val unused = total
}

// Backward-compatible stub.
@Composable
fun ArrangementScreen(
    onContinueToExport: () -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(MonoPalette.Background)) {
        Text("…", color = MonoPalette.Foreground)
    }
}
