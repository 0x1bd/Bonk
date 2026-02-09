package org.kvxd.bonk.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.kvxd.bonk.Globals
import org.kvxd.bonk.utils.ShellUtils
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

data class DownloadProgress(
    val url: String,
    val progress: Float = 0f,
    val status: String = "Idle",
    val isActive: Boolean = false,
    val log: String = ""
)

class YtDlpService(private val soundsDir: File) {

    val downloadState = MutableStateFlow(DownloadProgress(""))

    suspend fun updateBinary() = withContext(Dispatchers.IO) {
        val bin = Globals.YT_DLP_BIN
        try {
            if (!bin.exists()) {
                downloadState.value = downloadState.value.copy(status = "Downloading yt-dlp...")
                val url = URI("https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp").toURL()
                url.openStream().use { input -> bin.outputStream().use { output -> input.copyTo(output) } }

                runCatching {
                    val perms = PosixFilePermissions.fromString("rwxr-xr-x")
                    Files.setPosixFilePermissions(bin.toPath(), perms)
                }
            } else {
                downloadState.value = downloadState.value.copy(status = "Checking for yt-dlp updates...")
                ShellUtils.runCommand(listOf(bin.absolutePath, "-U"))
            }
        } catch (e: Exception) {
            downloadState.value = downloadState.value.copy(status = "Update failed: ${e.message}")
        }
    }

    suspend fun downloadAudio(url: String, format: String = "mp3") = withContext(Dispatchers.IO) {
        updateBinary()

        downloadState.value = DownloadProgress(url, 0f, "Starting...", true)

        val cmd = listOf(
            Globals.YT_DLP_BIN.absolutePath,
            "-x",
            "--audio-format", format,
            "--audio-quality", "0",
            "-o", "${soundsDir.absolutePath}/%(title)s.%(ext)s",
            "--newline",
            url
        )

        try {
            val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
            process.inputStream.bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val output = line ?: ""
                    val progress = parseProgress(output)

                    downloadState.value = downloadState.value.copy(
                        progress = progress ?: downloadState.value.progress,
                        status = if (output.contains("[ExtractAudio]")) "Converting..." else "Downloading...",
                        log = downloadState.value.log + "\n" + output
                    )
                }
            }
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                downloadState.value = downloadState.value.copy(status = "Finished", isActive = false, progress = 1f)
            } else {
                downloadState.value = downloadState.value.copy(status = "Error (Exit $exitCode)", isActive = false)
            }
        } catch (e: Exception) {
            downloadState.value = downloadState.value.copy(status = "Failed: ${e.message}", isActive = false)
        }
    }

    private fun parseProgress(line: String): Float? {
        val regex = Regex("""\[download\]\s+(\d+\.\d+)%""")
        return regex.find(line)?.groupValues?.get(1)?.toFloatOrNull()?.div(100f)
    }
}