package org.kvxd.bonk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kvxd.bonk.model.ActiveSoundState
import org.kvxd.bonk.ui.theme.AppColors

@Composable
fun ActiveSoundRow(
    sound: ActiveSoundState,
    isShiftPressed: Boolean,
    onTogglePause: (String) -> Unit,
    onStop: (String) -> Unit,
    onVolumeChange: (String, Boolean, Float) -> Unit,
    onSeek: (String, Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.SurfaceLight, RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (sound.isPaused) AppColors.TextDim else AppColors.Accent)
                    .clickable { onTogglePause(sound.id) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (sound.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    sound.soundFile.name,
                    color = AppColors.Text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1
                )

                var isDragging by remember { mutableStateOf(false) }
                var dragProgress by remember { mutableStateOf(0f) }

                val currentProgress = if (isDragging) dragProgress else sound.progress

                Slider(
                    value = currentProgress,
                    onValueChange = {
                        isDragging = true
                        dragProgress = it
                    },
                    onValueChangeFinished = {
                        onSeek(sound.id, dragProgress)
                        isDragging = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = if (sound.isPaused) AppColors.TextDim else AppColors.Accent,
                        activeTrackColor = if (sound.isPaused) AppColors.TextDim else AppColors.Accent,
                        inactiveTrackColor = Color.Black,
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth().height(14.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            IconButton(onClick = { onStop(sound.id) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, null, tint = AppColors.Danger, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Headphones, null, tint = AppColors.TextDim, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))

                Slider(
                    value = sound.localVolume,
                    onValueChange = { newVal ->
                        onVolumeChange(sound.id, true, newVal)
                        if (isShiftPressed) onVolumeChange(sound.id, false, newVal)
                    },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = AppColors.Text, activeTrackColor = AppColors.Text),
                    modifier = Modifier.height(20.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Mic, null, tint = AppColors.Danger, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))

                Slider(
                    value = sound.remoteVolume,
                    onValueChange = { newVal ->
                        onVolumeChange(sound.id, false, newVal)
                        if (isShiftPressed) onVolumeChange(sound.id, true, newVal)
                    },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = AppColors.Danger, activeTrackColor = AppColors.Danger),
                    modifier = Modifier.height(20.dp)
                )
            }
        }
    }
}