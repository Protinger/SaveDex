package com.savedex.core.data.access

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DefaultStorageAccessManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : StorageAccessManager {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun currentAccess(): StorageAccess {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            return StorageAccess.Full(Environment.getExternalStorageDirectory())
        }
        val treeUri = persistedTreeUri()
        if (treeUri != null) return StorageAccess.Scoped(treeUri)
        return StorageAccess.Unavailable
    }

    override fun createManageAllFilesIntent(): Intent =
        Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            "package:${context.packageName}".toUri(),
        )

    override fun createOpenDocumentTreeIntent(): Intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)

    override fun onDocumentTreeGranted(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        prefs.edit().putString(KEY_TREE_URI, uri.toString()).apply()
    }

    /** Re-checks the URI permission grant rather than trusting the saved string alone — it can be revoked outside the app. */
    private fun persistedTreeUri(): Uri? {
        val savedUri = prefs.getString(KEY_TREE_URI, null)?.toUri() ?: return null
        val stillGranted = context.contentResolver.persistedUriPermissions
            .any { it.uri == savedUri && it.isReadPermission }
        return savedUri.takeIf { stillGranted }
    }

    private companion object {
        const val PREFS_NAME = "storage_access"
        const val KEY_TREE_URI = "tree_uri"
    }
}
