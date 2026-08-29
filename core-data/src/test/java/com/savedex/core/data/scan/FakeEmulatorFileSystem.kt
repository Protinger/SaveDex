package com.savedex.core.data.scan

/** In-memory [EmulatorFileSystem] for tests — no real device or filesystem access involved. */
class FakeEmulatorFileSystem : EmulatorFileSystem {

    private sealed interface Node {
        class Dir(val children: MutableMap<String, Node> = linkedMapOf()) : Node
        class LeafFile(val sizeBytes: Long, val lastModifiedEpochMillis: Long) : Node
    }

    private val root = Node.Dir()

    fun addFile(path: String, sizeBytes: Long = 0, lastModifiedEpochMillis: Long = 0) {
        val savePath = SavePath.parse(path)
        require(savePath.segments.isNotEmpty()) { "Cannot add a file at the root path" }
        val parent = dir(SavePath(savePath.segments.dropLast(1)))
        parent.children[savePath.name] = Node.LeafFile(sizeBytes, lastModifiedEpochMillis)
    }

    fun addDirectory(path: String) {
        dir(SavePath.parse(path))
    }

    private fun dir(path: SavePath): Node.Dir {
        var current = root
        for (segment in path.segments) {
            current = current.children.getOrPut(segment) { Node.Dir() } as? Node.Dir
                ?: error("'$segment' in '$path' is a file, not a directory")
        }
        return current
    }

    private fun resolve(path: SavePath): Node? {
        var current: Node = root
        for (segment in path.segments) {
            current = (current as? Node.Dir)?.children?.get(segment) ?: return null
        }
        return current
    }

    override fun isDirectory(path: SavePath): Boolean = resolve(path) is Node.Dir

    override fun children(path: SavePath): List<SavePath> =
        (resolve(path) as? Node.Dir)?.children?.keys?.map { path.child(it) } ?: emptyList()

    override fun metadata(path: SavePath): FileMetadata? =
        (resolve(path) as? Node.LeafFile)?.let { FileMetadata(it.sizeBytes, it.lastModifiedEpochMillis) }
}
