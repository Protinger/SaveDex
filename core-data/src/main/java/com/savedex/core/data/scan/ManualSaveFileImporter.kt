package com.savedex.core.data.scan

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.savedex.core.domain.Emulator
import com.savedex.core.domain.SaveFile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Fallback for when [EmulatorScanner] doesn't find a save — an unknown
 * emulator build, a save moved to a custom folder, a profile whose path
 * guess turned out wrong. Lets the user pick the file directly via SAF
 * (`ACTION_OPEN_DOCUMENT`) and tag it with the [Emulator] it belongs to.
 */
class ManualSaveFileImporter @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun createPickerIntent(): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*")

    /** Null if [uri] no longer resolves to a real document. */
    fun import(uri: Uri, emulator: Emulator): SaveFile? {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        val document = DocumentFile.fromSingleUri(context, uri)?.takeIf { it.exists() } ?: return null
        return SaveFile(
            id = uri.toString(),
            emulator = emulator,
            path = uri.toString(),
            fileName = document.name ?: uri.lastPathSegment.orEmpty(),
            sizeBytes = document.length(),
            lastModifiedEpochMillis = document.lastModified(),
        )
    }
}
