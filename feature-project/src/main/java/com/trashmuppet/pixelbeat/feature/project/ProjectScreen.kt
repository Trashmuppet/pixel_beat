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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Project browser destination.
 *
 * Phase 0 placeholder — Phase 1 will replace this with a list sourced
 * from `:storage`.
 */
@Composable
fun ProjectScreen(
    onProjectSelected: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Recent Projects", color = Color.White)
        Text("(empty)", color = Color.White)

        Button(
            onClick = onProjectSelected,
            modifier = Modifier.sizeIn(minHeight = 48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            )
        ) { Text("Open Sample") }

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
