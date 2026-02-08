package org.kvxd.sondbord.infra

import java.io.IOException

object DependencyChecker {

    fun checkMissing(): List<String> {
        val missing = mutableListOf<String>()

        if (!isCommandAvailable("mpv", "--version")) {
            missing.add("mpv")
        }

        if (!isCommandAvailable("pactl", "--version")) {
            missing.add("pulseaudio-utils (pactl)")
        }

        return missing
    }

    private fun isCommandAvailable(cmd: String, arg: String): Boolean {
        return try {
            val process = ProcessBuilder(cmd, arg).start()
            val code = process.waitFor()
            code == 0
        } catch (e: IOException) {
            false
        } catch (e: Exception) {
            false
        }
    }
}