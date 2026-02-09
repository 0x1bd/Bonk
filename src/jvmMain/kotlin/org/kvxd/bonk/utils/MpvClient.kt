package org.kvxd.bonk.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Path

object MpvClient {

    private val json = Json { ignoreUnknownKeys = true }

    fun sendCommand(socketPath: String, command: String) {
        if (!waitForSocket(socketPath)) return
        runCatching {
            val addr = UnixDomainSocketAddress.of(socketPath)
            SocketChannel.open(addr).use { channel ->
                val payload = if (command.endsWith("\n")) command else "$command\n"
                channel.write(ByteBuffer.wrap(payload.toByteArray()))
            }
        }
    }

    fun getProperty(socketPath: String, property: String): String? {
        if (!waitForSocket(socketPath)) return null
        return runCatching {
            val cmd = "{ \"command\": [\"get_property\", \"$property\"] }\n"
            val addr = UnixDomainSocketAddress.of(socketPath)
            SocketChannel.open(addr).use { channel ->
                channel.write(ByteBuffer.wrap(cmd.toByteArray()))
                val buffer = ByteBuffer.allocate(2048)
                val read = channel.read(buffer)
                if (read > 0) String(buffer.array(), 0, read) else null
            }
        }.getOrNull()
    }

    fun parsePercentage(jsonResponse: String?): Float? {
        if (jsonResponse == null || !jsonResponse.contains("data")) return null
        return try {
            val element = json.parseToJsonElement(jsonResponse)
            element.jsonObject["data"]?.jsonPrimitive?.floatOrNull?.div(100f)
        } catch (_: Exception) {
            null
        }
    }

    private fun waitForSocket(path: String, retries: Int = 3): Boolean {
        var attempts = 0
        val p = Path.of(path)
        while (attempts < retries) {
            if (Files.exists(p)) return true
            try {
                Thread.sleep(50)
            } catch (_: Exception) {
            }
            attempts++
        }
        return Files.exists(p)
    }
}