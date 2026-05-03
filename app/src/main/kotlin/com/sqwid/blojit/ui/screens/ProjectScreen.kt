package com.sqwid.blojit.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sqwid.blojit.data.AssetKind
import com.sqwid.blojit.data.AssetNode
import com.sqwid.blojit.data.FolderNode
import com.sqwid.blojit.data.ProjectStore
import com.sqwid.blojit.data.ScriptNode
import com.sqwid.blojit.data.TreeNode
import com.sqwid.blojit.ui.Routes
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(nav: NavController, store: ProjectStore, projectId: String) {
    val context = LocalContext.current
    val project = remember(projectId) { store.loadProject(projectId) }
    if (project == null) {
        Text("Проєкт не знайдено")
        return
    }
    var content by remember { mutableStateOf(store.loadContent(projectId)) }
    var tab by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var addInScripts by remember { mutableStateOf(true) }
    var addParentFolder by remember { mutableStateOf<FolderNode?>(null) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val fname = "img_${System.currentTimeMillis()}.bin"
        val dest = File(store.assetsDir(projectId), fname)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(dest).use { input.copyTo(it) }
        }
        content.assets.add(TreeNode.Asset(AssetNode(
            id = AssetNode.newId(), name = fname, relPath = fname, kind = AssetKind.Image
        )))
        store.saveContent(content)
        content = store.loadContent(projectId)
    }
    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val fname = "vid_${System.currentTimeMillis()}.bin"
        val dest = File(store.assetsDir(projectId), fname)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(dest).use { input.copyTo(it) }
        }
        content.assets.add(TreeNode.Asset(AssetNode(
            id = AssetNode.newId(), name = fname, relPath = fname, kind = AssetKind.Video
        )))
        store.saveContent(content)
        content = store.loadContent(projectId)
    }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val fname = "file_${System.currentTimeMillis()}.bin"
        val dest = File(store.assetsDir(projectId), fname)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(dest).use { input.copyTo(it) }
        }
        content.assets.add(TreeNode.Asset(AssetNode(
            id = AssetNode.newId(), name = fname, relPath = fname, kind = AssetKind.File
        )))
        store.saveContent(content)
        content = store.loadContent(projectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project.appName) },
                navigationIcon = {
                    TextButton(onClick = { nav.popBackStack() }) { Text("Назад") }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    addInScripts = tab == 1
                    addParentFolder = null
                    showAddDialog = true
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(if (tab == 0) "Додати ресурс" else "Додати скрипт") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Ресурси") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Скрипти") })
            }
            val list = if (tab == 0) content.assets else content.scripts
            LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                treeItems(list, depth = 0) { node ->
                    NodeRow(
                        node = node, depth = 0,
                        onOpenScript = { sc ->
                            store.saveContent(content)
                            nav.navigate(Routes.editor(projectId, sc.id))
                        },
                        onAddInside = { folder ->
                            addInScripts = tab == 1
                            addParentFolder = folder
                            showAddDialog = true
                        },
                        onDelete = { id ->
                            removeById(list, id)
                            store.saveContent(content)
                            content = store.loadContent(projectId)
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddDialog(
            inScripts = addInScripts,
            onDismiss = { showAddDialog = false },
            onAddFolder = { name ->
                val target = addParentFolder?.children
                    ?: if (addInScripts) content.scripts else content.assets
                target.add(TreeNode.Folder(FolderNode(id = FolderNode.newId(), name = name)))
                store.saveContent(content)
                content = store.loadContent(projectId)
                showAddDialog = false
            },
            onAddScript = { name ->
                val target = addParentFolder?.children ?: content.scripts
                target.add(TreeNode.Script(ScriptNode(id = ScriptNode.newId(), name = name)))
                store.saveContent(content)
                content = store.loadContent(projectId)
                showAddDialog = false
            },
            onPickImage = { showAddDialog = false; pickImage.launch("image/*") },
            onPickVideo = { showAddDialog = false; pickVideo.launch("video/*") },
            onPickFile = { showAddDialog = false; pickFile.launch("*/*") }
        )
    }
}

private fun removeById(list: MutableList<TreeNode>, id: String): Boolean {
    val it = list.iterator()
    while (it.hasNext()) {
        val node = it.next()
        if (node.id == id) { it.remove(); return true }
        if (node is TreeNode.Folder) {
            if (removeById(node.node.children, id)) return true
        }
    }
    return false
}

private fun androidx.compose.foundation.lazy.LazyListScope.treeItems(
    list: List<TreeNode>,
    depth: Int,
    content: @Composable (TreeNode) -> Unit
) {
    list.forEach { node ->
        item(key = node.id) { content(node) }
        if (node is TreeNode.Folder) {
            treeItems(node.node.children, depth + 1, content)
        }
    }
}

@Composable
private fun NodeRow(
    node: TreeNode,
    depth: Int,
    onOpenScript: (ScriptNode) -> Unit,
    onAddInside: (FolderNode) -> Unit,
    onDelete: (String) -> Unit
) {
    val (icon, label) = when (node) {
        is TreeNode.Folder -> Icons.Filled.Folder to "Папка"
        is TreeNode.Script -> Icons.Filled.Description to "Скрипт"
        is TreeNode.Asset -> when (node.node.kind) {
            AssetKind.Image -> Icons.Filled.Image to "Фото"
            AssetKind.Video -> Icons.Filled.Videocam to "Відео"
            AssetKind.Audio -> Icons.Filled.AudioFile to "Звук"
            AssetKind.File -> Icons.Filled.Description to "Файл"
        }
    }
    val onClick: () -> Unit = {
        when (node) {
            is TreeNode.Script -> onOpenScript(node.node)
            is TreeNode.Folder -> { /* folder rows expand inline via items() */ }
            else -> {}
        }
    }
    var menu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp, top = 4.dp, end = 4.dp, bottom = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(node.name, style = MaterialTheme.typography.titleLarge)
                Text(label, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = null)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                if (node is TreeNode.Folder) {
                    DropdownMenuItem(text = { Text("Додати всередину") }, onClick = {
                        menu = false; onAddInside(node.node)
                    })
                }
                DropdownMenuItem(text = { Text("Видалити") }, onClick = {
                    menu = false; onDelete(node.id)
                })
            }
        }
    }
}

@Composable
private fun AddDialog(
    inScripts: Boolean,
    onDismiss: () -> Unit,
    onAddFolder: (String) -> Unit,
    onAddScript: (String) -> Unit,
    onPickImage: () -> Unit,
    onPickVideo: () -> Unit,
    onPickFile: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (inScripts) "Новий скрипт або папка" else "Додати ресурс") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (inScripts) {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Назва") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { onAddScript(name.ifBlank { "скрипт" }) },
                        modifier = Modifier.fillMaxWidth()) { Text("Створити скрипт") }
                    Button(onClick = { onAddFolder(name.ifBlank { "папка" }) },
                        modifier = Modifier.fillMaxWidth()) { Text("Створити папку") }
                } else {
                    Button(onClick = onPickImage, modifier = Modifier.fillMaxWidth()) { Text("Фото") }
                    Button(onClick = onPickVideo, modifier = Modifier.fillMaxWidth()) { Text("Відео") }
                    Button(onClick = onPickFile, modifier = Modifier.fillMaxWidth()) { Text("Файл") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } }
    )
}
