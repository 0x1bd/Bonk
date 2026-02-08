package org.kvxd.bonk.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kvxd.bonk.model.SoundFile
import org.kvxd.bonk.ui.theme.AppColors

@Composable
fun SoundCard(
    sound: SoundFile,
    onClick: () -> Unit,
    onToggleFav: () -> Unit
) {
    val isDir = sound.file.isDirectory
    Card(
        backgroundColor = if (isDir) AppColors.SurfaceLight else AppColors.Surface,
        modifier = Modifier.height(50.dp).clickable { onClick() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            Icon(
                if (isDir) Icons.Default.Folder else Icons.Default.MusicNote,
                null,
                tint = if (isDir) AppColors.Text else AppColors.Accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))

            Text(
                text = sound.name,
                color = AppColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                fontSize = 13.sp
            )

            if (!isDir) {
                IconButton(onClick = onToggleFav, modifier = Modifier.size(24.dp)) {
                    Icon(
                        if (sound.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        null,
                        tint = if (sound.isFavorite) Color.Yellow else AppColors.TextDim,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}