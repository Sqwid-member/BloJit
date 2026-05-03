package com.sqwid.blojit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sqwid.blojit.R
import com.sqwid.blojit.data.Orientation
import com.sqwid.blojit.data.Project
import com.sqwid.blojit.data.ProjectStore
import com.sqwid.blojit.ui.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectScreen(nav: NavController, store: ProjectStore) {
    var appName by remember { mutableStateOf("My App") }
    var pkg by remember { mutableStateOf("com.sqwid.user.myapp") }
    var minSdk by remember { mutableStateOf("24") }
    var targetSdk by remember { mutableStateOf("34") }
    var orientation by remember { mutableStateOf(Orientation.Portrait) }
    var authorName by remember { mutableStateOf("") }
    var authorPasscode by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.np_title)) },
                navigationIcon = {
                    TextButton(onClick = { nav.popBackStack() }) {
                        Text(stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = appName, onValueChange = { appName = it },
                label = { Text(stringResource(R.string.np_app_name)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = pkg, onValueChange = { pkg = it },
                label = { Text(stringResource(R.string.np_package)) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = minSdk, onValueChange = { minSdk = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.np_min_sdk)) },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = targetSdk, onValueChange = { targetSdk = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.np_target_sdk)) },
                    modifier = Modifier.weight(1f)
                )
            }
            Text(stringResource(R.string.np_orientation), style = MaterialTheme.typography.titleLarge)
            Orientation.values().forEach { opt ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = orientation == opt, onClick = { orientation = opt })
                    Text(stringResource(when (opt) {
                        Orientation.Portrait -> R.string.np_orientation_portrait
                        Orientation.Landscape -> R.string.np_orientation_landscape
                        Orientation.Sensor -> R.string.np_orientation_sensor
                    }))
                }
            }

            Text(stringResource(R.string.np_author_section), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = authorName, onValueChange = { authorName = it },
                label = { Text(stringResource(R.string.np_author_name)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = authorPasscode, onValueChange = { authorPasscode = it.filter { c -> c.isDigit() } },
                label = { Text(stringResource(R.string.np_author_passcode)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val project = Project(
                        id = Project.newId(),
                        appName = appName.ifBlank { "Untitled" },
                        packageName = pkg.ifBlank { "com.sqwid.user.untitled" },
                        minSdk = minSdk.toIntOrNull() ?: 24,
                        targetSdk = targetSdk.toIntOrNull() ?: 34,
                        orientation = orientation,
                        authorName = authorName,
                        authorPasscode = authorPasscode
                    )
                    store.saveProject(project)
                    nav.navigate(Routes.project(project.id)) {
                        popUpTo(Routes.Menu)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(stringResource(R.string.np_create)) }
        }
    }
}
