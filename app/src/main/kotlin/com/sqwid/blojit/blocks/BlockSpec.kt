package com.sqwid.blojit.blocks

import com.sqwid.blojit.data.BlockInstance

/**
 * Static description of a block type. Instances are created via [BlockInstance] and refer back
 * to a spec by [id]. The spec is the single source of truth for: which params/slots a block has,
 * how it renders in the catalogue, and how it lowers to Lua source.
 */
data class BlockSpec(
    val id: String,
    val category: BlockCategory,
    val label: String,
    val description: String = "",
    val params: List<ParamSpec> = emptyList(),
    val slots: List<SlotSpec> = emptyList(),
    /** Returns Lua source for this block including any nested slot output. */
    val emit: (BlockInstance, EmitContext) -> String
)

data class ParamSpec(
    val name: String,
    val label: String,
    val default: String,
    val kind: ParamKind = ParamKind.Expression
)

enum class ParamKind {
    Expression,   // any Lua expression
    Number,       // numeric literal
    Text,         // string literal (will be quoted)
    Color,        // 0xRRGGBB or "#RRGGBB"
    Identifier,   // a variable / function name
    Boolean,      // true / false
}

data class SlotSpec(
    val name: String,
    val label: String,
    /** If true, this slot accepts only one child (e.g. condition wrapper). */
    val singleChild: Boolean = false
)

/** State shared across a single Lua codegen pass. */
class EmitContext(var indentLevel: Int = 0) {
    fun indent(s: String): String {
        val pad = "  ".repeat(indentLevel)
        return s.split("\n").joinToString("\n") { if (it.isEmpty()) it else pad + it }
    }

    fun emitChildren(slot: List<BlockInstance>): String {
        if (slot.isEmpty()) return ""
        indentLevel++
        val out = slot.joinToString("\n") { child ->
            val spec = BlockCatalog.byId(child.specId)
                ?: return@joinToString "${"  ".repeat(indentLevel)}-- unknown block ${child.specId}"
            indent(spec.emit(child, this))
        }
        indentLevel--
        return out
    }
}
