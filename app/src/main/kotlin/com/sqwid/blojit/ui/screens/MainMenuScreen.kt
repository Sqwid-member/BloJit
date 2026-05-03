package com.sqwid.blojit.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sqwid.blojit.R
import com.sqwid.blojit.data.ProjectStore
import com.sqwid.blojit.ui.Routes

@Composable
fun MainMenuScreen(nav: NavController, store: ProjectStore) {
    val last = store.lastOpened()
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(48.dp))

        MenuButton(
            label = stringResource(R.string.menu_continue),
            icon = Icons.Filled.PlayArrow,
            enabled = last != null,
            onClick = { last?.let { nav.navigate(Routes.project(it)) } }
        )
        Spacer(Modifier.height(12.dp))
        MenuButton(
            label = stringResource(R.string.menu_new),
            icon = Icons.Filled.Add,
            onClick = { nav.navigate(Routes.New) }
        )
        Spacer(Modifier.height(12.dp))
        MenuButton(
            label = stringResource(R.string.menu_projects),
            icon = Icons.Filled.FolderOpen,
            onClick = { nav.navigate(Routes.Projects) }
        )
        Spacer(Modifier.height(12.dp))
        MenuButton(
            label = stringResource(R.string.menu_settings),
            icon = Icons.Filled.Settings,
            onClick = { nav.navigate(Routes.Settings) }
        )
    }
}

@Composable
private fun MenuButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.padding(horizontal = 6.dp))
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}
