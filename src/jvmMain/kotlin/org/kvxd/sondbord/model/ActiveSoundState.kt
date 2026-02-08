package org.kvxd.sondbord.model

data class ActiveSoundState(
    val id: String,
    val soundFile: SoundFile,
    val progress: Float = 0f,
    val isPaused: Boolean = false,
    val localVolume: Float,
    val remoteVolume: Float,
)