package org.kvxd.sondbord.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import org.kvxd.sondbord.Globals
import org.kvxd.sondbord.model.Microphone
import org.kvxd.sondbord.model.SoundFilter
import org.kvxd.sondbord.ui.sections.DownloaderDialog
import org.kvxd.sondbord.ui.sections.PlayerPanel
import org.kvxd.sondbord.ui.sections.Sidebar
import org.kvxd.sondbord.ui.sections.SoundBrowser
import org.kvxd.sondbord.ui.theme.AppColors
import org.kvxd.sondbord.viewmodel.SoundboardViewModel
import org.kvxd.sondbord.viewmodel.UiState

@Composable
fun MainScreen(state: UiState, viewModel: SoundboardViewModel) {
    val focusManager = LocalFocusManager.current

    var isShiftPressed by remember { mutableStateOf(false) }
    var isDownloaderOpen by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(AppColors.Background)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent {
                if (it.key == Key.ShiftLeft || it.key == Key.ShiftRight) {
                    isShiftPressed = it.type == KeyEventType.KeyDown || it.type != KeyEventType.KeyUp
                }
                false
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
                focusRequester.requestFocus()
            }
    ) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        Column(Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(1f)) {
                Sidebar(
                    state = state,
                    isShiftPressed = isShiftPressed,
                    actions = SidebarActions(
                        onFilterChange = viewModel::setFilter,
                        onSortChange = viewModel::toggleSort,
                        onVolumeChange = viewModel::setMasterVolume,
                        onMicChange = viewModel::setMicrophone,
                        onRefresh = viewModel::refreshFiles,
                        onStopAll = viewModel::stopAll,
                        onOpenDownloader = { isDownloaderOpen = true }
                    )
                )

                Box(Modifier.width(1.dp).fillMaxHeight().background(AppColors.SurfaceLight))

                SoundBrowser(
                    currentDir = state.currentDir,
                    sounds = state.sounds,
                    searchQuery = state.searchQuery,
                    isHome = state.currentDir.absolutePath == Globals.SOUNDS_DIR.absolutePath,
                    onSearch = viewModel::setSearch,
                    onNavigate = viewModel::navigate,
                    onNavigateUp = viewModel::navigateUp,
                    onPlay = viewModel::playSound,
                    onToggleFav = viewModel::toggleFavorite
                )
            }

            AnimatedVisibility(
                visible = state.activeSounds.isNotEmpty(),
                enter = slideInVertically { it } + expandVertically() + fadeIn(),
                exit = slideOutVertically { it } + shrinkVertically() + fadeOut()
            ) {
                Column {
                    Box(Modifier.height(1.dp).fillMaxWidth().background(AppColors.SurfaceLight))
                    PlayerPanel(
                        activeSounds = state.activeSounds,
                        isShiftPressed = isShiftPressed,
                        onTogglePause = viewModel::togglePause,
                        onStop = viewModel::stopSound,
                        onVolumeChange = viewModel::setSoundVolume,
                        onSeek = viewModel::seekSound
                    )
                }
            }
        }

        if (isDownloaderOpen) {
            DownloaderDialog(
                downloadState = state.downloadState,
                onDownload = viewModel::downloadYoutube,
                onDismiss = { isDownloaderOpen = false }
            )
        }
    }
}

data class SidebarActions(
    val onFilterChange: (SoundFilter) -> Unit,
    val onSortChange: () -> Unit,
    val onVolumeChange: (Boolean, Float) -> Unit,
    val onMicChange: (Microphone) -> Unit,
    val onRefresh: () -> Unit,
    val onStopAll: () -> Unit,
    val onOpenDownloader: () -> Unit
)