package org.kvxd.bonk.ui

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
import org.kvxd.bonk.Globals
import org.kvxd.bonk.controller.SoundboardController
import org.kvxd.bonk.controller.UiState
import org.kvxd.bonk.ui.sections.DownloaderDialog
import org.kvxd.bonk.ui.sections.PlayerPanel
import org.kvxd.bonk.ui.sections.Sidebar
import org.kvxd.bonk.ui.sections.SoundBrowser
import org.kvxd.bonk.ui.theme.AppColors

@Composable
fun MainScreen(state: UiState) {
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
                    onOpenDownloader = { isDownloaderOpen = true }
                )

                Box(Modifier.width(1.dp).fillMaxHeight().background(AppColors.SurfaceLight))

                SoundBrowser(
                    currentDir = state.currentDir,
                    sounds = state.sounds,
                    searchQuery = state.searchQuery,
                    isHome = state.currentDir.absolutePath == Globals.SOUNDS_DIR.absolutePath,
                    onSearch = { SoundboardController.setSearch(it) },
                    onNavigate = { SoundboardController.navigate(it) },
                    onNavigateUp = { SoundboardController.navigateUp() },
                    onPlay = { SoundboardController.playSound(it) },
                    onToggleFav = { SoundboardController.toggleFavorite(it) }
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
                        onTogglePause = { SoundboardController.togglePause(it) },
                        onStop = { SoundboardController.stopSound(it) },
                        onVolumeChange = { id, local, vol -> SoundboardController.setSoundVolume(id, local, vol) },
                        onSeek = { id, pos -> SoundboardController.seekSound(id, pos) }
                    )
                }
            }
        }

        if (isDownloaderOpen) {
            DownloaderDialog(
                downloadState = state.downloadState,
                onDownload = { SoundboardController.downloadYoutube(it) },
                onDismiss = { isDownloaderOpen = false }
            )
        }
    }
}