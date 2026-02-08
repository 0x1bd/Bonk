package org.kvxd.sondbord.ui.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.kvxd.sondbord.model.ActiveSoundState
import org.kvxd.sondbord.ui.components.ActiveSoundRow
import org.kvxd.sondbord.ui.theme.AppColors

@Composable
fun PlayerPanel(
    activeSounds: List<ActiveSoundState>,
    isShiftPressed: Boolean,
    onTogglePause: (String) -> Unit,
    onStop: (String) -> Unit,
    onVolumeChange: (String, Boolean, Float) -> Unit,
    onSeek: (String, Float) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(AppColors.Surface)
            .padding(10.dp)
    ) {
        Text("NOW PLAYING", style = MaterialTheme.typography.caption, color = AppColors.Accent)
        Spacer(Modifier.height(5.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(activeSounds, key = { it.id }) { sound ->
                ActiveSoundRow(
                    sound = sound,
                    isShiftPressed = isShiftPressed,
                    onTogglePause = onTogglePause,
                    onStop = onStop,
                    onVolumeChange = onVolumeChange,
                    onSeek = onSeek
                )
            }
        }
    }
}