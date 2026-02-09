package org.kvxd.bonk.service

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.kvxd.bonk.utils.MpvClient
import org.kvxd.bonk.model.ActiveSoundState
import org.kvxd.bonk.model.SoundFile
import java.io.File
import java.util.UUID

class AudioService(
    private val socketDir: File,
    private val scope: CoroutineScope
) {

    private val _activeSounds = MutableStateFlow<List<ActiveSoundState>>(emptyList())
    val activeSounds = _activeSounds.asStateFlow()

    private data class SoundRuntime(
        val processes: List<Process>,
        val localSocket: String?,
        val remoteSocket: String?
    )

    private val runtimeMap = mutableMapOf<String, SoundRuntime>()

    init {
        startPoller()
    }

    fun play(sound: SoundFile, masterLocal: Float, masterRemote: Float) {
        val id = UUID.randomUUID().toString().replace("-", "")

        scope.launch(Dispatchers.IO) {
            val localSocket = if (masterLocal > 0) File(socketDir, "${id}_local.sock").absolutePath else null
            val remoteSocket = if (masterRemote > 0) File(socketDir, "${id}_remote.sock").absolutePath else null

            val processes = mutableListOf<Process>()
            val startLocalVol = 100f
            val startRemoteVol = 100f

            localSocket?.let { sock ->
                val eff = (masterLocal * startLocalVol) / 100f
                spawnMpv(sound.file.absolutePath, null, eff, sock)?.let { processes.add(it) }
            }

            remoteSocket?.let { sock ->
                val eff = (masterRemote * startRemoteVol) / 100f
                spawnMpv(sound.file.absolutePath, "Soundboard_Mixer_Sink", eff, sock)?.let { processes.add(it) }
            }

            if (processes.isEmpty()) return@launch

            val state = ActiveSoundState(id, sound, 0f, false, startLocalVol, startRemoteVol)

            synchronized(runtimeMap) {
                runtimeMap[id] = SoundRuntime(processes, localSocket, remoteSocket)
            }
            _activeSounds.update { it + state }

            processes.forEach { runCatching { it.waitFor() } }
            cleanupSound(id)
        }
    }

    private fun spawnMpv(path: String, device: String?, vol: Float, socket: String): Process? {
        return try {
            val args = mutableListOf(
                "mpv", "--no-terminal", "--ao=pulse", "--vid=no", "--audio-display=no",
                "--volume-max=250", "--volume=$vol", "--input-ipc-server=$socket",
                "--idle=no",
                path
            )
            if (device != null) args.add("--audio-device=pulse/$device")
            ProcessBuilder(args).start()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun stop(id: String) {
        scope.launch(Dispatchers.IO) {
            val runtime = synchronized(runtimeMap) { runtimeMap[id] } ?: return@launch
            listOfNotNull(runtime.localSocket, runtime.remoteSocket).forEach {
                MpvClient.sendCommand(it, """{ "command": ["quit"] }""")
            }

            delay(100)
            runtime.processes.forEach { if (it.isAlive) it.destroyForcibly() }
            cleanupSound(id)
        }
    }

    fun stopAll() {
        val ids = _activeSounds.value.map { it.id }
        ids.forEach { stop(it) }
    }

    fun seek(id: String, percent: Float) {
        _activeSounds.update { list ->
            list.map { if (it.id == id) it.copy(progress = percent) else it }
        }

        val runtime = synchronized(runtimeMap) { runtimeMap[id] } ?: return
        val mpvPercent = percent * 100

        listOfNotNull(runtime.localSocket, runtime.remoteSocket).forEach {
            MpvClient.sendCommand(it, """{ "command": ["seek", $mpvPercent, "absolute-percent"] }""")
        }
    }

    fun togglePause(id: String) {
        val sound = _activeSounds.value.find { it.id == id } ?: return
        val newPaused = !sound.isPaused

        _activeSounds.update { list -> list.map { if (it.id == id) it.copy(isPaused = newPaused) else it } }

        val runtime = synchronized(runtimeMap) { runtimeMap[id] } ?: return
        val boolStr = if (newPaused) "yes" else "no"
        listOfNotNull(runtime.localSocket, runtime.remoteSocket).forEach {
            MpvClient.sendCommand(it, """{ "command": ["set_property", "pause", "$boolStr"] }""")
        }
    }

    fun updateMasterVolume(isLocal: Boolean, newMasterVol: Float) {
        _activeSounds.value.forEach { sound ->
            val baseVol = if (isLocal) sound.localVolume else sound.remoteVolume
            setIndividualVolume(sound.id, isLocal, baseVol, newMasterVol)
        }
    }

    fun setIndividualVolume(id: String, isLocal: Boolean, newPercent: Float, masterVol: Float) {
        _activeSounds.update { list ->
            list.map {
                if (it.id == id) {
                    if (isLocal) it.copy(localVolume = newPercent) else it.copy(remoteVolume = newPercent)
                } else it
            }
        }
        val runtime = synchronized(runtimeMap) { runtimeMap[id] } ?: return
        val socket = if (isLocal) runtime.localSocket else runtime.remoteSocket
        val effective = (masterVol * newPercent) / 100f
        socket?.let { MpvClient.sendCommand(it, """{ "command": ["set_property", "volume", $effective] }""") }
    }

    private fun cleanupSound(id: String) {
        val runtime = synchronized(runtimeMap) { runtimeMap.remove(id) }
        runtime?.localSocket?.let { runCatching { File(it).delete() } }
        runtime?.remoteSocket?.let { runCatching { File(it).delete() } }
        _activeSounds.update { it.filter { s -> s.id != id } }
    }

    private fun startPoller() {
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(250)
                val currentIds = _activeSounds.value.map { it.id }
                if (currentIds.isEmpty()) continue

                val updates = mutableMapOf<String, Float>()
                currentIds.forEach { id ->
                    val runtime = synchronized(runtimeMap) { runtimeMap[id] }
                    val socket = runtime?.localSocket ?: runtime?.remoteSocket
                    if (socket != null) {
                        val resp = MpvClient.getProperty(socket, "percent-pos")
                        MpvClient.parsePercentage(resp)?.let { updates[id] = it }
                    }
                }
                if (updates.isNotEmpty()) {
                    _activeSounds.update { list ->
                        list.map { item -> updates[item.id]?.let { item.copy(progress = it) } ?: item }
                    }
                }
            }
        }
    }
}