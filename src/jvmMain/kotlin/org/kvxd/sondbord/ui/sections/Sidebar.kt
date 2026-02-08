package org.kvxd.sondbord.ui.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kvxd.sondbord.model.SortMode
import org.kvxd.sondbord.model.SoundFilter
import org.kvxd.sondbord.ui.SidebarActions
import org.kvxd.sondbord.ui.components.FilterButton
import org.kvxd.sondbord.ui.components.MicSelector
import org.kvxd.sondbord.ui.components.VolumeControl
import org.kvxd.sondbord.ui.theme.AppColors
import org.kvxd.sondbord.viewmodel.UiState

@Composable
fun Sidebar(
    state: UiState,
    isShiftPressed: Boolean,
    actions: SidebarActions
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
                IconButton(onClick = actions.onRefresh, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Refresh, null, tint = AppColors.TextDim)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterButton("All", state.filterMode == SoundFilter.All) { actions.onFilterChange(SoundFilter.All) }
                FilterButton("Favorites", state.filterMode == SoundFilter.Favorites) {
                    actions.onFilterChange(
                        SoundFilter.Favorites
                    )
                }
            }

            Divider(color = AppColors.SurfaceLight)
            SidebarSectionHeader("INPUT DEVICE")
            MicSelector(state.microphones, state.settings.inputDeviceId, actions.onMicChange)

            OutlinedButton(
                onClick = actions.onSortChange,
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
                actions.onVolumeChange(true, newVal)
                if (isShiftPressed) actions.onVolumeChange(false, newVal)
            }

            VolumeControl(
                label = "Inject (Mic)",
                value = state.settings.remoteVolume,
                max = 100f,
                color = AppColors.Danger
            ) { newVal ->
                actions.onVolumeChange(false, newVal)
                if (isShiftPressed) actions.onVolumeChange(true, newVal)
            }
        }

        Button(
            onClick = actions.onStopAll,
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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text, color = AppColors.TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        action?.invoke()
    }
}