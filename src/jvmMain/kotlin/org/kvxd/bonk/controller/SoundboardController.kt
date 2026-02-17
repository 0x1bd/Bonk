package org.kvxd.bonk.controller

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.kvxd.bonk.model.*
import org.kvxd.bonk.service.*
import org.kvxd.bonk.utils.DependencyChecker
import java.io.File

object SoundboardController {

    private var scope = CoroutineScope(Dispatchers.Main)
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var soundsDir: File

    private var audioService: AudioService? = null
    private lateinit var ytDlpService: YtDlpService

    private val _uiState = MutableStateFlow(UiState(File("/")))
    val uiState = _uiState.asStateFlow()

    fun initialize(
        appScope: CoroutineScope,
        repo: SettingsRepository,
        baseDir: File,
        socketDir: File,
        missingDeps: List<String>
    ) {
        scope = appScope
        settingsRepo = repo
        soundsDir = baseDir

        if (missingDeps.isNotEmpty()) {
            _uiState.update { it.copy(error = "Missing: ${missingDeps.joinToString(", ")}") }
            return
        }

        val mpvPath = DependencyChecker.findBinary("mpv")
        if (mpvPath != null) {
            audioService = AudioService(socketDir, mpvPath, scope)
        }
        ytDlpService = YtDlpService(soundsDir)
        val hasYtDlp = DependencyChecker.findBinary("yt-dlp") != null

        val savedSettings = repo.load()
        _uiState.update {
            it.copy(
                currentDir = baseDir,
                settings = savedSettings,
                isDownloaderAvailable = hasYtDlp
            )
        }

        startServiceCollectors()

        scope.launch(Dispatchers.IO) {
            val mics = PulseAudioSystem.getMicrophones()
            _uiState.update { it.copy(microphones = mics) }
            PulseAudioSystem.setup(savedSettings.inputDeviceId)
            reloadFileList()
        }
    }

    private fun startServiceCollectors() {
        audioService?.let { service ->
            scope.launch {
                service.activeSounds.collect { active ->
                    _uiState.update { it.copy(activeSounds = active) }
                }
            }
        }

        scope.launch {
            ytDlpService.downloadState.collect { progress ->
                _uiState.update { it.copy(downloadState = progress) }
            }
        }
    }

    fun playSound(sound: SoundFile) {
        val s = _uiState.value.settings
        audioService?.play(sound, s.localVolume, s.remoteVolume)
    }

    fun stopSound(id: String) = audioService?.stop(id)
    fun stopAll() = audioService?.stopAll()
    fun togglePause(id: String) = audioService?.togglePause(id)
    fun seekSound(id: String, percent: Float) = audioService?.seek(id, percent)

    fun setMasterVolume(local: Boolean, volume: Float) {
        updateSettings { if (local) it.copy(localVolume = volume) else it.copy(remoteVolume = volume) }
        audioService?.updateMasterVolume(local, volume)
    }

    fun setSoundVolume(id: String, local: Boolean, volume: Float) {
        val s = _uiState.value.settings
        val master = if (local) s.localVolume else s.remoteVolume
        audioService?.setIndividualVolume(id, local, volume, master)
    }

    fun setMicrophone(mic: Microphone) {
        updateSettings { it.copy(inputDeviceId = mic.id) }
        scope.launch(Dispatchers.IO) { PulseAudioSystem.setupLoopback(mic.id) }
    }

    fun toggleFavorite(sound: SoundFile) {
        updateSettings { current ->
            val newFavs = if (current.favorites.contains(sound.id)) {
                current.favorites - sound.id
            } else {
                current.favorites + sound.id
            }
            current.copy(favorites = newFavs)
        }
        reloadFileList()
    }

    fun navigate(dir: File) {
        if (dir.isDirectory) {
            _uiState.update { it.copy(currentDir = dir) }
            reloadFileList()
        }
    }

    fun navigateUp() {
        val current = _uiState.value.currentDir
        val parent = current.parentFile
        if (parent != null && parent.absolutePath.startsWith(soundsDir.absolutePath)) {
            navigate(parent)
        } else {
            navigate(soundsDir)
        }
    }

    fun setFilter(mode: SoundFilter) {
        _uiState.update { it.copy(filterMode = mode) }
        reloadFileList()
    }

    fun setSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        reloadFileList()
    }

    fun toggleSort() {
        updateSettings {
            it.copy(sortMode = if (it.sortMode == SortMode.Name) SortMode.LastAdded else SortMode.Name)
        }
        reloadFileList()
    }

    fun refreshFiles() {
        scope.launch(Dispatchers.IO) {
            val mics = PulseAudioSystem.getMicrophones()
            _uiState.update { it.copy(microphones = mics) }
            reloadFileList()
        }
    }

    fun downloadYoutube(url: String) {
        scope.launch {
            ytDlpService.downloadAudio(url)
            refreshFiles()
        }
    }

    fun onCleanup() {
        audioService?.stopAll()
        PulseAudioSystem.unloadAllBonkModules()
    }

    private fun reloadFileList() {
        scope.launch(Dispatchers.IO) {
            val s = _uiState.value

            val files = LibraryService.loadFiles(
                rootDir = soundsDir,
                currentDir = s.currentDir,
                settings = s.settings,
                filter = s.filterMode,
                query = s.searchQuery
            )

            _uiState.update { it.copy(sounds = files) }
        }
    }

    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        _uiState.update {
            val newSettings = transform(it.settings)
            it.copy(settings = newSettings)
        }
        scope.launch(Dispatchers.IO) {
            settingsRepo.save(_uiState.value.settings)
        }
    }
}