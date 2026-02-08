package org.kvxd.sondbord.model

import java.io.File

data class SoundFile(
    val file: File,
    val isFavorite: Boolean
) {

    val name: String get() = file.nameWithoutExtension
    val id: String get() = file.absolutePath
}