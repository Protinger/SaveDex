package com.savedex.core.data.scan

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.savedex.core.data.access.StorageAccess
import com.savedex.core.data.access.StorageAccessManager
import com.savedex.core.domain.Emulator
import com.savedex.core.domain.SaveFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * Scans for emulator save files using whichever storage access the user has
 * granted, plus a manual fallback ([importManualSave]) for when a save
 * doesn't turn up in the scan — an unrecognized emulator build, a save moved
 * to a custom folder, or a wrong path guess in [com.savedex.core.domain.EmulatorProfiles].
 */
class EmulatorSaveRepository @Inject constructor(
    private val storageAccessManager: StorageAccessManager,
    private val manualSaveFileImporter: ManualSaveFileImporter,
    @ApplicationContext private val context: Context,
) {

    fun scan(): List<SaveFile> {
        val fileSystem = fileSystemOrNull() ?: return emptyList()
        return EmulatorScanner(fileSystem).scan()
    }

    fun createManualSavePickerIntent(): Intent = manualSaveFileImporter.createPickerIntent()

    fun importManualSave(uri: Uri, emulator: Emulator): SaveFile? =
        manualSaveFileImporter.import(uri, emulator)

    /**
     * Resolves a [scan]-produced [SaveFile]'s path — segments joined by "/",
     * opaque outside this package — to the real path
     * [com.savedex.core.domain.RealSaveAccess] can actually read/write: a
     * plain filesystem path under [StorageAccess.Full]'s root, or a genuine
     * `content://` document URI walked through [StorageAccess.Scoped]'s tree
     * (same technique [SafFileSystem] uses internally). Null if the access
     * that produced the scan is gone or the document no longer exists.
     */
    fun resolveRealPath(saveFile: SaveFile): String? =
        when (val access = storageAccessManager.currentAccess()) {
            is StorageAccess.Full -> SavePath.parse(saveFile.path).segments.fold(access.root, ::File).path
            is StorageAccess.Scoped -> resolveScopedPath(saveFile.path, access.treeUri)
            StorageAccess.Unavailable -> null
        }

    private fun resolveScopedPath(path: String, treeUri: Uri): String? {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        val document = SavePath.parse(path).segments.fold(root as DocumentFile?) { current, segment ->
            current?.findFile(segment)
        }
        return document?.uri?.toString()
    }

    private fun fileSystemOrNull(): EmulatorFileSystem? =
        when (val access = storageAccessManager.currentAccess()) {
            is StorageAccess.Full -> DeviceFileSystem(access.root)
            is StorageAccess.Scoped -> DocumentFile.fromTreeUri(context, access.treeUri)?.let(::SafFileSystem)
            StorageAccess.Unavailable -> null
        }
}
