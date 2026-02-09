package org.kvxd.bonk.utils

import java.io.File

object DependencyChecker {

    fun checkMissing(): List<String> {
        val missing = mutableListOf<String>()

        if (findBinary("mpv") == null) missing.add("mpv")
        if (findBinary("pactl") == null) missing.add("pulseaudio-utils (pactl)")
        if (findBinary("ffmpeg") == null) missing.add("ffmpeg")

        return missing
    }

    fun findBinary(name: String): String? {
        val paths = listOf("/usr/bin/", "/bin/", "/usr/local/bin/", "/usr/sbin/")

        for (path in paths) {
            val file = File(path + name)
            if (file.exists() && file.canExecute()) return file.absolutePath
        }

        val systemPath = System.getenv("PATH") ?: ""
        for (path in systemPath.split(File.pathSeparator)) {
            val file = File(path, name)
            if (file.exists() && file.canExecute()) return file.absolutePath
        }

        return null
    }
}