package org.kvxd.bonk.model

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val favorites: Set<String> = emptySet(),
    val localVolume: Float = 80f,
    val remoteVolume: Float = 100f,
    val sortMode: SortMode = SortMode.Name,
    val inputDeviceId: String? = null
)