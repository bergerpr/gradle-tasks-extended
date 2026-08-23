package de.berger.gradletasksview.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Baumform der Modulebene nach REQ-074, Struktur wie `../gradle-tasks-extended-testproject`. */
class ModuleTreeNodeTest {

    private fun task(name: String, module: String) = GradleTaskDescriptor(
        taskName = name,
        taskPath = "$module:$name".replace("::", ":"),
        groupName = "build",
        description = null,
        externalProjectPath = "/project",
        gradleProjectPath = module,
        moduleName = module.substringAfterLast(':').ifEmpty { "testproject" },
        stableId = buildStableId("/project", module, name)
    )

    @Test
    fun `the root project is the top node and keeps its own tasks`() {
        val root = buildModuleTree(listOf(task("assembleAll", ":"), task("runApp", ":app")))

        assertEquals(":", root.gradlePath)
        assertEquals(listOf("assembleAll"), root.tasks.map { it.taskName })
        assertEquals(listOf(":app"), root.childrenModules.keys.toList())
    }

    @Test
    fun `a nested subproject hangs below its parent, not beside it`() {
        val root = buildModuleTree(listOf(task("runApp", ":app"), task("coreCheck", ":app:core")))

        val app = root.childrenModules.getValue(":app")
        assertEquals(listOf(":app:core"), app.childrenModules.keys.toList())
        assertEquals(listOf("coreCheck"), app.childrenModules.getValue(":app:core").tasks.map { it.taskName })
    }

    @Test
    fun `intermediate modules without own tasks are still created`() {
        // Sonst stuende ":app:core" elternlos da und die Struktur zerrisse.
        val root = buildModuleTree(listOf(task("coreCheck", ":app:core")))

        val app = root.childrenModules.getValue(":app")
        assertTrue(app.tasks.isEmpty())
        assertEquals("", app.moduleName)
        assertEquals(listOf(":app:core"), app.childrenModules.keys.toList())
    }

    @Test
    fun `the root node exists even when no task belongs to the root project`() {
        val root = buildModuleTree(listOf(task("runApp", ":app")))

        assertEquals(":", root.gradlePath)
        assertTrue(root.tasks.isEmpty())
    }

    @Test
    fun `sibling modules are sorted alphabetically`() {
        val root = buildModuleTree(
            listOf(task("t1", ":lib"), task("t2", ":app"), task("t3", ":legacy"))
        )

        assertEquals(listOf(":app", ":legacy", ":lib"), root.childrenModules.keys.toList())
    }

    @Test
    fun `identically named tasks in two modules stay apart`() {
        val root = buildModuleTree(listOf(task("buildImage", ":app"), task("buildImage", ":lib")))

        assertEquals(listOf("buildImage"), root.childrenModules.getValue(":app").tasks.map { it.taskName })
        assertEquals(listOf("buildImage"), root.childrenModules.getValue(":lib").tasks.map { it.taskName })
        assertEquals(
            listOf(":app:buildImage", ":lib:buildImage"),
            root.childrenModules.values.flatMap { it.tasks }.map { it.taskPath }
        )
    }
}
