package org.kvxd.bonk.controller

import org.kvxd.bonk.model.*
import org.kvxd.bonk.service.*
import java.io.File

data class UiState(
    val currentDir: File,
    val sounds: List<SoundFile> = emptyList(),
    val activeSounds: List<ActiveSoundState> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val microphones: List<Microphone> = emptyList(),
    val filterMode: SoundFilter = SoundFilter.All,
    val searchQuery: String = "",
    val downloadState: DownloadProgress = DownloadProgress(""),
    val isDownloaderAvailable: Boolean = false,
    val error: String? = null
)