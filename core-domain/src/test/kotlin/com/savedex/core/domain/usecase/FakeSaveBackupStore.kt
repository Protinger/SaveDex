package com.savedex.core.domain.usecase

import com.savedex.core.domain.SaveBackupStore

/** In-memory stand-in for the backup file store — no filesystem involved. */
class FakeSaveBackupStore : SaveBackupStore {
    private val backups = mutableMapOf<String, ByteArray>()
    private var counter = 0

    override suspend fun write(slotId: String, bytes: ByteArray, timestampEpochMillis: Long): String {
        val path = "backup/$slotId/${timestampEpochMillis}_${counter++}"
        backups[path] = bytes
        return path
    }

    override suspend fun read(backupPath: String): ByteArray? = backups[backupPath]
}
