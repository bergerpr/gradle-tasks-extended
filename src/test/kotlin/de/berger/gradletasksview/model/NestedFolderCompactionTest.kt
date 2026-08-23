package de.berger.gradletasksview.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Zusammenfassen von Ordnerpfaden ohne eigene Tasks (REQ-004). Ein Fehler hier faellt sonst
 * nur als "sieht komisch aus" auf, weshalb die Regel Fall fuer Fall festgenagelt ist.
 */
class NestedFolderCompactionTest {

    private fun task(name: String, group: String?) = GradleTaskDescriptor(
        taskName = name,
        taskPath = ":$name",
        groupName = group,
        description = null,
        externalProjectPath = "/project",
        gradleProjectPath = ":",
        moduleName = "project",
        stableId = buildStableId("/project", ":", name)
    )

    /** Angezeigte Namen der Kindordner, in Anzeigereihenfolge. */
    private fun NestedFolderNode.childNames() = childrenFolders.values.map { it.compactedName }

    @Test
    fun `a chain without own tasks collapses into a single row`() {
        val root = buildNestedTree(listOf(task("buildImage", "docker/server/gui")))

        assertEquals(listOf("docker/server/gui"), root.childNames())
        val collapsed = root.childrenFolders.getValue("docker/server/gui")
        assertEquals(listOf("buildImage"), collapsed.childrenTasks.map { it.taskName })
        assertEquals(emptyMap(), collapsed.childrenFolders)
    }

    @Test
    fun `a task on an intermediate level splits the path again`() {
        val root = buildNestedTree(
            listOf(task("buildImage", "docker/server/gui"), task("serverOnly", "docker/server"))
        )

        assertEquals(listOf("docker/server"), root.childNames())
        val server = root.childrenFolders.getValue("docker/server")
        assertEquals(listOf("serverOnly"), server.childrenTasks.map { it.taskName })
        assertEquals(listOf("gui"), server.childNames())
    }

    @Test
    fun `a task on every level keeps all three rows`() {
        val root = buildNestedTree(
            listOf(
                task("buildImage", "docker/server/gui"),
                task("serverOnly", "docker/server"),
                task("dockerOnly", "docker")
            )
        )

        assertEquals(listOf("docker"), root.childNames())
        val docker = root.childrenFolders.getValue("docker")
        assertEquals(listOf("server"), docker.childNames())
        assertEquals(listOf("gui"), docker.childrenFolders.getValue("server").childNames())
    }

    @Test
    fun `two children prevent the collapse`() {
        // Sonst muesste "server" doppelt gezeichnet werden und der gemeinsame Ast ginge verloren.
        val root = buildNestedTree(
            listOf(task("runGui", "server/gui"), task("runCli", "server/cli"))
        )

        assertEquals(listOf("server"), root.childNames())
        assertEquals(listOf("cli", "gui"), root.childrenFolders.getValue("server").childNames())
    }

    @Test
    fun `a flat group stays a single row`() {
        val root = buildNestedTree(listOf(task("assembleAll", "build")))

        assertEquals(listOf("build"), root.childNames())
        assertEquals("build", root.childrenFolders.getValue("build").fullPath)
    }

    @Test
    fun `the full path survives the collapse so config keys keep working`() {
        val root = buildNestedTree(listOf(task("buildImage", "docker/server/gui")))
        val collapsed = root.childrenFolders.getValue("docker/server/gui")

        assertEquals("docker/server/gui", collapsed.fullPath)
        // displayName bleibt genau ein Segment, naemlich das letzte des vollen Pfads.
        assertEquals("gui", collapsed.displayName)
    }

    @Test
    fun `collapsing also happens below a folder that has own tasks`() {
        val root = buildNestedTree(
            listOf(task("printAppGui", "docker"), task("buildImage", "docker/server/gui"))
        )

        // "docker" bleibt wegen des eigenen Tasks stehen, darunter wird zusammengefasst.
        assertEquals(listOf("docker"), root.childNames())
        val docker = root.childrenFolders.getValue("docker")
        assertEquals(listOf("server/gui"), docker.childNames())
        assertEquals("docker/server/gui", docker.childrenFolders.getValue("server/gui").fullPath)
    }

    @Test
    fun `sibling order is unaffected by the collapse`() {
        val root = buildNestedTree(
            listOf(
                task("t1", "zzz/deep"),
                task("t2", "aaa/deep"),
                task("t3", "mmm")
            )
        )

        assertEquals(listOf("aaa/deep", "mmm", "zzz/deep"), root.childNames())
    }

    @Test
    fun `tasks without a group still land in Other and stay last`() {
        val root = buildNestedTree(listOf(task("orphan", null), task("t", "aaa/deep")))

        assertEquals(listOf("aaa/deep", UNNAMED_GROUP_DISPLAY_NAME), root.childNames())
    }
}
