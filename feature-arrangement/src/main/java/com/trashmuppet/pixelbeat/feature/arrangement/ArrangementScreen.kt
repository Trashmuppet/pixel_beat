package com.trashmuppet.pixelbeat.feature.arrangement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Song-arranger destination.
 *
 * Phase 0 lists three named pattern slots. Phase 3 wires the real
 * arrangement timeline (`07_TIMELINE_ENGINE.md` — "Arrangement transitions").
 */
@Composable
fun ArrangementScreen(
    onContinueToExport: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Arrangement", color = Color.White)

        listOf("Intro", "Verse", "Outro").forEach { slot ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(slot, modifier = Modifier.padding(16.dp))
            }
        }

        Button(
            onClick = onContinueToExport,
            modifier = Modifier.sizeIn(minHeight = 48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            )
        ) { Text("Export") }

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
