package com.sqwid.blojit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sqwid.blojit.R
import com.sqwid.blojit.data.Project
import com.sqwid.blojit.data.ProjectStore
import com.sqwid.blojit.ui.Routes
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(nav: NavController, store: ProjectStore) {
    var projects by remember { mutableStateOf(store.listProjects()) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_projects)) },
                navigationIcon = {
                    TextButton(onClick = { nav.popBackStack() }) {
                        Text(stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        if (projects.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.pw_no_projects), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.pw_no_projects_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(projects, key = { it.id }) { p ->
                ProjectCard(
                    project = p,
                    onOpen = { nav.navigate(Routes.project(p.id)) },
                    onDelete = {
                        store.deleteProject(p.id)
                        projects = store.listProjects()
                    }
                )
            }
        }
    }
}

@Composable
private fun ProjectCard(project: Project, onOpen: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(project.appName, style = MaterialTheme.typography.titleLarge)
                Text(project.packageName, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date(project.updatedAt))
                Text("$date · ${project.orientation}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = null)
            }
        }
    }
}
