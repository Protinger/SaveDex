package com.savedex.core.data.save

import android.content.Context
import com.savedex.core.domain.SaveBackupStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

/**
 * Backups live under the app's private files directory — always writable,
 * never behind SAF, regardless of what storage access the user has granted
 * for real emulator save paths.
 */
class DefaultSaveBackupStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : SaveBackupStore {

    private val root: File get() = File(context.filesDir, "save_backups")

    override suspend fun write(slotId: String, bytes: ByteArray, timestampEpochMillis: Long): String {
        val slotDir = File(root, slotId).apply { mkdirs() }
        val file = File(slotDir, "${timestampEpochMillis}_${UUID.randomUUID()}.sav")
        file.writeBytes(bytes)
        return file.absolutePath
    }

    override suspend fun read(backupPath: String): ByteArray? =
        File(backupPath).takeIf { it.exists() }?.readBytes()
}
