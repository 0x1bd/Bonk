package org.kvxd.bonk.service

import org.kvxd.bonk.utils.ShellUtils
import org.kvxd.bonk.model.Microphone
import kotlin.concurrent.thread

object PulseAudioSystem {

    private const val SINK_NAME = "Bonk_Sink"
    private const val SOURCE_NAME = "Bonk_Mic"

    fun setup(preferredMicId: String?) {
        unloadAllBonkModules()

        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            unloadAllBonkModules()
        })

        loadModule(
            "module-null-sink",
            "sink_name=$SINK_NAME",
            "sink_properties=device.description='Bonk_Internal_Mixer'"
        )

        ShellUtils.runCommand(listOf("pactl", "suspend-sink", SINK_NAME, "0"))
        ShellUtils.runCommand(listOf("pactl", "set-sink-mute", SINK_NAME, "0"))
        ShellUtils.runCommand(listOf("pactl", "set-sink-volume", SINK_NAME, "100%"))

        try { Thread.sleep(100) } catch (e: Exception) {}

        loadModule(
            "module-remap-source",
            "master=$SINK_NAME.monitor",
            "source_name=$SOURCE_NAME",
            "source_properties=device.description='Bonk_Microphone'"
        )

        ShellUtils.runCommand(listOf("pactl", "set-source-mute", "$SINK_NAME.monitor", "0"))
        ShellUtils.runCommand(listOf("pactl", "set-source-mute", SOURCE_NAME, "0"))
        ShellUtils.runCommand(listOf("pactl", "set-source-volume", SOURCE_NAME, "100%"))

        setupLoopback(preferredMicId)
    }

    fun setupLoopback(micId: String?) {
        unloadLoopbacksTargetingBonk()

        val source = if (micId.isNullOrEmpty()) {
            ShellUtils.runCommand(listOf("pactl", "get-default-source")).trim()
        } else {
            micId
        }

        if (source.isNotEmpty() && !source.contains(SINK_NAME) && !source.contains(SOURCE_NAME)) {
            loadModule(
                "module-loopback",
                "source=$source",
                "sink=$SINK_NAME",
                "latency_msec=20"
            )
        }
    }

    fun unloadAllBonkModules() {
        val raw = ShellUtils.runCommand(listOf("pactl", "list", "short", "modules"))
        raw.lines().forEach { line ->
            if (line.contains(SINK_NAME) || line.contains(SOURCE_NAME)) {
                val id = line.trim().split("\\s+".toRegex()).firstOrNull()
                id?.let {
                    ShellUtils.runCommand(listOf("pactl", "unload-module", it))
                }
            }
        }
    }

    private fun unloadLoopbacksTargetingBonk() {
        val raw = ShellUtils.runCommand(listOf("pactl", "list", "short", "modules"))
        raw.lines().forEach { line ->
            if (line.contains("module-loopback") && line.contains(SINK_NAME)) {
                val id = line.trim().split("\\s+".toRegex()).firstOrNull()
                id?.let {
                    ShellUtils.runCommand(listOf("pactl", "unload-module", it))
                }
            }
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
        val isOurDesc = desc.contains("Bonk_Microphone") || desc.contains("Bonk_Internal_Mixer")

        if (!isInternalMonitor && !isOurSink && !isOurSource && !isOurDesc) {
            list.add(Microphone(id = name, description = desc))
        }
    }

    private fun loadModule(name: String, vararg args: String) {
        val command = mutableListOf("pactl", "load-module", name)
        command.addAll(args)
        ShellUtils.runCommand(command)
    }
}