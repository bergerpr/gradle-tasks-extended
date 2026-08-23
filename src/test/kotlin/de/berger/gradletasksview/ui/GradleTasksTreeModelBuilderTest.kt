package de.berger.gradletasksview.ui

import de.berger.gradletasksview.model.GradleTaskDescriptor
import de.berger.gradletasksview.model.GradleTaskViewMode
import de.berger.gradletasksview.model.buildStableId
import javax.swing.tree.DefaultMutableTreeNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Baumform des `Default`-Modus nach REQ-077: feste Modulebene, ein `Tasks`-Knoten je Modul mit
 * eigenen Tasks, darunter der Ordnerbaum aus REQ-004. Struktur wie
 * `../gradle-tasks-extended-testproject`.
 */
class GradleTasksTreeModelBuilderTest {

    private fun task(name: String, module: String, group: String?) = GradleTaskDescriptor(
        taskName = name,
        taskPath = "$module:$name".replace("::", ":"),
        groupName = group,
        description = null,
        externalProjectPath = "/project",
        gradleProjectPath = module,
        moduleName = module.substringAfterLast(':').ifEmpty { "testproject" },
        stableId = buildStableId("/project", module, name)
    )

    /**
     * Der Baum als eingerueckter Text - lesbarer als eine Kette von `getChildAt(0).getChildAt(1)`
     * und zugleich der Zielbild-Darstellung aus REQ-077 unmittelbar gegenueberstellbar.
     */
    private fun render(mode: GradleTaskViewMode, tasks: List<GradleTaskDescriptor>, showModules: Boolean = false) =
        buildString {
            fun renderChildren(node: DefaultMutableTreeNode, depth: Int) {
                for (i in 0 until node.childCount) {
                    val child = node.getChildAt(i) as DefaultMutableTreeNode
                    append("  ".repeat(depth)).append(child.userObject).append('\n')
                    renderChildren(child, depth + 1)
                }
            }
            val root = GradleTasksTreeModelBuilder.build(mode, tasks, showModules).root as DefaultMutableTreeNode
            renderChildren(root, 0)
        }.trimEnd()

    @Test
    fun `the root project gets a Tasks node with the folder tree below it`() {
        val tree = render(
            GradleTaskViewMode.DEFAULT,
            listOf(
                task("assembleAll", ":", "build"),
                task("printRootGui", ":", "docker/gui"),
                task("rootOnly", ":", "tooling")
            )
        )

        assertEquals(
            """
            :
              Tasks
                build
                  assembleAll
                docker/gui
                  printRootGui
                tooling
                  rootOnly
            """.trimIndent(),
            tree
        )
    }

    @Test
    fun `a module without own tasks gets no Tasks node, only its submodules`() {
        // Sonst stuenden dort eine leere Zeile und ein Klick ins Nichts.
        val tree = render(GradleTaskViewMode.DEFAULT, listOf(task("coreCheck", ":app:core", "verification")))

        assertEquals(
            """
            :
              :app
                :app:core
                  Tasks
                    verification
                      coreCheck
            """.trimIndent(),
            tree
        )
    }

    @Test
    fun `a nested subproject hangs below its parent, each with its own Tasks node`() {
        val tree = render(
            GradleTaskViewMode.DEFAULT,
            listOf(
                task("runApp", ":app", "application"),
                task("printAppJsp", ":app", "docker/gui/jsp"),
                task("coreCheck", ":app:core", "verification")
            )
        )

        assertEquals(
            """
            :
              :app
                Tasks
                  application
                    runApp
                  docker/gui/jsp
                    printAppJsp
                :app:core
                  Tasks
                    verification
                      coreCheck
            """.trimIndent(),
            tree
        )
    }

    @Test
    fun `identically named tasks appear once below each of their own modules`() {
        val tree = render(
            GradleTaskViewMode.DEFAULT,
            listOf(task("buildImage", ":app", "docker"), task("buildImage", ":lib", "docker"))
        )

        assertEquals(
            """
            :
              :app
                Tasks
                  docker
                    buildImage
              :lib
                Tasks
                  docker
                    buildImage
            """.trimIndent(),
            tree
        )
    }

    @Test
    fun `groups and tasks are sorted alphabetically with Other last`() {
        val tree = render(
            GradleTaskViewMode.DEFAULT,
            listOf(
                task("zzz", ":", null),
                task("tooling", ":", "verification"),
                task("runB", ":", "application"),
                task("runA", ":", "application")
            )
        )

        assertEquals(
            """
            :
              Tasks
                application
                  runA
                  runB
                verification
                  tooling
                Other
                  zzz
            """.trimIndent(),
            tree
        )
    }

    @Test
    fun `the module toggle has no effect in Default - the module level is always there`() {
        val tasks = listOf(task("runApp", ":app", "application"))

        assertEquals(
            render(GradleTaskViewMode.DEFAULT, tasks, showModules = false),
            render(GradleTaskViewMode.DEFAULT, tasks, showModules = true)
        )
    }

    @Test
    fun `the module toggle still works in Sort and Nested`() {
        val tasks = listOf(task("runApp", ":app", "application"))

        assertEquals("application\n  runApp", render(GradleTaskViewMode.SORT, tasks, showModules = false))
        assertEquals(
            ":\n  :app\n    application\n      runApp",
            render(GradleTaskViewMode.SORT, tasks, showModules = true)
        )
        assertEquals("application\n  runApp", render(GradleTaskViewMode.NESTED, tasks, showModules = false))
    }

    @Test
    fun `the Tasks node has no configuration key`() {
        // Ein Schluessel "Tasks" waere von einer gleichnamigen echten Gradle-Gruppe nicht zu
        // unterscheiden; der Knoten folgt deshalb dem Zustand seines Moduls (REQ-077).
        val root = GradleTasksTreeModelBuilder
            .build(GradleTaskViewMode.DEFAULT, listOf(task("assembleAll", ":", "build")))
            .root as DefaultMutableTreeNode
        val moduleNode = root.getChildAt(0) as DefaultMutableTreeNode
        val tasksNode = moduleNode.getChildAt(0) as DefaultMutableTreeNode

        assertEquals(TasksNodeData, tasksNode.userObject)
        assertNull(configKeyOf(tasksNode))
        assertEquals(":", enclosingModulePath(tasksNode))
    }
}
