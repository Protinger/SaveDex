package com.savedex.core.data.scan

import com.savedex.core.domain.EmulatorProfile
import com.savedex.core.domain.EmulatorProfiles
import com.savedex.core.domain.SaveFile

/**
 * Walks [fileSystem] looking for files matching each [profiles] entry's
 * known save directories and extensions.
 */
class EmulatorScanner(
    private val fileSystem: EmulatorFileSystem,
    private val profiles: List<EmulatorProfile> = EmulatorProfiles.all,
) {

    fun scan(): List<SaveFile> = profiles.flatMap(::scanProfile)

    private fun scanProfile(profile: EmulatorProfile): List<SaveFile> =
        profile.saveDirectories.flatMap { directory ->
            val root = SavePath.parse(directory)
            if (fileSystem.isDirectory(root)) walk(root, profile) else emptyList()
        }

    private fun walk(directory: SavePath, profile: EmulatorProfile): List<SaveFile> =
        fileSystem.children(directory).flatMap { child ->
            when {
                fileSystem.isDirectory(child) -> walk(child, profile)
                isSaveFile(child, profile) -> listOfNotNull(toSaveFile(child, profile))
                else -> emptyList()
            }
        }

    private fun isSaveFile(path: SavePath, profile: EmulatorProfile): Boolean =
        profile.saveFileExtensions.any { extension -> path.name.endsWith(extension, ignoreCase = true) }

    private fun toSaveFile(path: SavePath, profile: EmulatorProfile): SaveFile? {
        val metadata = fileSystem.metadata(path) ?: return null
        return SaveFile(
            id = path.toString(),
            emulator = profile.emulator,
            path = path.toString(),
            fileName = path.name,
            sizeBytes = metadata.sizeBytes,
            lastModifiedEpochMillis = metadata.lastModifiedEpochMillis,
        )
    }
}
