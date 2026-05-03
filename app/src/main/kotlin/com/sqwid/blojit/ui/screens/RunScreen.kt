package com.sqwid.blojit.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sqwid.blojit.blocks.LuaCodegen
import com.sqwid.blojit.data.ProjectStore
import com.sqwid.blojit.data.ScriptNode
import com.sqwid.blojit.data.TreeNode
import com.sqwid.blojit.runtime.RuntimeCanvas
import com.sqwid.blojit.scripting.LuaEngine
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunScreen(
    nav: NavController,
    store: ProjectStore,
    projectId: String,
    scriptId: String
) {
    val project = remember(projectId) { store.loadProject(projectId) } ?: return
    val content = remember(projectId) { store.loadContent(projectId) }
    val script = remember(scriptId) { findScript(content.scripts, scriptId) } ?: return

    val source = remember(scriptId) { LuaCodegen.generate(script.root, script.name) }
    val engine = remember(scriptId) { LuaEngine() }
    var error by remember { mutableStateOf<String?>(null) }
    var logs by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(scriptId) {
        when (val r = engine.run(source)) {
            is LuaEngine.Result.Error -> error = r.message
            else -> error = null
        }
    }

    LaunchedEffect(scriptId) {
        while (true) {
            logs = engine.logs()
            delay(250)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Запуск: ${script.name}") },
                navigationIcon = {
                    TextButton(onClick = { nav.popBackStack() }) { Text("Назад") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(Color.Black, RoundedCornerShape(8.dp))
                .padding(2.dp)
            ) {
                RuntimeCanvas(engine = engine, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(8.dp))
            error?.let {
                Text(
                    "Помилка: $it",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
            Text("Журнал", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            LazyColumn(modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .padding(horizontal = 12.dp)
            ) {
                items(logs) { line ->
                    Text(line, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        }
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
