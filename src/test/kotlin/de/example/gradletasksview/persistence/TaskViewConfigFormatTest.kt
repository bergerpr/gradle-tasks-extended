package de.example.gradletasksview.persistence

import de.example.gradletasksview.model.GradleTaskViewMode
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Die vier Namensraeume der obersten Ebene - `settings`, `groups`, `tasks`, `modules` - und
 * die Modulbloecke darin (OPEN-009). Der Beispielinhalt entspricht der `default.yaml` von
 * `../gradle-tasks-extended-testproject`.
 */
class TaskViewConfigFormatTest {

    private val tempDir = Files.createTempDirectory("task-view-config-format-test")

    @AfterTest
    fun cleanup() {
        tempDir.toFile().deleteRecursively()
    }

    private fun read(content: String): TaskViewConfig {
        val file = tempDir.resolve("config-${content.hashCode()}.yaml").toFile()
        file.writeText(content.trimIndent() + "\n")
        return TaskViewConfigYaml.readOrEmpty(file)
    }

    @Test
    fun `groups, tasks and modules are separate namespaces`() {
        val config = read(
            """
            groups:
              docker/gui/jsp:
                icon: smile.svg
            tasks:
              buildImage:
                icon: task.svg
            modules:
              ":app":
                expanded: false
                icon: app.svg
                groups:
                  docker/gui/jsp:
                    expanded: true
                tasks:
                  buildImage:
                    hidden: true
            """
        )

        assertEquals(mapOf("docker/gui/jsp" to TaskViewNodeConfig(icon = "smile.svg")), config.groups)
        assertEquals(mapOf("buildImage" to TaskViewNodeConfig(icon = "task.svg")), config.tasks)
        assertEquals(
            mapOf(
                ":app" to TaskViewModuleConfig(
                    node = TaskViewNodeConfig(expanded = false, icon = "app.svg"),
                    groups = mapOf("docker/gui/jsp" to TaskViewNodeConfig(expanded = true)),
                    tasks = mapOf("buildImage" to TaskViewNodeConfig(hidden = true))
                )
            ),
            config.modules
        )
    }

    @Test
    fun `the root project is addressed as a plain colon`() {
        val config = read(
            """
            modules:
              ":":
                icon: smile.svg
            """
        )

        assertEquals(TaskViewNodeConfig(icon = "smile.svg"), config.modules.getValue(":").node)
    }

    @Test
    fun `submodule keys stay absolute instead of nesting inside their parent block`() {
        val config = read(
            """
            modules:
              ":app":
                expanded: true
              ":app:core":
                icon: smile.svg
            """
        )

        assertEquals(listOf(":app", ":app:core"), config.modules.keys.toList())
        assertTrue(config.modules.getValue(":app").groups.isEmpty())
    }

    @Test
    fun `module paths are normalised to their canonical form`() {
        val config = read(
            """
            modules:
              "app:core":
                expanded: false
            """
        )

        assertEquals(setOf(":app:core"), config.modules.keys)
    }

    @Test
    fun `settings carry showModules, exclude, include and per-mode lists`() {
        val config = read(
            """
            settings:
              showModules: true
              exclude:
                - ":legacy"
              include:
                ":legacy":
                  - docker/gui
              modes:
                sort:
                  exclude:
                    - ":app:core"
            """
        )

        assertEquals(true, config.settings.showModules)
        assertEquals(listOf(":legacy"), config.settings.filter.exclude)
        assertEquals(mapOf(":legacy" to listOf("docker/gui")), config.settings.filter.include)
        assertEquals(
            mapOf(GradleTaskViewMode.SORT to TaskViewFilter(exclude = listOf(":app:core"))),
            config.settings.modeFilters
        )
        // settings ist ein eigener Namensraum und darf nicht als Knoteneintrag durchschlagen.
        assertTrue(config.groups.isEmpty())
        assertTrue(config.tasks.isEmpty())
        assertTrue(config.modules.isEmpty())
    }

    @Test
    fun `module paths in the lists are normalised as well`() {
        val config = read(
            """
            settings:
              exclude:
                - "app:core"
              include:
                "legacy":
                  - docker/gui
            """
        )

        assertEquals(listOf(":app:core"), config.settings.filter.exclude)
        assertEquals(setOf(":legacy"), config.settings.filter.include.keys)
    }

    @Test
    fun `an unknown mode name is ignored instead of discarding the whole file`() {
        val config = read(
            """
            settings:
              showModules: true
              modes:
                doesNotExist:
                  exclude:
                    - ":legacy"
            """
        )

        assertEquals(true, config.settings.showModules)
        assertTrue(config.settings.modeFilters.isEmpty())
    }

    @Test
    fun `an empty file yields an empty config`() {
        val config = read("# nur ein Kommentar")

        assertTrue(config.groups.isEmpty())
        assertTrue(config.tasks.isEmpty())
        assertTrue(config.modules.isEmpty())
        assertTrue(config.settings.isEmpty)
        assertNull(config.settings.showModules)
    }

    @Test
    fun `write then read round-trips every namespace`() {
        val file = tempDir.resolve("roundtrip.yaml").toFile()
        val config = TaskViewConfig(
            settings = TaskViewSettings(
                showModules = true,
                filter = TaskViewFilter(
                    exclude = listOf(":legacy"),
                    include = mapOf(":legacy" to listOf("docker/gui"))
                ),
                modeFilters = mapOf(GradleTaskViewMode.SORT to TaskViewFilter(exclude = listOf(":app:core")))
            ),
            groups = mapOf("docker/gui" to TaskViewNodeConfig(expanded = false)),
            tasks = mapOf("buildImage" to TaskViewNodeConfig(icon = "smile.svg")),
            modules = mapOf(
                ":" to TaskViewModuleConfig(node = TaskViewNodeConfig(expanded = true)),
                ":app" to TaskViewModuleConfig(
                    node = TaskViewNodeConfig(expanded = false, icon = "app.svg"),
                    groups = mapOf("docker/gui/jsp" to TaskViewNodeConfig(expanded = true)),
                    tasks = mapOf("runApp" to TaskViewNodeConfig(hidden = true))
                ),
                ":app:core" to TaskViewModuleConfig(
                    node = TaskViewNodeConfig(
                        modes = mapOf(GradleTaskViewMode.SORT to TaskViewNodeConfig(hidden = true))
                    )
                )
            )
        )

        TaskViewConfigYaml.write(file, config)

        assertEquals(config, TaskViewConfigYaml.readOrEmpty(file))
    }

    @Test
    fun `a module block with nothing but an expand state keeps the short boolean form`() {
        val file = tempDir.resolve("short-module.yaml").toFile()
        TaskViewConfigYaml.write(
            file,
            TaskViewConfig(modules = mapOf(":app" to TaskViewModuleConfig(node = TaskViewNodeConfig(expanded = false))))
        )

        // SnakeYAML gibt den Schluessel als einfachen Skalar aus; der fuehrende Doppelpunkt
        // ist im Block-Kontext zulaessig und wird beim Lesen wieder als Modulpfad erkannt.
        assertEquals("modules:\n  :app: false", file.readText().trim())
    }

    @Test
    fun `empty namespaces are not written at all`() {
        val file = tempDir.resolve("no-settings.yaml").toFile()
        TaskViewConfigYaml.write(
            file,
            TaskViewConfig(groups = mapOf("docker/gui" to TaskViewNodeConfig(expanded = false)))
        )

        assertEquals("groups:\n  docker/gui: false", file.readText().trim())
    }
}
