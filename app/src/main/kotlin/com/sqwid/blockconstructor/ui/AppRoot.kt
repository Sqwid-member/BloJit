package com.sqwid.blockconstructor.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sqwid.blockconstructor.data.ProjectStore
import com.sqwid.blockconstructor.ui.screens.EditorScreen
import com.sqwid.blockconstructor.ui.screens.MainMenuScreen
import com.sqwid.blockconstructor.ui.screens.NewProjectScreen
import com.sqwid.blockconstructor.ui.screens.ProjectScreen
import com.sqwid.blockconstructor.ui.screens.ProjectsScreen
import com.sqwid.blockconstructor.ui.screens.RunScreen
import com.sqwid.blockconstructor.ui.screens.SettingsScreen
import com.sqwid.blockconstructor.ui.theme.BlockConstructorTheme
import com.sqwid.blockconstructor.ui.theme.ThemePreference

object Routes {
    const val Menu = "menu"
    const val New = "new"
    const val Projects = "projects"
    const val Settings = "settings"
    const val Project = "project/{id}"
    const val Editor = "editor/{projectId}/{scriptId}"
    const val Run = "run/{projectId}/{scriptId}"
    fun project(id: String) = "project/$id"
    fun editor(projectId: String, scriptId: String) = "editor/$projectId/$scriptId"
    fun run(projectId: String, scriptId: String) = "run/$projectId/$scriptId"
}

@Composable
fun AppRoot() {
    var theme by rememberSaveable { mutableStateOf(ThemePreference.System) }
    BlockConstructorTheme(preference = theme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val nav = rememberNavController()
            val context = LocalContext.current
            val store = remember { ProjectStore(context) }
            NavHost(navController = nav, startDestination = Routes.Menu) {
                composable(Routes.Menu) { MainMenuScreen(nav, store) }
                composable(Routes.New) { NewProjectScreen(nav, store) }
                composable(Routes.Projects) { ProjectsScreen(nav, store) }
                composable(Routes.Settings) {
                    SettingsScreen(nav, theme) { theme = it }
                }
                composable(Routes.Project) { backStack ->
                    val id = backStack.arguments?.getString("id") ?: return@composable
                    ProjectScreen(nav, store, id)
                }
                composable(Routes.Editor) { backStack ->
                    val pid = backStack.arguments?.getString("projectId") ?: return@composable
                    val sid = backStack.arguments?.getString("scriptId") ?: return@composable
                    EditorScreen(nav, store, pid, sid)
                }
                composable(Routes.Run) { backStack ->
                    val pid = backStack.arguments?.getString("projectId") ?: return@composable
                    val sid = backStack.arguments?.getString("scriptId") ?: return@composable
                    RunScreen(nav, store, pid, sid)
                }
            }
        }
    }
}
