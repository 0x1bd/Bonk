package org.kvxd.bonk.service

import org.kvxd.bonk.infra.ShellUtils
import org.kvxd.bonk.model.Microphone

object PulseAudioSystem {

    private const val SINK_NAME = "Soundboard_Mixer_Sink"
    private const val SOURCE_NAME = "Soundboard_Source_Mic"

    private var loopbackModuleId: String? = null
    private val otherModuleIds = mutableListOf<String>()

    fun setup(preferredMicId: String?) {
        val check = ShellUtils.runCommand(listOf("pactl", "list", "short", "sinks"))
        if (!check.contains(SINK_NAME)) {
            loadModule(
                "module-null-sink",
                "sink_name=$SINK_NAME",
                "sink_properties=device.description='Soundboard_Internal_Mixer'"
            )?.let { otherModuleIds.add(it) }
        }
        ShellUtils.runCommand(listOf("pactl", "set-sink-volume", SINK_NAME, "100%"))

        loadModule(
            "module-remap-source",
            "master=$SINK_NAME.monitor",
            "source_name=$SOURCE_NAME",
            "source_properties=device.description='Soundboard_Final_Mic'"
        )?.let { otherModuleIds.add(it) }

        setupLoopback(preferredMicId)
    }

    fun setupLoopback(micId: String?) {
        loopbackModuleId?.let { ShellUtils.runCommand(listOf("pactl", "unload-module", it)) }
        loopbackModuleId = null

        val source = if (micId.isNullOrEmpty()) {
            ShellUtils.runCommand(listOf("pactl", "get-default-source")).trim()
        } else {
            micId
        }

        if (source.isNotEmpty() && !source.contains(SINK_NAME) && !source.contains(SOURCE_NAME)) {
            loopbackModuleId = loadModule("module-loopback", "source=$source", "sink=$SINK_NAME", "latency_msec=1")
        }
    }

    fun getMicrophones(): List<Microphone> {
        val raw = ShellUtils.runCommand(listOf("pactl", "list", "sources"))
        val mics = mutableListOf<Microphone>()

        val lines = raw.lines()
        var currentName: String? = null
        var currentDesc: String? = null

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("Name: ")) {
                if (currentName != null && currentDesc != null) {
                    addMicIfValid(mics, currentName, currentDesc)
                }
                currentName = trimmed.substringAfter("Name: ").trim()
                currentDesc = null
            } else if (trimmed.startsWith("device.description = ")) {
                currentDesc = trimmed.substringAfter("=").trim().removeSurrounding("\"")
            }
        }
        if (currentName != null && currentDesc != null) {
            addMicIfValid(mics, currentName, currentDesc)
        }
        return mics
    }

    private fun addMicIfValid(list: MutableList<Microphone>, name: String?, desc: String?) {
        if (name == null || desc == null) return

        val isInternalMonitor = name.endsWith(".monitor")
        val isOurSink = name.contains(SINK_NAME)
        val isOurSource = name.contains(SOURCE_NAME)
        val isOurDesc = desc.contains("Soundboard_Final_Mic") || desc.contains("Soundboard_Internal_Mixer")

        if (!isInternalMonitor && !isOurSink && !isOurSource && !isOurDesc) {
            list.add(Microphone(id = name, description = desc))
        }
    }

    private fun loadModule(name: String, vararg args: String): String? {
        val command = mutableListOf("pactl", "load-module", name)
        command.addAll(args)
        return ShellUtils.runCommand(command).trim().toIntOrNull()?.toString()
    }

    fun cleanup() {
        loopbackModuleId?.let { ShellUtils.runCommand(listOf("pactl", "unload-module", it)) }
        otherModuleIds.forEach { ShellUtils.runCommand(listOf("pactl", "unload-module", it)) }
        otherModuleIds.clear()
        ShellUtils.runCommand(listOf("pactl", "unload-module", "module-remap-source"))
    }
}