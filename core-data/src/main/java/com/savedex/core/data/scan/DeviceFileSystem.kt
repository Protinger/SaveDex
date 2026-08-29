package com.savedex.core.data.scan

import java.io.File

/** [EmulatorFileSystem] backed by a plain [File] tree, for use once MANAGE_EXTERNAL_STORAGE is granted. */
class DeviceFileSystem(private val root: File) : EmulatorFileSystem {

    override fun isDirectory(path: SavePath): Boolean = resolve(path).isDirectory

    override fun children(path: SavePath): List<SavePath> =
        resolve(path).listFiles()?.map { path.child(it.name) } ?: emptyList()

    override fun metadata(path: SavePath): FileMetadata? {
        val file = resolve(path)
        if (!file.exists()) return null
        return FileMetadata(sizeBytes = file.length(), lastModifiedEpochMillis = file.lastModified())
    }

    private fun resolve(path: SavePath): File = path.segments.fold(root, ::File)
}
