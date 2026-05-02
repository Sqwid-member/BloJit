package com.sqwid.blockconstructor.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sqwid.blockconstructor.ui.theme.ThemePreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    nav: NavController,
    current: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Налаштування") },
                navigationIcon = {
                    TextButton(onClick = { nav.popBackStack() }) { Text("Назад") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)
        ) {
            Text("Тема", style = MaterialTheme.typography.titleLarge)
            ThemePreference.values().forEach { opt ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    RadioButton(selected = current == opt, onClick = { onThemeChange(opt) })
                    Text(when (opt) {
                        ThemePreference.System -> "Системна"
                        ThemePreference.Dark -> "Темна"
                        ThemePreference.Light -> "Світла"
                    })
                }
            }
            Text(
                "Lua-рушій: LuaJ (pure Java). Перехід на LuaJIT через JNI заплановано.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                "Block Constructor v0.1 — Sqwid",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
