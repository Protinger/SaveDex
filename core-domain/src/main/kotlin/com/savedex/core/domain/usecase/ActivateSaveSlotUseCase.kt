package com.savedex.core.domain.usecase

import com.savedex.core.domain.Hashing
import com.savedex.core.domain.RealSaveAccess
import com.savedex.core.domain.SaveBackupStore
import com.savedex.core.domain.SaveRepository
import com.savedex.core.domain.SaveVersion
import java.util.UUID

/**
 * Switches the active `SaveSlot` for a game to [targetSlotId], writing it
 * into the emulator's real save path so the next time the game is launched,
 * that's the state it loads.
 *
 * Before overwriting anything, whatever the previously-active slot left at
 * [realPath] is preserved as a new [SaveVersion] of that slot — but only if
 * it isn't already captured by that slot's latest version, so switching back
 * and forth between slots without touching the emulator in between doesn't
 * pile up identical backups.
 */
class ActivateSaveSlotUseCase(
    private val repository: SaveRepository,
    private val realSaveAccess: RealSaveAccess,
    private val backupStore: SaveBackupStore,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend operator fun invoke(gameId: String, targetSlotId: String, realPath: String) {
        val previousActive = repository.getActiveSlot(gameId)
        if (previousActive != null && previousActive.id != targetSlotId) {
            backupIfChanged(previousActive.id, realPath)
        }

        val targetVersion = repository.latestVersion(targetSlotId)
        if (targetVersion != null) {
            val bytes = checkNotNull(backupStore.read(targetVersion.backupPath)) {
                "Backup file missing for version ${targetVersion.id}"
            }
            realSaveAccess.write(realPath, bytes)
        }

        repository.setActiveSlot(gameId, targetSlotId)
    }

    private suspend fun backupIfChanged(slotId: String, realPath: String) {
        val currentBytes = realSaveAccess.read(realPath) ?: return
        val currentHash = Hashing.sha256(currentBytes)
        val latest = repository.latestVersion(slotId)
        if (latest != null && latest.hash == currentHash) return

        val timestamp = clock()
        val backupPath = backupStore.write(slotId, currentBytes, timestamp)
        repository.upsertVersion(
            SaveVersion(
                id = UUID.randomUUID().toString(),
                slotId = slotId,
                timestampEpochMillis = timestamp,
                hash = currentHash,
                backupPath = backupPath,
            ),
        )
    }
}
