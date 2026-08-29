package com.savedex.core.data.access

import android.content.Intent
import android.net.Uri

/**
 * Drives the MANAGE_EXTERNAL_STORAGE-with-SAF-fallback permission flow:
 * check [currentAccess] first, and if it's [StorageAccess.Unavailable],
 * launch [createManageAllFilesIntent] to send the user to the system's
 * "All files access" settings screen; if they decline that (or it's not
 * offered on their device/policy), fall back to [createOpenDocumentTreeIntent]
 * and report the result via [onDocumentTreeGranted].
 */
interface StorageAccessManager {
    fun currentAccess(): StorageAccess

    fun createManageAllFilesIntent(): Intent

    fun createOpenDocumentTreeIntent(): Intent

    /** Persists [uri] as a read/write-permitted SAF tree so [currentAccess] can report it later. */
    fun onDocumentTreeGranted(uri: Uri)
}
