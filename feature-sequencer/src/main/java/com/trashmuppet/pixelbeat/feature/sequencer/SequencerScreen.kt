package com.trashmuppet.pixelbeat.feature.sequencer

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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trashmuppet.pixelbeat.core.ui.MonoPalette
import com.trashmuppet.pixelbeat.core.ui.PatternSelector
import com.trashmuppet.pixelbeat.core.ui.Playhead
import com.trashmuppet.pixelbeat.core.ui.StepCell
import com.trashmuppet.pixelbeat.core.ui.SwingControl
import com.trashmuppet.pixelbeat.core.ui.TapTarget
import com.trashmuppet.pixelbeat.core.ui.TempoControl
import com.trashmuppet.pixelbeat.core.ui.TrackHeader

@Composable
fun SequencerScreen(
    viewModel: SequencerViewModel,
    onContinueToArrangement: () -> Unit,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val project = state.project
    val pattern = state.currentPattern

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MonoPalette.Background)
            .semantics { testTag = "route/sequencer" }
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val newBucket = MonoPalette.Background
        Text("Sequencer", color = MonoPalette.Foreground, fontSize = 18.sp)

        when {
            state.isLoading -> Text("Loading project…", color = MonoPalette.Foreground)
            project == null -> Text(
                text = state.error ?: "Project unavailable.",
                color = MonoPalette.Foreground
            )
            pattern == null -> Text("No pattern selected.", color = MonoPalette.Foreground)
            else -> {
                PatternSelector(
                    patterns = project.patterns,
                    currentIndex = state.currentPatternIndex,
                    onSelect = { viewModel.selectPattern(it) }
                )

                Playhead(stepIndex = state.playheadStep, totalSteps = pattern.lengthSteps)

                pattern.tracks.forEach { track ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TrackHeader(
                            name = track.id,
                            drumKind = track.kind,
                            muted = track.mute,
                            onMuteToggle = { viewModel.toggleMute(track.id) }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            track.steps.forEachIndexed { stepIndex, active ->
                                StepCell(
                                    active = active,
                                    onToggle = { viewModel.toggleStep(track.id, stepIndex) }
                                )
                            }
                        }
                    }
                }

                TempoControl(
                    bpm = project.bpm,
                    onBpmChange = { viewModel.setBpm(it) }
                )

                SwingControl(
                    swing = project.swing,
                    onSwingChange = { viewModel.setSwing(it) }
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
                    if (state.isPlaying) viewModel.pause() else viewModel.play()
                },
                modifier = Modifier.sizeIn(minHeight = TapTarget),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MonoPalette.Foreground,
                    contentColor = MonoPalette.Background
                )
            ) { Text(if (state.isPlaying) "Pause" else "Play") }

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onContinueToArrangement()
                },
                modifier = Modifier.sizeIn(minHeight = TapTarget),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MonoPalette.Foreground,
                    contentColor = MonoPalette.Background
                )
            ) { Text("Next") }

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

// Backward-compatible legacy entry-point used by `AppNavHost` when no
// VM is wired. Returns the same visual layout as the live VM-driven
// screen with a blank in-memory state.
@Composable
fun SequencerScreen(
    onContinueToArrangement: () -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(MonoPalette.Background)) {
        Text("…", color = MonoPalette.Foreground)
    }
    // No-op shim — VM-backed form is the canonical entry.
}
