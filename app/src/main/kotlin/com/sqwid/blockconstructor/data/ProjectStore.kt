package com.sqwid.blockconstructor.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Filesystem-backed project storage. Each project lives under
 * `<files>/projects/<projectId>/` and contains:
 *   - `project.json` — [Project] metadata
 *   - `content.json` — [ProjectContent] with the asset/script tree
 *   - `assets/` — copied media files
 *
 * Storage is intentionally JSON files rather than a full database — projects are
 * naturally serializable and small (the heavy data is the asset binaries, which
 * stay outside JSON).
 */
class ProjectStore(private val context: Context) {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private val rootDir: File get() = File(context.filesDir, "projects").apply { mkdirs() }
    private val prefsFile: File get() = File(context.filesDir, "prefs.json").apply {
        if (!exists()) writeText("{}")
    }

    fun listProjects(): List<Project> = rootDir.listFiles()
        ?.filter { it.isDirectory }
        ?.mapNotNull { dir -> runCatching { json.decodeFromString<Project>(File(dir, "project.json").readText()) }.getOrNull() }
        ?.sortedByDescending { it.updatedAt }
        ?: emptyList()

    fun loadProject(id: String): Project? {
        val f = File(File(rootDir, id), "project.json")
        if (!f.exists()) return null
        return runCatching { json.decodeFromString<Project>(f.readText()) }.getOrNull()
    }

    fun saveProject(project: Project) {
        val dir = File(rootDir, project.id).apply { mkdirs() }
        project.updatedAt = System.currentTimeMillis()
        File(dir, "project.json").writeText(json.encodeToString(project))
        // ensure content file exists
        val contentFile = File(dir, "content.json")
        if (!contentFile.exists()) {
            contentFile.writeText(json.encodeToString(ProjectContent(project.id)))
        }
        setLastOpened(project.id)
    }

    fun deleteProject(id: String) {
        File(rootDir, id).deleteRecursively()
    }

    fun loadContent(projectId: String): ProjectContent {
        val f = File(File(rootDir, projectId), "content.json")
        if (!f.exists()) return ProjectContent(projectId)
        return runCatching { json.decodeFromString<ProjectContent>(f.readText()) }
            .getOrElse { ProjectContent(projectId) }
    }

    fun saveContent(content: ProjectContent) {
        val dir = File(rootDir, content.projectId).apply { mkdirs() }
        File(dir, "content.json").writeText(json.encodeToString(content))
        loadProject(content.projectId)?.let { saveProject(it) } // bump updatedAt
    }

    fun assetsDir(projectId: String): File =
        File(File(rootDir, projectId), "assets").apply { mkdirs() }

    fun setLastOpened(projectId: String) {
        prefsFile.writeText(json.encodeToString(mapOf("lastOpened" to projectId)))
    }

    fun lastOpened(): String? {
        return runCatching {
            val map = json.decodeFromString<Map<String, String>>(prefsFile.readText())
            map["lastOpened"]
        }.getOrNull()
    }
}
