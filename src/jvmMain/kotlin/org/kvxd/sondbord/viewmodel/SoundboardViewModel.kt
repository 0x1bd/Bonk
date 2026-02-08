package org.kvxd.sondbord.viewmodel

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.kvxd.sondbord.model.*
import org.kvxd.sondbord.service.AudioService
import org.kvxd.sondbord.service.DownloadProgress
import org.kvxd.sondbord.service.PulseAudioSystem
import org.kvxd.sondbord.service.SettingsRepository
import org.kvxd.sondbord.service.YtDlpService
import java.io.File

sealed class AppState {
    data class Error(val missingDeps: List<String>) : AppState()
    data class Ready(val uiState: UiState) : AppState()
}

data class UiState(
    val currentDir: File,
    val sounds: List<SoundFile> = emptyList(),
    val activeSounds: List<ActiveSoundState> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val microphones: List<Microphone> = emptyList(),
    val filterMode: SoundFilter = SoundFilter.All,
    val searchQuery: String = "",
    val downloadState: DownloadProgress = DownloadProgress("")
)

class SoundboardViewModel(
    private val settingsRepo: SettingsRepository,
    private val soundsDir: File,
    socketDir: File,
    missingDependencies: List<String>
) {

    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val audioService: AudioService? = if (missingDependencies.isEmpty()) {
        AudioService(socketDir, viewModelScope)
    } else null

    private val ytDlpService = YtDlpService(soundsDir)

    private val _settings = MutableStateFlow(settingsRepo.load())
    private val _currentDir = MutableStateFlow(soundsDir)
    private val _filterMode = MutableStateFlow(SoundFilter.All)
    private val _searchQuery = MutableStateFlow("")
    private val _refreshTrigger = MutableStateFlow(0)
    private val _microphones = MutableStateFlow<List<Microphone>>(emptyList())

    val appState: StateFlow<AppState> = if (missingDependencies.isNotEmpty()) {
        MutableStateFlow(AppState.Error(missingDependencies)).asStateFlow()
    } else {
        combineUiState().map { AppState.Ready(it) }
            .stateIn(viewModelScope, SharingStarted.Lazily, AppState.Ready(UiState(soundsDir)))
    }

    init {
        if (missingDependencies.isEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                refreshMicrophones()
                PulseAudioSystem.setup(_settings.value.inputDeviceId)
            }
        }
    }

    private data class UiContext(
        val settings: AppSettings,
        val mics: List<Microphone>,
        val filter: SoundFilter,
        val query: String,
        val dir: File
    )

    private fun combineUiState(): Flow<UiState> {
        val fileFlow = combine(
            _currentDir,
            _settings.map { it.favorites to it.sortMode }.distinctUntilChanged(),
            _filterMode,
            _searchQuery,
            _refreshTrigger
        ) { dir, (favs, sort), filter, query, _ ->
            loadFiles(dir, AppSettings(favorites = favs, sortMode = sort), filter, query)
        }.flowOn(Dispatchers.IO)

        val contextFlow = combine(
            _settings,
            _microphones,
            _filterMode,
            _searchQuery,
            _currentDir
        ) { settings, mics, filter, query, dir ->
            UiContext(settings, mics, filter, query, dir)
        }

        val activeSoundsFlow = audioService?.activeSounds ?: flowOf(emptyList())

        return combine(
            fileFlow,
            activeSoundsFlow,
            contextFlow,
            ytDlpService.downloadState
        ) { files, active, ctx, download ->
            UiState(
                currentDir = ctx.dir,
                sounds = files,
                activeSounds = active,
                settings = ctx.settings,
                microphones = ctx.mics,
                filterMode = ctx.filter,
                searchQuery = ctx.query,
                downloadState = download
            )
        }
    }

    fun playSound(sound: SoundFile) {
        val s = _settings.value
        audioService?.play(sound, s.localVolume, s.remoteVolume)
    }

    fun stopSound(id: String) = audioService?.stop(id)
    fun stopAll() = audioService?.stopAll()
    fun togglePause(id: String) = audioService?.togglePause(id)

    fun setMasterVolume(local: Boolean, volume: Float) {
        updateSettings { if (local) it.copy(localVolume = volume) else it.copy(remoteVolume = volume) }
        audioService?.updateMasterVolume(local, volume)
    }

    fun setSoundVolume(id: String, local: Boolean, volume: Float) {
        val master = if (local) _settings.value.localVolume else _settings.value.remoteVolume
        audioService?.setIndividualVolume(id, local, volume, master)
    }

    fun setMicrophone(mic: Microphone) {
        updateSettings { it.copy(inputDeviceId = mic.id) }
        viewModelScope.launch(Dispatchers.IO) { PulseAudioSystem.setupLoopback(mic.id) }
    }

    fun toggleFavorite(sound: SoundFile) {
        updateSettings { current ->
            val newFavs =
                if (current.favorites.contains(sound.id)) current.favorites - sound.id else current.favorites + sound.id
            current.copy(favorites = newFavs)
        }
    }

    fun navigate(dir: File) {
        if (dir.isDirectory) _currentDir.value = dir
    }

    fun navigateUp() {
        val parent = _currentDir.value.parentFile
        if (parent != null && parent.absolutePath.startsWith(soundsDir.absolutePath)) {
            _currentDir.value = parent
        } else {
            _currentDir.value = soundsDir
        }
    }

    fun setFilter(mode: SoundFilter) {
        _filterMode.value = mode
    }

    fun setSearch(query: String) {
        _searchQuery.value = query
    }

    fun toggleSort() {
        updateSettings { it.copy(sortMode = if (it.sortMode == SortMode.Name) SortMode.LastAdded else SortMode.Name) }
    }

    fun refreshFiles() {
        _refreshTrigger.value += 1
        viewModelScope.launch(Dispatchers.IO) { refreshMicrophones() }
    }

    private suspend fun refreshMicrophones() {
        _microphones.value = PulseAudioSystem.getMicrophones()
    }

    fun onCleanup() {
        audioService?.stopAll()
        PulseAudioSystem.cleanup()
    }

    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val newSettings = transform(_settings.value)
        _settings.value = newSettings
        viewModelScope.launch(Dispatchers.IO) { settingsRepo.save(newSettings) }
    }

    private fun loadFiles(dir: File, settings: AppSettings, filter: SoundFilter, query: String): List<SoundFile> {
        val supported = setOf("mp3", "wav", "ogg", "flac", "m4a")
        val candidates = if (query.isNotEmpty() || filter == SoundFilter.Favorites) {
            soundsDir.walk().filter { it.isFile }
        } else {
            dir.walk().maxDepth(1).filter { it != dir }
        }

        return candidates
            .filter { (it.isDirectory && query.isEmpty() && filter == SoundFilter.All) || (it.extension.lowercase() in supported) }
            .map { SoundFile(it, settings.favorites.contains(it.absolutePath)) }
            .filter { s ->
                (query.isEmpty() || s.name.contains(
                    query,
                    true
                )) && (filter == SoundFilter.All || s.isFavorite)
            }
            .sortedWith { a, b ->
                if (a.file.isDirectory != b.file.isDirectory) {
                    if (a.file.isDirectory) -1 else 1
                } else {
                    when (settings.sortMode) {
                        SortMode.Name -> a.name.compareTo(b.name, true)
                        SortMode.LastAdded -> b.file.lastModified().compareTo(a.file.lastModified())
                    }
                }
            }.toList()
    }

    fun downloadYoutube(url: String) {
        viewModelScope.launch {
            ytDlpService.downloadAudio(url)
            refreshFiles()
        }
    }
}