package org.kvxd.bonk.utils

object ShellUtils {

    fun runCommand(args: List<String>): String = runCatching {
        val proc = ProcessBuilder(args).redirectErrorStream(true).start()
        proc.waitFor()
        proc.inputStream.bufferedReader().use { it.readText() }
    }.getOrDefault("")
}