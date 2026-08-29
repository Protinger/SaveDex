package com.savedex.core.data.scan

/**
 * A path relative to whatever root an [EmulatorFileSystem] was opened
 * against, kept as segments so it means the same thing whether the
 * underlying access is a plain [java.io.File] tree or a SAF document tree.
 */
data class SavePath(val segments: List<String>) {

    val name: String get() = segments.lastOrNull().orEmpty()

    fun child(segment: String): SavePath = SavePath(segments + segment)

    override fun toString(): String = segments.joinToString("/")

    companion object {
        fun parse(path: String): SavePath =
            SavePath(path.split("/").filter { it.isNotBlank() })
    }
}
