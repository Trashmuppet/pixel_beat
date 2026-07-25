package com.trashmuppet.pixelbeat.feature.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
 * Export destination.
 *
 * Phase 0 ships a format selector (WAV / MP4 / GIF) per
 * `18_COMPONENT_LIBRARY-1.md` / `12_EXPORT_PIPELINE.md`. Real encoding
 * hooks land in Phase 5.
 */
@Composable
fun ExportScreen(
    onFinished: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Export", color = Color.White)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("WAV", "MP4", "GIF").forEach { fmt ->
                Button(
                    onClick = onFinished,
                    modifier = Modifier.sizeIn(minHeight = 48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) { Text(fmt) }
            }
        }

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
