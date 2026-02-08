package org.kvxd.sondbord.infra

object DependencyChecker {

    fun checkMissing(): List<String> {
        val missing = mutableListOf<String>()

        if (!isCommandAvailable("mpv", "--version")) missing.add("mpv")
        if (!isCommandAvailable("pactl", "--version")) missing.add("pulseaudio-utils (pactl)")
        if (!isCommandAvailable("ffmpeg", "-version")) missing.add("ffmpeg")

        return missing
    }

    private fun isCommandAvailable(cmd: String, arg: String): Boolean {
        return try {
            val process = ProcessBuilder(cmd, arg).start()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
}