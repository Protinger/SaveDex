package com.savedex.core.data.scan

import androidx.documentfile.provider.DocumentFile

/** [EmulatorFileSystem] backed by a SAF tree, for use when MANAGE_EXTERNAL_STORAGE was denied. */
class SafFileSystem(private val root: DocumentFile) : EmulatorFileSystem {

    override fun isDirectory(path: SavePath): Boolean = resolve(path)?.isDirectory == true

    override fun children(path: SavePath): List<SavePath> {
        val directory = resolve(path) ?: return emptyList()
        return directory.listFiles().mapNotNull { document ->
            document.name?.let { name -> path.child(name) }
        }
    }

    override fun metadata(path: SavePath): FileMetadata? {
        val document = resolve(path) ?: return null
        if (!document.exists()) return null
        return FileMetadata(sizeBytes = document.length(), lastModifiedEpochMillis = document.lastModified())
    }

    private fun resolve(path: SavePath): DocumentFile? =
        path.segments.fold(root as DocumentFile?) { current, segment -> current?.findFile(segment) }
}
