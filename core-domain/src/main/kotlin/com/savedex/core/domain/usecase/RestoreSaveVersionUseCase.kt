package com.savedex.core.domain.usecase

import com.savedex.core.domain.RealSaveAccess
import com.savedex.core.domain.SaveBackupStore
import com.savedex.core.domain.SaveRepository

/** Overwrites [realPath] with the exact bytes backed up for [versionId]. */
class RestoreSaveVersionUseCase(
    private val repository: SaveRepository,
    private val realSaveAccess: RealSaveAccess,
    private val backupStore: SaveBackupStore,
) {
    suspend operator fun invoke(versionId: String, realPath: String) {
        val version = checkNotNull(repository.getVersion(versionId)) { "Save version not found: $versionId" }
        val bytes = checkNotNull(backupStore.read(version.backupPath)) {
            "Backup file missing for version $versionId at ${version.backupPath}"
        }
        realSaveAccess.write(realPath, bytes)
    }
}
