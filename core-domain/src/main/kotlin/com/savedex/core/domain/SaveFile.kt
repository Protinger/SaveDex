package com.savedex.core.domain

/**
 * A save file discovered on device for a given [emulator]. [path] is
 * whatever the scanner that produced this resolved it to (a plain
 * filesystem path or a SAF document path) — it's opaque to domain code,
 * useful only for display and for re-opening the file via the same access
 * mechanism that found it.
 */
data class SaveFile(
    val id: String,
    val emulator: Emulator,
    val path: String,
    val fileName: String,
    val sizeBytes: Long,
    val lastModifiedEpochMillis: Long,
)
