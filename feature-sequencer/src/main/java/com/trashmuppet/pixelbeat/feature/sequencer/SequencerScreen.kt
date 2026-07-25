package com.trashmuppet.pixelbeat.feature.sequencer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Pattern editor destination.
 *
 * Phase 0 ships a 16-step grid using `StepCell` cells (`18_COMPONENT_LIBRARY-1.md`).
 * Phase 3 wires the real pattern editor + Tempo + Swing controls.
 */
@Composable
fun SequencerScreen(
    onContinueToArrangement: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Sequencer — 16 steps", color = Color.White)

        val stepCount = 16
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (i in 0 until stepCount) {
                StepCell(active = i % 4 == 0)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onContinueToArrangement,
                modifier = Modifier.sizeIn(minHeight = 48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                )
            ) { Text("Next") }
            Button(
                onClick = onBack,
                modifier = Modifier.sizeIn(minHeight = 48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                )
            ) { Text("Back") }
        }
    }
}

@Composable
private fun StepCell(active: Boolean) {
    val bg = if (active) Color.White else Color.Black
    val fg = if (active) Color.Black else Color.White
    Text(
        text = "●",
        color = fg,
        modifier = Modifier
            .size(40.dp)
            .background(bg)
            .padding(8.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}
