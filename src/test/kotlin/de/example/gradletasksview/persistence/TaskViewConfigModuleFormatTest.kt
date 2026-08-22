package de.example.gradletasksview.persistence

import de.example.gradletasksview.model.GradleTaskViewMode
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Modulbloecke und der reservierte Schluessel `gradle_tasks_extended` (REQ-074/REQ-075,
 * ADR-014). Der Beispielinhalt entspricht dem Zielblock in der `default.yaml` von
 * `../gradle-tasks-extended-testproject`.
 */
class TaskViewConfigModuleFormatTest {

    private val tempDir = Files.createTempDirectory("task-view-config-module-test")

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
    fun `a leading colon marks a module block, everything else stays a flat entry`() {
        val config = read(
            """
            docker/gui/jsp:
              icon: smile.svg
            ":app":
              expanded: false
              icon: app.svg
              docker/gui/jsp:
                expanded: true
            """
        )

        assertEquals(
            mapOf("docker/gui/jsp" to TaskViewNodeConfig(icon = "smile.svg")),
            config.entries
        )
        assertEquals(
            mapOf(
                ":app" to TaskViewModuleConfig(
                    node = TaskViewNodeConfig(expanded = false, icon = "app.svg"),
                    groups = mapOf("docker/gui/jsp" to TaskViewNodeConfig(expanded = true))
                )
            ),
            config.modules
        )
    }

    @Test
    fun `the root project is addressed as a plain colon`() {
        val config = read(
            """
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
            ":app":
              expanded: true
            ":app:core":
              icon: smile.svg
            """
        )

        assertEquals(listOf(":app", ":app:core"), config.modules.keys.toList())
        // Waere ":app:core" ein Unterschluessel von ":app", liesse es sich nicht mehr von
        // einem Gruppenpfad unterscheiden.
        assertTrue(config.modules.getValue(":app").groups.isEmpty())
    }

    @Test
    fun `the reserved key carries showModules, exclude, include and per-mode lists`() {
        val config = read(
            """
            gradle_tasks_extended:
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
        // Der reservierte Schluessel darf nicht als Knoteneintrag durchschlagen.
        assertTrue(config.entries.isEmpty())
        assertTrue(config.modules.isEmpty())
    }

    @Test
    fun `module paths in the lists are normalised to their canonical form`() {
        val config = read(
            """
            gradle_tasks_extended:
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
            gradle_tasks_extended:
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
    fun `an existing flat file yields exactly what it did before`() {
        val config = read(
            """
            build: false
            tooling: false
            docker/gui/jsp:
              expanded: true
              icon: smile.svg
            buildImage:
              icon: smile.svg
            """
        )

        assertEquals(
            mapOf(
                "build" to TaskViewNodeConfig(expanded = false),
                "tooling" to TaskViewNodeConfig(expanded = false),
                "docker/gui/jsp" to TaskViewNodeConfig(expanded = true, icon = "smile.svg"),
                "buildImage" to TaskViewNodeConfig(icon = "smile.svg")
            ),
            config.entries
        )
        assertTrue(config.modules.isEmpty())
        assertTrue(config.settings.isEmpty)
        assertNull(config.settings.showModules)
    }

    @Test
    fun `write then read round-trips modules and view settings`() {
        val file = tempDir.resolve("roundtrip.yaml").toFile()
        val config = TaskViewConfig(
            entries = mapOf("docker/gui" to TaskViewNodeConfig(expanded = false)),
            modules = mapOf(
                ":" to TaskViewModuleConfig(node = TaskViewNodeConfig(expanded = true)),
                ":app" to TaskViewModuleConfig(
                    node = TaskViewNodeConfig(expanded = false, icon = "app.svg"),
                    groups = mapOf("docker/gui/jsp" to TaskViewNodeConfig(expanded = true))
                )
            ),
            settings = TaskViewSettings(
                showModules = true,
                filter = TaskViewFilter(
                    exclude = listOf(":legacy"),
                    include = mapOf(":legacy" to listOf("docker/gui"))
                ),
                modeFilters = mapOf(GradleTaskViewMode.SORT to TaskViewFilter(exclude = listOf(":app:core")))
            )
        )

        TaskViewConfigYaml.write(file, config)

        assertEquals(config, TaskViewConfigYaml.readOrEmpty(file))
    }

    @Test
    fun `a module block without groups keeps the short boolean form`() {
        val file = tempDir.resolve("short-module.yaml").toFile()
        TaskViewConfigYaml.write(
            file,
            TaskViewConfig(modules = mapOf(":app" to TaskViewModuleConfig(node = TaskViewNodeConfig(expanded = false))))
        )

        // SnakeYAML gibt den Schluessel als einfachen Skalar aus; der fuehrende Doppelpunkt
        // ist im Block-Kontext zulaessig und wird beim Lesen wieder als Modulblock erkannt.
        assertEquals(":app: false", file.readText().trim())
    }

    @Test
    fun `an empty view settings block is not written at all`() {
        val file = tempDir.resolve("no-settings.yaml").toFile()
        TaskViewConfigYaml.write(file, TaskViewConfig(mapOf("docker/gui" to TaskViewNodeConfig(expanded = false))))

        assertEquals("docker/gui: false", file.readText().trim())
    }
}
