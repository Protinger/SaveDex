package com.savedex.core.domain

/**
 * Reads and writes the emulator's real, live save file — the one the
 * emulator itself loads from and writes to during play. `path` is whatever
 * a [SaveFile] resolved to: a plain filesystem path or a SAF document URI,
 * opaque to domain code. Callers (not the domain model) are responsible for
 * knowing which real path corresponds to a given [Game]/[SaveSlot], since it
 * can shift across scans (a granted SAF tree changing, a reinstalled
 * emulator) independently of the slot history tracked here.
 */
interface RealSaveAccess {
    /** Null if nothing exists at [path] yet. */
    suspend fun read(path: String): ByteArray?

    suspend fun write(path: String, bytes: ByteArray)
}
