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

    private fun fileSystemOrNull(): EmulatorFileSystem? =
        when (val access = storageAccessManager.currentAccess()) {
            is StorageAccess.Full -> DeviceFileSystem(access.root)
            is StorageAccess.Scoped -> DocumentFile.fromTreeUri(context, access.treeUri)?.let(::SafFileSystem)
            StorageAccess.Unavailable -> null
        }
}
