package com.savedex.core.domain.usecase

import com.savedex.core.domain.Hashing
import com.savedex.core.domain.RealSaveAccess
import com.savedex.core.domain.SaveBackupStore
import com.savedex.core.domain.SaveRepository
import com.savedex.core.domain.SaveVersion
import java.util.UUID

/** Manually snapshots whatever's currently at [realPath] as a new [SaveVersion] of [slotId]. */
class BackupSaveUseCase(
    private val repository: SaveRepository,
    private val realSaveAccess: RealSaveAccess,
    private val backupStore: SaveBackupStore,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend operator fun invoke(slotId: String, realPath: String): SaveVersion {
        val bytes = checkNotNull(realSaveAccess.read(realPath)) { "No save file found at $realPath" }
        val timestamp = clock()
        val backupPath = backupStore.write(slotId, bytes, timestamp)
        val version = SaveVersion(
            id = UUID.randomUUID().toString(),
            slotId = slotId,
            timestampEpochMillis = timestamp,
            hash = Hashing.sha256(bytes),
            backupPath = backupPath,
        )
        repository.upsertVersion(version)
        return version
    }
}
