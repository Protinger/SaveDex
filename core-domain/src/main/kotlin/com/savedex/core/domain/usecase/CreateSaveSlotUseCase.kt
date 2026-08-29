package com.savedex.core.domain.usecase

import com.savedex.core.domain.Hashing
import com.savedex.core.domain.RealSaveAccess
import com.savedex.core.domain.SaveBackupStore
import com.savedex.core.domain.SaveRepository
import com.savedex.core.domain.SaveSlot
import com.savedex.core.domain.SaveVersion
import java.util.UUID

/**
 * Creates a new [SaveSlot] for a game, optionally seeded with a first
 * [SaveVersion] copied from whatever's currently at [seedFromRealPath] — the
 * emulator's real save path. Pass `null` to start the slot empty.
 */
class CreateSaveSlotUseCase(
    private val repository: SaveRepository,
    private val realSaveAccess: RealSaveAccess,
    private val backupStore: SaveBackupStore,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend operator fun invoke(
        gameId: String,
        name: String,
        seedFromRealPath: String? = null,
    ): SaveSlot {
        val slot = SaveSlot(id = UUID.randomUUID().toString(), gameId = gameId, name = name, isActive = false)
        repository.upsertSlot(slot)

        val seedBytes = seedFromRealPath?.let { realSaveAccess.read(it) }
        if (seedBytes != null) {
            val timestamp = clock()
            val backupPath = backupStore.write(slot.id, seedBytes, timestamp)
            repository.upsertVersion(
                SaveVersion(
                    id = UUID.randomUUID().toString(),
                    slotId = slot.id,
                    timestampEpochMillis = timestamp,
                    hash = Hashing.sha256(seedBytes),
                    backupPath = backupPath,
                ),
            )
        }

        return slot
    }
}
