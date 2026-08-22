package de.example.gradletasksview.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Deckt das Beispiel aus REQ-004 ab:
 *
 * ```
 * docker/gui -> build-all-gui
 * docker/gui/jsp -> build-jsp, run-jsp
 * docker/gui/react -> build-react
 * docker/server/gui -> build-image
 * ```
 */
class TaskTreeBuilderTest {

    private fun task(name: String, group: String?) = GradleTaskDescriptor(
        taskName = name,
        taskPath = name,
        groupName = group,
        description = null,
        externalProjectPath = "/project",
        gradleProjectPath = ":",
        moduleName = "project",
        stableId = buildStableId("/project", ":", name)
    )

    private val exampleTasks = listOf(
        task("build-all-gui", "docker/gui"),
        task("build-jsp", "docker/gui/jsp"),
        task("run-jsp", "docker/gui/jsp"),
        task("build-react", "docker/gui/react"),
        task("build-image", "docker/server/gui")
    )

    @Test
    fun `sort mode produces a flat group list ordered by full group path`() {
        val groups = buildSortedGroups(exampleTasks)
        assertEquals(
            listOf("docker/gui", "docker/gui/jsp", "docker/gui/react", "docker/server/gui"),
            groups.map { it.first }
        )
    }

    @Test
    fun `sort mode sorts tasks within a group by task name`() {
        val groups = buildSortedGroups(exampleTasks).toMap()
        assertEquals(listOf("build-jsp", "run-jsp"), groups.getValue("docker/gui/jsp").map { it.taskName })
    }

    @Test
    fun `nested mode builds a folder hierarchy with tasks before subfolders`() {
        val root = buildNestedTree(exampleTasks)

        val docker = root.childrenFolders.getValue("docker")
        assertEquals(emptyList(), docker.childrenTasks)

        val gui = docker.childrenFolders.getValue("gui")
        assertEquals(listOf("build-all-gui"), gui.childrenTasks.map { it.taskName })
        assertEquals(listOf("jsp", "react"), gui.childrenFolders.keys.toList())

        val jsp = gui.childrenFolders.getValue("jsp")
        assertEquals(listOf("build-jsp", "run-jsp"), jsp.childrenTasks.map { it.taskName })
        assertEquals(emptyMap(), jsp.childrenFolders)

        val react = gui.childrenFolders.getValue("react")
        assertEquals(listOf("build-react"), react.childrenTasks.map { it.taskName })

        // "server" hat keine eigenen Tasks und genau ein Kind und ist deshalb mit "gui" zu
        // einer Zeile zusammengefasst (REQ-004); "docker" bleibt stehen, es hat zwei Kinder.
        val serverGui = docker.childrenFolders.getValue("server/gui")
        assertEquals("docker/server/gui", serverGui.fullPath)
        assertEquals(listOf("build-image"), serverGui.childrenTasks.map { it.taskName })
    }

    @Test
    fun `folders are sorted alphabetically among siblings`() {
        val tasks = listOf(task("t1", "zzz"), task("t2", "aaa"), task("t3", "mmm"))
        val root = buildNestedTree(tasks)
        assertEquals(listOf("aaa", "mmm", "zzz"), root.childrenFolders.keys.toList())
    }

    @Test
    fun `tasks without a group land in Other as the last group`() {
        val tasks = listOf(task("t1", null), task("t2", "aaa"))
        val groups = buildSortedGroups(tasks)
        assertEquals(listOf("aaa", UNNAMED_GROUP_DISPLAY_NAME), groups.map { it.first })

        val root = buildNestedTree(tasks)
        assertEquals(listOf("aaa", UNNAMED_GROUP_DISPLAY_NAME), root.childrenFolders.keys.toList())
        assertNull(root.childrenFolders[UNNAMED_GROUP_DISPLAY_NAME]?.childrenFolders?.get("nonexistent"))
    }
}
