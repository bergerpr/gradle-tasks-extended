package de.example.gradletasksview.persistence

import de.example.gradletasksview.model.GradleTaskViewMode
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaskViewConfigYamlTest {

    private val tempDir = Files.createTempDirectory("task-view-config-yaml-test")

    @AfterTest
    fun cleanup() {
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun `readOrEmpty returns empty config for missing file`() {
        val file = tempDir.resolve("missing.yaml").toFile()
        assertEquals(TaskViewConfig(), TaskViewConfigYaml.readOrEmpty(file))
    }

    @Test
    fun `write then readOrEmpty round-trips group paths with slashes`() {
        val file = tempDir.resolve("state.yaml").toFile()
        val config = TaskViewConfig(
            groups = mapOf(
                "docker/gui" to TaskViewNodeConfig(expanded = true),
                "docker/gui/jsp" to TaskViewNodeConfig(expanded = false)
            )
        )

        TaskViewConfigYaml.write(file, config)

        assertEquals(config, TaskViewConfigYaml.readOrEmpty(file))
    }

    @Test
    fun `entries with nothing but an expand state stay in the short boolean form`() {
        val file = tempDir.resolve("short.yaml").toFile()
        TaskViewConfigYaml.write(
            file,
            TaskViewConfig(groups = mapOf("docker/gui" to TaskViewNodeConfig(expanded = false)))
        )

        assertEquals("groups:\n  docker/gui: false", file.readText().trim())
    }

    @Test
    fun `boolean short form is still understood`() {
        val file = tempDir.resolve("short-read.yaml").toFile()
        file.writeText("groups:\n  docker/gui/jsp: false\n")

        assertEquals(
            TaskViewConfig(groups = mapOf("docker/gui/jsp" to TaskViewNodeConfig(expanded = false))),
            TaskViewConfigYaml.readOrEmpty(file)
        )
    }

    @Test
    fun `mapping form carries expanded, icon and hidden`() {
        val file = tempDir.resolve("fields.yaml").toFile()
        file.writeText(
            """
            groups:
              docker/gui/jsp:
                expanded: false
                icon: smile.svg
              docker/server:
                hidden: true
            tasks:
              printGroup-docker-server-gui:
                icon: smile.svg
            """.trimIndent() + "\n"
        )

        assertEquals(
            TaskViewConfig(
                groups = mapOf(
                    "docker/gui/jsp" to TaskViewNodeConfig(expanded = false, icon = "smile.svg"),
                    "docker/server" to TaskViewNodeConfig(hidden = true)
                ),
                // Tasks sind Blattknoten und haben daher keinen Aufklappzustand.
                tasks = mapOf("printGroup-docker-server-gui" to TaskViewNodeConfig(icon = "smile.svg"))
            ),
            TaskViewConfigYaml.readOrEmpty(file)
        )
    }

    @Test
    fun `a group and a task of the same name do not share an entry`() {
        val file = tempDir.resolve("collision.yaml").toFile()
        file.writeText(
            """
            groups:
              build:
                expanded: false
            tasks:
              build:
                icon: smile.svg
            """.trimIndent() + "\n"
        )

        val config = TaskViewConfigYaml.readOrEmpty(file)

        // Genau dafuer gibt es die getrennten Namensraeume: "build" ist in Gradle sowohl
        // eine verbreitete Gruppe als auch ein verbreiteter Taskname.
        assertEquals(TaskViewNodeConfig(expanded = false), config.groups.getValue("build"))
        assertEquals(TaskViewNodeConfig(icon = "smile.svg"), config.tasks.getValue("build"))
    }

    @Test
    fun `mode specific values sit inside the entry they belong to`() {
        val file = tempDir.resolve("modes.yaml").toFile()
        file.writeText(
            """
            groups:
              docker/gui:
                expanded: true
                modes:
                  sort:
                    hidden: true
            """.trimIndent() + "\n"
        )

        assertEquals(
            TaskViewConfig(
                groups = mapOf(
                    "docker/gui" to TaskViewNodeConfig(
                        expanded = true,
                        modes = mapOf(GradleTaskViewMode.SORT to TaskViewNodeConfig(hidden = true))
                    )
                )
            ),
            TaskViewConfigYaml.readOrEmpty(file)
        )
    }

    @Test
    fun `a mode entry carries no further modes of its own`() {
        val file = tempDir.resolve("nested-modes.yaml").toFile()
        file.writeText(
            """
            groups:
              docker/gui:
                modes:
                  sort:
                    hidden: true
                    modes:
                      nested:
                        hidden: false
            """.trimIndent() + "\n"
        )

        val entry = TaskViewConfigYaml.readOrEmpty(file).groups.getValue("docker/gui")

        // Eine zweite Verschachtelung haette keine Bedeutung und wird verworfen, statt sich
        // wie eine gesetzte Angabe zu verhalten.
        assertEquals(
            mapOf(GradleTaskViewMode.SORT to TaskViewNodeConfig(hidden = true)),
            entry.modes
        )
    }

    @Test
    fun `icon survives a write triggered by an expand change`() {
        val file = tempDir.resolve("roundtrip.yaml").toFile()
        val config = TaskViewConfig(
            groups = mapOf("docker/gui/jsp" to TaskViewNodeConfig(expanded = true, icon = "smile.svg"))
        )

        TaskViewConfigYaml.write(file, config)

        assertEquals(config, TaskViewConfigYaml.readOrEmpty(file))
        assertTrue(file.readText().contains("icon: smile.svg"))
    }

    @Test
    fun `blank or empty mapping entries are ignored instead of pretending to be configured`() {
        val file = tempDir.resolve("blank.yaml").toFile()
        file.writeText("groups:\n  docker/gui:\n    icon: '   '\n  help: {}\n")

        assertEquals(TaskViewConfig(), TaskViewConfigYaml.readOrEmpty(file))
    }

    @Test
    fun `unknown top level keys are ignored`() {
        val file = tempDir.resolve("unknown.yaml").toFile()
        file.writeText("groups:\n  docker/gui: false\nwhatever: 42\n")

        assertEquals(
            TaskViewConfig(groups = mapOf("docker/gui" to TaskViewNodeConfig(expanded = false))),
            TaskViewConfigYaml.readOrEmpty(file)
        )
    }

    @Test
    fun `createStarterIfMissing creates a file only when absent`() {
        val file = tempDir.resolve("default.yaml").toFile()
        assertFalse(file.exists())

        TaskViewConfigYaml.createStarterIfMissing(file)
        assertTrue(file.exists())
        val contentAfterFirstCreate = file.readText()

        file.writeText("groups:\n  docker/gui: false\n")
        TaskViewConfigYaml.createStarterIfMissing(file)

        assertEquals("groups:\n  docker/gui: false\n", file.readText())
        assertTrue(contentAfterFirstCreate.lines().all { it.isBlank() || it.trimStart().startsWith("#") })
    }

    @Test
    fun `starter file is pure comment and readable as an empty config`() {
        val file = tempDir.resolve("starter.yaml").toFile()
        TaskViewConfigYaml.createStarterIfMissing(file)

        assertEquals(TaskViewConfig(), TaskViewConfigYaml.readOrEmpty(file))

        file.appendText("groups:\n  docker/gui/jsp: false\n")

        assertEquals(
            TaskViewConfig(groups = mapOf("docker/gui/jsp" to TaskViewNodeConfig(expanded = false))),
            TaskViewConfigYaml.readOrEmpty(file)
        )
    }

    @Test
    fun `malformed yaml falls back to empty config instead of throwing`() {
        val file = tempDir.resolve("broken.yaml").toFile()
        file.writeText("groups: [this, is, not, closed")

        assertEquals(TaskViewConfig(), TaskViewConfigYaml.readOrEmpty(file))
    }
}
