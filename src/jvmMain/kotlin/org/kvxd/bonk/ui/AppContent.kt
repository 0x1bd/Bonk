package org.kvxd.bonk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.kvxd.bonk.controller.SoundboardController
import org.kvxd.bonk.ui.theme.AppColors

@Composable
fun AppContent() {
    val state by SoundboardController.uiState.collectAsState()

    MaterialTheme(
        colors = darkColors(
            background = AppColors.Background,
            surface = AppColors.Surface,
            primary = AppColors.Accent,
            onBackground = AppColors.Text,
            onSurface = AppColors.Text
        )
    ) {
        if (state.error != null) {
            ErrorScreen(state.error!!.split(", "))
        } else {
            MainScreen(state)
        }
    }
}

@Composable
fun ErrorScreen(missingDeps: List<String>) {
    Box(Modifier.fillMaxSize().background(AppColors.Background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.Warning, null, tint = AppColors.Danger, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(24.dp))
            Text("Missing Dependencies", style = MaterialTheme.typography.h5, color = AppColors.Text)
            Spacer(Modifier.height(16.dp))
            Text("The following system components are required but not found:", color = AppColors.TextDim)
            Spacer(Modifier.height(8.dp))
            missingDeps.forEach {
                Text("• $it", color = AppColors.Accent, style = MaterialTheme.typography.body1)
            }
            Spacer(Modifier.height(24.dp))
            Text("Please install them via your package manager.", color = AppColors.TextDim)
        }
    }
}