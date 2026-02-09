package org.kvxd.bonk.service

import kotlinx.serialization.json.Json
import org.kvxd.bonk.model.AppSettings
import java.io.File

class SettingsRepository(private val file: File) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun load(): AppSettings {
        if (!file.exists()) return AppSettings()
        return try {
            json.decodeFromString(file.readText())
        } catch (_: Exception) {
            AppSettings()
        }
    }

    fun save(settings: AppSettings) {
        try {
            file.parentFile.mkdirs()
            file.writeText(json.encodeToString(settings))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}