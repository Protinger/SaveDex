package com.savedex.core.data.save

import android.content.Context
import androidx.core.net.toUri
import com.savedex.core.domain.RealSaveAccess
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * Reads/writes the emulator's real save path, which is opaque to domain
 * code: a plain filesystem path when the app has full storage access, or a
 * `content://` SAF document URI when it only has a granted tree. Both are
 * plain local I/O — neither touches the network.
 */
class DefaultRealSaveAccess @Inject constructor(
    @ApplicationContext private val context: Context,
) : RealSaveAccess {

    override suspend fun read(path: String): ByteArray? =
        if (path.startsWith("content://")) {
            runCatching { context.contentResolver.openInputStream(path.toUri())?.use { it.readBytes() } }.getOrNull()
        } else {
            File(path).takeIf { it.exists() }?.readBytes()
        }

    override suspend fun write(path: String, bytes: ByteArray) {
        if (path.startsWith("content://")) {
            context.contentResolver.openOutputStream(path.toUri(), "wt")?.use { it.write(bytes) }
        } else {
            File(path).apply { parentFile?.mkdirs() }.writeBytes(bytes)
        }
    }
}
