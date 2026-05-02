package com.sqwid.blockconstructor.data

import kotlinx.serialization.Serializable

/**
 * A concrete instance of a [com.sqwid.blockconstructor.blocks.BlockSpec] placed in the script tree.
 *
 * Blocks form a hierarchy: each block has a list of named "slots", and each slot can hold
 * an ordered list of child blocks (think of them as folders / scopes — the root is a "program"
 * block, and a typical event block has one slot named "body").
 *
 * Each parameter slot also has a value — a small expression-like object that the user edits via
 * the in-app keyboard. Values are stored as raw Lua source fragments so the keyboard/codegen do
 * not need to maintain a separate expression tree.
 */
@Serializable
data class BlockInstance(
    val id: String = newId(),
    /** Identifier of the [BlockSpec] this is an instance of. */
    val specId: String,
    /** User-given unique name (passcode + author scoping is handled at the project level). */
    var displayName: String = "",
    /** Map from parameter name → Lua source fragment representing its value. */
    var params: MutableMap<String, String> = mutableMapOf(),
    /** Map from slot name → ordered list of child block ids (resolved through [children]). */
    var slots: MutableMap<String, MutableList<BlockInstance>> = mutableMapOf()
) {
    companion object {
        fun newId(): String = "blk_" + System.currentTimeMillis().toString(36) +
            "_" + (1000..9999).random()

        const val PROGRAM_SPEC = "program"

        fun programRoot(): BlockInstance = BlockInstance(
            specId = PROGRAM_SPEC,
            displayName = "main",
            slots = mutableMapOf("body" to mutableListOf())
        )
    }
}
