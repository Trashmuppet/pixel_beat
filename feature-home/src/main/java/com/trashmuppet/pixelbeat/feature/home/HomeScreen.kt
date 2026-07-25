package com.trashmuppet.pixelbeat.feature.home

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
 * Home destination.
 *
 * Phase 0 ships two affordances: "Create" (primary) and "Open" (secondary).
 * Both honour the UI Bible's 48dp minimum touch target.
 */
@Composable
fun HomeScreen(
    onNewProject: () -> Unit,
    onOpenProject: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Monochrome Beat",
            color = Color.White
        )

        Button(
            onClick = onNewProject,
            modifier = Modifier.sizeIn(minHeight = 48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            )
        ) { Text("Create Project") }

        Button(
            onClick = onOpenProject,
            modifier = Modifier.sizeIn(minHeight = 48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            )
        ) { Text("Open Project") }
    }
}
