package org.kvxd.bonk.ui.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.kvxd.bonk.model.SoundFile
import org.kvxd.bonk.ui.components.SearchBar
import org.kvxd.bonk.ui.components.SoundCard
import org.kvxd.bonk.ui.theme.AppColors
import java.io.File

@Composable
fun SoundBrowser(
    currentDir: File,
    sounds: List<SoundFile>,
    searchQuery: String,
    isHome: Boolean,
    onSearch: (String) -> Unit,
    onNavigate: (File) -> Unit,
    onNavigateUp: () -> Unit,
    onPlay: (SoundFile) -> Unit,
    onToggleFav: (SoundFile) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        SearchBar(
            value = searchQuery,
            onValueChange = onSearch,
            placeholder = "Search sounds..."
        )

        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isHome && searchQuery.isEmpty()) {
                item {
                    Button(
                        onClick = onNavigateUp,
                        colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.SurfaceLight),
                        modifier = Modifier.height(50.dp).fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = AppColors.Text)
                        Text(" Back", color = AppColors.Text)
                    }
                }
            }

            items(sounds) { sound ->
                SoundCard(
                    sound = sound,
                    onClick = {
                        if (sound.file.isDirectory) onNavigate(sound.file) else onPlay(sound)
                    },
                    onToggleFav = { onToggleFav(sound) }
                )
            }
        }
    }
}