package org.kvxd.bonk.service

import org.kvxd.bonk.model.*
import java.io.File

object LibraryService {

    private val SUPPORTED_EXT = setOf("mp3", "wav", "ogg", "flac", "m4a")

    fun loadFiles(
        rootDir: File,
        currentDir: File,
        settings: AppSettings,
        filter: SoundFilter,
        query: String
    ): List<SoundFile> {
        val candidates = if (query.isNotEmpty() || filter == SoundFilter.Favorites) {
            rootDir.walk().filter { it.isFile }
        } else {
            currentDir.walk().maxDepth(1).filter { it != currentDir }
        }

        return candidates
            .filter { file ->
                val isAudio = file.extension.lowercase() in SUPPORTED_EXT
                val isDir = file.isDirectory

                if (isDir) {
                    query.isEmpty() && filter == SoundFilter.All
                } else {
                    isAudio
                }
            }
            .map { SoundFile(it, settings.favorites.contains(it.absolutePath)) }
            .filter { s ->
                val matchesQuery = query.isEmpty() || s.name.contains(query, true)
                val matchesFav = filter == SoundFilter.All || s.isFavorite
                matchesQuery && matchesFav
            }
            .sortedWith { a, b ->
                if (a.file.isDirectory != b.file.isDirectory) {
                    if (a.file.isDirectory) -1 else 1
                } else {
                    when (settings.sortMode) {
                        SortMode.Name -> a.name.compareTo(b.name, true)
                        SortMode.LastAdded -> b.file.lastModified().compareTo(a.file.lastModified())
                    }
                }
            }
            .toList()
    }
}