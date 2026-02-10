package org.kvxd.bonk.ui.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kvxd.bonk.controller.SoundboardController
import org.kvxd.bonk.model.SortMode
import org.kvxd.bonk.model.SoundFilter
import org.kvxd.bonk.ui.components.FilterButton
import org.kvxd.bonk.ui.components.MicSelector
import org.kvxd.bonk.ui.components.VolumeControl
import org.kvxd.bonk.ui.theme.AppColors
import org.kvxd.bonk.controller.UiState

@Composable
fun Sidebar(
    state: UiState,
    isShiftPressed: Boolean,
    onOpenDownloader: () -> Unit
) {
    Column(
        modifier = Modifier.width(260.dp).fillMaxHeight().background(AppColors.Surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SidebarSectionHeader("LIBRARY") {
                IconButton(onClick = { SoundboardController.refreshFiles() }, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Refresh, "Refresh Library", tint = AppColors.TextDim)
                }
            }

            OutlinedButton(
                onClick = onOpenDownloader,
                enabled = state.isDownloaderAvailable,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = AppColors.SurfaceLight.copy(alpha = 0.3f),
                    contentColor = AppColors.Text
                ),
                border = BorderStroke(1.dp, if(state.isDownloaderAvailable) AppColors.SurfaceLight else AppColors.SurfaceLight.copy(alpha=0.5f)),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(18.dp), tint = if(state.isDownloaderAvailable) AppColors.Accent else AppColors.TextDim)
                Spacer(Modifier.width(10.dp))
                Text("Download (yt-dlp)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterButton("All", state.filterMode == SoundFilter.All) { SoundboardController.setFilter(SoundFilter.All) }
                FilterButton("Favorites", state.filterMode == SoundFilter.Favorites) {
                    SoundboardController.setFilter(SoundFilter.Favorites)
                }
            }

            Divider(color = AppColors.SurfaceLight)
            SidebarSectionHeader("INPUT DEVICE")
            MicSelector(state.microphones, state.settings.inputDeviceId) { mic -> SoundboardController.setMicrophone(mic) }

            OutlinedButton(
                onClick = { SoundboardController.toggleSort() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Text),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.Sort, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (state.settings.sortMode == SortMode.Name) "Sort: Name" else "Sort: Date")
            }

            Divider(color = AppColors.SurfaceLight)
            SidebarSectionHeader("MASTER VOLUME")

            VolumeControl(
                label = "Local (Ear)",
                value = state.settings.localVolume,
                max = 100f
            ) { newVal ->
                SoundboardController.setMasterVolume(true, newVal)
                if (isShiftPressed) SoundboardController.setMasterVolume(false, newVal)
            }

            VolumeControl(
                label = "Inject (Mic)",
                value = state.settings.remoteVolume,
                max = 100f,
                color = AppColors.Danger
            ) { newVal ->
                SoundboardController.setMasterVolume(false, newVal)
                if (isShiftPressed) SoundboardController.setMasterVolume(true, newVal)
            }
        }

        Button(
            onClick = { SoundboardController.stopAll() },
            colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.Danger),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.Stop, null, tint = Color.Black)
            Spacer(Modifier.width(8.dp))
            Text("STOP ALL", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SidebarSectionHeader(text: String, action: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = AppColors.TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        action?.invoke()
    }
}