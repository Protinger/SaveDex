package com.savedex.core.domain

/**
 * A chronological snapshot of a [SaveSlot], backed by a copy of the save
 * file at [backupPath]. [hash] is a content hash (see [Hashing]) used to
 * detect whether the emulator's real save file already matches this
 * version, so callers can avoid writing redundant backups.
 */
data class SaveVersion(
    val id: String,
    val slotId: String,
    val timestampEpochMillis: Long,
    val hash: String,
    val backupPath: String,
)
