package org.kvxd.sondbord

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.kvxd.sondbord.infra.DependencyChecker
import org.kvxd.sondbord.service.SettingsRepository
import org.kvxd.sondbord.ui.AppContent
import org.kvxd.sondbord.viewmodel.SoundboardViewModel
import java.io.File

object Globals {

    private val USER_HOME = System.getProperty("user.home")
    val APP_DIR = File("$USER_HOME/.sondbord")
    val SOUNDS_DIR = File(APP_DIR, "sounds")
    val CONFIG_FILE = File(APP_DIR, "config.json")
    val SOCKET_DIR = File("/tmp/sondbord_sockets")

    fun init() {
        APP_DIR.mkdirs()
        SOUNDS_DIR.mkdirs()
        if (SOCKET_DIR.exists()) SOCKET_DIR.deleteRecursively()
        SOCKET_DIR.mkdirs()
    }
}

fun main() = application {
    val viewModel = remember {
        Globals.init()
        val missingDeps = DependencyChecker.checkMissing()

        SoundboardViewModel(
            settingsRepo = SettingsRepository(Globals.CONFIG_FILE),
            soundsDir = Globals.SOUNDS_DIR,
            socketDir = Globals.SOCKET_DIR,
            missingDependencies = missingDeps
        )
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.onCleanup() }
    }

    Window(onCloseRequest = ::exitApplication, title = "Sondbord") {
        AppContent(viewModel)
    }
}