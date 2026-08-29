package com.savedex.core.data.access

import android.net.Uri
import java.io.File

/** Current state of SaveDex's access to the device's shared storage. */
sealed interface StorageAccess {
    /** MANAGE_EXTERNAL_STORAGE is granted; [root] is the primary shared storage directory. */
    data class Full(val root: File) : StorageAccess

    /** Fallback path: the user picked a SAF tree instead of granting all-files access. */
    data class Scoped(val treeUri: Uri) : StorageAccess

    /** Neither has been granted yet. */
    data object Unavailable : StorageAccess
}
