package com.savedex.core.domain.usecase

import com.savedex.core.domain.RealSaveAccess

/** In-memory stand-in for the emulator's real save path — no filesystem or SAF involved. */
class FakeRealSaveAccess : RealSaveAccess {
    private val files = mutableMapOf<String, ByteArray>()

    fun seed(path: String, bytes: ByteArray) {
        files[path] = bytes
    }

    override suspend fun read(path: String): ByteArray? = files[path]

    override suspend fun write(path: String, bytes: ByteArray) {
        files[path] = bytes
    }
}
