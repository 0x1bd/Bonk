package org.kvxd.bonk.service

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.kvxd.bonk.model.ActiveSoundState
import org.kvxd.bonk.model.SoundFile
import org.kvxd.bonk.utils.MpvClient
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class AudioService(
    private val socketDir: File,
    private val mpvExecutable: String,
    private val scope: CoroutineScope
) {

    private val _activeSounds = MutableStateFlow<List<ActiveSoundState>>(emptyList())
    val activeSounds = _activeSounds.asStateFlow()

    private data class SoundRuntime(
        val processes: List<Process>,
        val localSocket: String?,
        val remoteSocket: String?,
        val job: Job
    )

    private val REMOTE_SINK_NAME = "Bonk_Sink"
    private val runtimeMap = ConcurrentHashMap<String, SoundRuntime>()

    init {
        startPoller()

        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            runtimeMap.values.forEach { runtime ->
                runtime.processes.forEach { if (it.isAlive) it.destroyForcibly() }
            }
        })
    }

    fun play(sound: SoundFile, masterLocal: Float, masterRemote: Float) {
        val id = UUID.randomUUID().toString().replace("-", "")

        scope.launch(Dispatchers.IO) {
            val localSocketPath = if (masterLocal > 0) File(socketDir, "${id}_local.sock").absolutePath else null

            val remoteSinkExists = checkPulseSinkExists(REMOTE_SINK_NAME)
            val remoteSocketPath = if (masterRemote > 0 && remoteSinkExists) {
                File(socketDir, "${id}_remote.sock").absolutePath
            } else {
                null
            }

            val processes = mutableListOf<Process>()
            var activeLocalSocket: String? = null
            var activeRemoteSocket: String? = null

            val startLocalVol = 100f
            val startRemoteVol = 100f

            try {
                localSocketPath?.let { sock ->
                    val effective = (masterLocal * startLocalVol) / 100f
                    spawnMpv(sound.file.absolutePath, null, effective, sock)?.let {
                        processes.add(it)
                        activeLocalSocket = sock
                    }
                }

                remoteSocketPath?.let { sock ->
                    val effective = (masterRemote * startRemoteVol) / 100f
                    spawnMpv(sound.file.absolutePath, REMOTE_SINK_NAME, effective, sock)?.let {
                        processes.add(it)
                        activeRemoteSocket = sock
                    }
                }

                if (processes.isEmpty()) return@launch

                val runtime = SoundRuntime(processes, activeLocalSocket, activeRemoteSocket, this.coroutineContext.job)
                runtimeMap[id] = runtime

                val state = ActiveSoundState(id, sound, 0f, false, startLocalVol, startRemoteVol)
                _activeSounds.update { it + state }

                while (isActive && processes.any { it.isAlive }) {
                    delay(250)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cleanupResources(id, processes, activeLocalSocket, activeRemoteSocket)
            }
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

        val runtime = runtimeMap[id] ?: return
        val socket = if (isLocal) runtime.localSocket else runtime.remoteSocket
        val effective = (masterVol * newPercent) / 100f

        socket?.let {
            MpvClient.sendCommand(it, """{ "command": ["set_property", "volume", $effective] }""")
        }
    }

    fun updateMasterVolume(isLocal: Boolean, newMasterVol: Float) {
        _activeSounds.value.forEach { sound ->
            val individualVol = if (isLocal) sound.localVolume else sound.remoteVolume
            setIndividualVolume(sound.id, isLocal, individualVol, newMasterVol)
        }
    }


    private fun cleanupResources(id: String, processes: List<Process>, localSocket: String?, remoteSocket: String?) {
        listOfNotNull(localSocket, remoteSocket).forEach {
            runCatching { MpvClient.sendCommand(it, """{ "command": ["quit"] }""") }
        }

        processes.forEach { if (it.isAlive) runCatching { it.destroyForcibly() } }
        localSocket?.let { runCatching { File(it).delete() } }
        remoteSocket?.let { runCatching { File(it).delete() } }

        runtimeMap.remove(id)
        _activeSounds.update { it.filter { s -> s.id != id } }
    }

    private fun spawnMpv(path: String, device: String?, vol: Float, socket: String): Process? {
        return try {
            val args = mutableListOf(
                mpvExecutable, "--no-terminal", "--ao=pulse", "--vid=no", "--audio-display=no",
                "--volume-max=250", "--volume=$vol", "--input-ipc-server=$socket", "--idle=no", path
            )
            if (device != null) args.add("--audio-device=pulse/$device")
            ProcessBuilder(args).redirectErrorStream(true).start()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun checkPulseSinkExists(sinkName: String): Boolean {
        return try {
            val proc = ProcessBuilder("pactl", "list", "short", "sinks").redirectErrorStream(true).start()
            val output = proc.inputStream.bufferedReader().use { it.readText() }

            proc.waitFor(1, TimeUnit.SECONDS)

            output.contains(sinkName)
        } catch (e: Exception) {
            false
        }
    }

    fun stop(id: String) {
        runtimeMap[id]?.job?.cancel()
    }

    fun stopAll() {
        runtimeMap.values.forEach { it.job.cancel() }
    }

    fun seek(id: String, percent: Float) {
        _activeSounds.update { list -> list.map { if (it.id == id) it.copy(progress = percent) else it } }
        val runtime = runtimeMap[id] ?: return
        val mpvPercent = percent * 100

        listOfNotNull(runtime.localSocket, runtime.remoteSocket).forEach {
            MpvClient.sendCommand(it, """{ "command": ["seek", $mpvPercent, "absolute-percent"] }""")
        }
    }

    fun togglePause(id: String) {
        val sound = _activeSounds.value.find { it.id == id } ?: return
        val newPaused = !sound.isPaused

        _activeSounds.update { list -> list.map { if (it.id == id) it.copy(isPaused = newPaused) else it } }

        val runtime = runtimeMap[id] ?: return
        val boolStr = if (newPaused) "yes" else "no"

        listOfNotNull(runtime.localSocket, runtime.remoteSocket).forEach {
            MpvClient.sendCommand(it, """{ "command": ["set_property", "pause", "$boolStr"] }""")
        }
    }

    private fun startPoller() {
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(250)
                val currentIds = runtimeMap.keys.toList()
                if (currentIds.isEmpty()) continue
                val updates = mutableMapOf<String, Float>()
                currentIds.forEach { id ->
                    val runtime = runtimeMap[id]
                    val socket = runtime?.localSocket ?: runtime?.remoteSocket
                    if (socket != null && File(socket).exists()) {
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