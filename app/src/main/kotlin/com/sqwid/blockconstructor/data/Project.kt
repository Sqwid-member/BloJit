package com.sqwid.blockconstructor.data

import kotlinx.serialization.Serializable

/** Top-level project metadata. */
@Serializable
data class Project(
    val id: String,
    var appName: String,
    var packageName: String,
    var minSdk: Int = 24,
    var targetSdk: Int = 34,
    var orientation: Orientation = Orientation.Portrait,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    /** Username + passcode used as a unique identity for blocks the user creates. */
    var authorName: String = "",
    var authorPasscode: String = ""
) {
    companion object {
        fun newId(): String = "proj_" + System.currentTimeMillis().toString(36) +
            "_" + (10000..99999).random()
    }
}

@Serializable
enum class Orientation { Portrait, Landscape, Sensor }

/** A scripts/assets folder under a project. */
@Serializable
data class FolderNode(
    val id: String,
    var name: String,
    var children: MutableList<TreeNode> = mutableListOf()
) {
    companion object { fun newId(): String = "fld_" + System.currentTimeMillis().toString(36) }
}

@Serializable
data class ScriptNode(
    val id: String,
    var name: String,
    /** Root block tree. */
    var root: BlockInstance = BlockInstance.programRoot()
) {
    companion object { fun newId(): String = "scr_" + System.currentTimeMillis().toString(36) }
}

@Serializable
data class AssetNode(
    val id: String,
    var name: String,
    /** Internal storage path (relative to the project's assets dir). */
    var relPath: String,
    var kind: AssetKind
) {
    companion object { fun newId(): String = "ast_" + System.currentTimeMillis().toString(36) }
}

@Serializable
enum class AssetKind { Image, Video, Audio, File }

/** Polymorphic union of nodes that may live inside a folder. */
@Serializable
sealed class TreeNode {
    abstract val id: String
    abstract var name: String

    @Serializable data class Folder(val node: FolderNode) : TreeNode() {
        override val id get() = node.id
        override var name: String
            get() = node.name
            set(value) { node.name = value }
    }

    @Serializable data class Script(val node: ScriptNode) : TreeNode() {
        override val id get() = node.id
        override var name: String
            get() = node.name
            set(value) { node.name = value }
    }

    @Serializable data class Asset(val node: AssetNode) : TreeNode() {
        override val id get() = node.id
        override var name: String
            get() = node.name
            set(value) { node.name = value }
    }
}

/** Persistent data structure that lives next to a Project. */
@Serializable
data class ProjectContent(
    val projectId: String,
    var assets: MutableList<TreeNode> = mutableListOf(),
    var scripts: MutableList<TreeNode> = mutableListOf()
)
