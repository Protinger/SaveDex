package com.savedex.core.domain

import java.security.MessageDigest

/** Content hashing shared by [SaveVersion] snapshots and stable [Game] identifiers. */
object Hashing {
    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(separator = "") { "%02x".format(it) }
}
