package com.savedex.core.data.scan

data class FileMetadata(val sizeBytes: Long, val lastModifiedEpochMillis: Long)

/**
 * Minimal read-only filesystem view [EmulatorScanner] walks. Kept free of
 * any Android or `java.io` types so it can be backed by a real device root
 * ([DeviceFileSystem]), a SAF tree ([SafFileSystem]), or an in-memory fake in
 * tests — none of which needs a device or Robolectric to exercise the
 * scanner's traversal logic.
 */
interface EmulatorFileSystem {
    fun isDirectory(path: SavePath): Boolean
    fun children(path: SavePath): List<SavePath>

    /** Null if [path] doesn't exist. */
    fun metadata(path: SavePath): FileMetadata?
}
