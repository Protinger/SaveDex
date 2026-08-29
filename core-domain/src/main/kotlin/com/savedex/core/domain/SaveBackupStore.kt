package com.savedex.core.domain

/**
 * Stores immutable backup copies of save files, addressed by the path each
 * [write] hands back. Backups live in app-private storage, never behind SAF,
 * so they stay readable/writable regardless of what storage access the user
 * has granted for real emulator save paths.
 */
interface SaveBackupStore {
    /** Writes a new backup for [slotId] and returns its storage path. */
    suspend fun write(slotId: String, bytes: ByteArray, timestampEpochMillis: Long): String

    /** Null if [backupPath] no longer resolves to a file — e.g. deleted out of band. */
    suspend fun read(backupPath: String): ByteArray?
}
