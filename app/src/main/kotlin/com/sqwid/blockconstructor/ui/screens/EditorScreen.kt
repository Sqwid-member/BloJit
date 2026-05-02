package com.sqwid.blockconstructor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sqwid.blockconstructor.blocks.BlockCatalog
import com.sqwid.blockconstructor.blocks.BlockCategory
import com.sqwid.blockconstructor.blocks.BlockSpec
import com.sqwid.blockconstructor.blocks.LuaCodegen
import com.sqwid.blockconstructor.data.BlockInstance
import com.sqwid.blockconstructor.data.ProjectStore
import com.sqwid.blockconstructor.data.ScriptNode
import com.sqwid.blockconstructor.data.TreeNode
import com.sqwid.blockconstructor.ui.Routes
import com.sqwid.blockconstructor.ui.components.BlockKeyboard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    nav: NavController,
    store: ProjectStore,
    projectId: String,
    scriptId: String
) {
    val project = remember(projectId) { store.loadProject(projectId) } ?: return
    val content = remember(projectId, scriptId) { store.loadContent(projectId) }
    val script = remember(scriptId) { findScript(content.scripts, scriptId) } ?: return
    var version by remember { mutableStateOf(0) }
    fun bump() { version++; store.saveContent(content) }

    var showCatalog by remember { mutableStateOf(false) }
    var catalogTarget by remember { mutableStateOf<Pair<BlockInstance, String>?>(null) }
    var showLua by remember { mutableStateOf(false) }
    var keyboardTarget by remember { mutableStateOf<Triple<BlockInstance, String, String>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${project.appName} / ${script.name}") },
                navigationIcon = {
                    TextButton(onClick = {
                        store.saveContent(content)
                        nav.popBackStack()
                    }) { Text("Назад") }
                },
                actions = {
                    TextButton(onClick = { showLua = true }) { Text("Lua") }
                    Button(onClick = {
                        store.saveContent(content)
                        nav.navigate(Routes.run(projectId, scriptId))
                    }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Запустити")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    catalogTarget = script.root to "body"
                    showCatalog = true
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Додати блок") }
            )
        }
    ) { padding ->
        // re-render when version changes
        @Suppress("UNUSED_EXPRESSION") version
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            BlockTreeView(
                root = script.root,
                onAddInSlot = { parent, slot ->
                    catalogTarget = parent to slot
                    showCatalog = true
                },
                onParamTap = { block, paramName, currentValue ->
                    keyboardTarget = Triple(block, paramName, currentValue)
                },
                onDelete = { parentSlot, child ->
                    parentSlot.remove(child)
                    bump()
                },
                onRename = { block, newName ->
                    block.displayName = newName; bump()
                }
            )
        }
    }

    if (showCatalog) {
        ModalBottomSheet(
            onDismissRequest = { showCatalog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            BlockCatalogPicker(onPick = { spec ->
                val target = catalogTarget ?: return@BlockCatalogPicker
                val parent = target.first
                val slotName = target.second
                val newInstance = BlockInstance(
                    specId = spec.id,
                    displayName = "${spec.label} ${(100..999).random()}",
                    params = spec.params.associate { it.name to it.default }.toMutableMap()
                ).also { inst ->
                    spec.slots.forEach { slot -> inst.slots[slot.name] = mutableListOf() }
                }
                parent.slots.getOrPut(slotName) { mutableListOf() }.add(newInstance)
                bump()
                showCatalog = false
            })
        }
    }

    keyboardTarget?.let { (block, paramName, value) ->
        ModalBottomSheet(
            onDismissRequest = { keyboardTarget = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            BlockKeyboard(
                initial = value,
                paramLabel = paramName,
                onCommit = { newVal ->
                    block.params[paramName] = newVal
                    bump()
                    keyboardTarget = null
                },
                onCancel = { keyboardTarget = null }
            )
        }
    }

    if (showLua) {
        AlertDialog(
            onDismissRequest = { showLua = false },
            title = { Text("Згенерований Lua") },
            text = {
                Column(modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        LuaCodegen.generate(script.root, script.name),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showLua = false }) { Text("OK") } }
        )
    }
}

private fun findScript(list: List<TreeNode>, id: String): ScriptNode? {
    for (n in list) {
        when (n) {
            is TreeNode.Script -> if (n.node.id == id) return n.node
            is TreeNode.Folder -> findScript(n.node.children, id)?.let { return it }
            else -> {}
        }
    }
    return null
}

@Composable
private fun BlockTreeView(
    root: BlockInstance,
    onAddInSlot: (BlockInstance, String) -> Unit,
    onParamTap: (BlockInstance, String, String) -> Unit,
    onDelete: (MutableList<BlockInstance>, BlockInstance) -> Unit,
    onRename: (BlockInstance, String) -> Unit
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(8.dp)
        .verticalScroll(rememberScrollState())
    ) {
        BlockNodeView(
            block = root,
            depth = 0,
            ownerSlot = null,
            onAddInSlot = onAddInSlot,
            onParamTap = onParamTap,
            onDelete = onDelete,
            onRename = onRename
        )
    }
}

@Composable
private fun BlockNodeView(
    block: BlockInstance,
    depth: Int,
    ownerSlot: MutableList<BlockInstance>?,
    onAddInSlot: (BlockInstance, String) -> Unit,
    onParamTap: (BlockInstance, String, String) -> Unit,
    onDelete: (MutableList<BlockInstance>, BlockInstance) -> Unit,
    onRename: (BlockInstance, String) -> Unit
) {
    val spec = BlockCatalog.byId(block.specId) ?: return
    val color = spec.category.color
    var renameOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 12).dp, top = 4.dp)
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
            .border(1.dp, color, RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier
                .size(width = 8.dp, height = 22.dp)
                .background(color, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f).clickable { renameOpen = true }) {
                Text(spec.label, fontWeight = FontWeight.SemiBold)
                if (block.displayName.isNotBlank() && block.displayName != spec.label) {
                    Text(
                        block.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (ownerSlot != null) {
                IconButton(onClick = { onDelete(ownerSlot, block) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Видалити")
                }
            }
        }
        // params
        if (spec.params.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Column {
                spec.params.forEach { p ->
                    val current = block.params[p.name] ?: p.default
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clickable { onParamTap(block, p.name, current) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(p.label + ":", modifier = Modifier.width(140.dp),
                            style = MaterialTheme.typography.bodyMedium)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(current.ifBlank { "(пусто)" },
                                fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        // slots
        if (spec.slots.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            spec.slots.forEach { slot ->
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 4.dp)
                ) {
                    Text("▽ ${slot.label}", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val children = block.slots.getOrPut(slot.name) { mutableListOf() }
                    children.forEach { child ->
                        BlockNodeView(
                            block = child,
                            depth = depth + 1,
                            ownerSlot = children,
                            onAddInSlot = onAddInSlot,
                            onParamTap = onParamTap,
                            onDelete = onDelete,
                            onRename = onRename
                        )
                    }
                    TextButton(onClick = { onAddInSlot(block, slot.name) }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Додати в «${slot.label}»")
                    }
                }
            }
        }
    }

    if (renameOpen) {
        var name by remember { mutableStateOf(block.displayName) }
        AlertDialog(
            onDismissRequest = { renameOpen = false },
            title = { Text("Назва блоку") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Унікальна назва") }
                )
            },
            confirmButton = {
                TextButton(onClick = { onRename(block, name); renameOpen = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { renameOpen = false }) { Text("Скасувати") } }
        )
    }
}

@Composable
private fun BlockCatalogPicker(onPick: (BlockSpec) -> Unit) {
    var selectedCategory by remember { mutableStateOf<BlockCategory?>(null) }
    val grouped = BlockCatalog.byCategory()

    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Text("Каталог блоків", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        if (selectedCategory == null) {
            grouped.keys.chunked(2).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEach { cat ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                                .clickable { selectedCategory = cat },
                            colors = CardDefaults.cardColors(containerColor = cat.color.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(cat.label, fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text("${grouped[cat]?.size ?: 0} блоків",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.85f))
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        } else {
            val cat = selectedCategory!!
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { selectedCategory = null }) { Text("‹ Категорії") }
                Text(cat.label, style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp))
            }
            Spacer(Modifier.height(4.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                items(grouped[cat] ?: emptyList(), key = { it.id }) { spec ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onPick(spec) },
                        colors = CardDefaults.cardColors(containerColor = cat.color.copy(alpha = 0.18f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(spec.label, fontWeight = FontWeight.SemiBold)
                            if (spec.description.isNotBlank()) {
                                Text(spec.description, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.size(width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp) =
    this.then(Modifier.width(width).height(height))
