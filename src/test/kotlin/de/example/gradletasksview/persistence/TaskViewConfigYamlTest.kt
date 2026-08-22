package de.example.gradletasksview.persistence

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
            mapOf(
                "docker/gui" to TaskViewNodeConfig(expanded = true),
                "docker/gui/jsp" to TaskViewNodeConfig(expanded = false)
            )
        )

        TaskViewConfigYaml.write(file, config)

        assertEquals(config, TaskViewConfigYaml.readOrEmpty(file))
    }

    @Test
    fun `entries without an icon stay in the short boolean form`() {
        val file = tempDir.resolve("short.yaml").toFile()
        TaskViewConfigYaml.write(file, TaskViewConfig(mapOf("docker/gui" to TaskViewNodeConfig(expanded = false))))

        assertEquals("docker/gui: false", file.readText().trim())
    }

    @Test
    fun `boolean short form is still understood`() {
        val file = tempDir.resolve("legacy.yaml").toFile()
        file.writeText("docker/gui/jsp: false\n")

        assertEquals(
            TaskViewConfig(mapOf("docker/gui/jsp" to TaskViewNodeConfig(expanded = false))),
            TaskViewConfigYaml.readOrEmpty(file)
        )
    }

    @Test
    fun `mapping form carries expanded and icon`() {
        val file = tempDir.resolve("icons.yaml").toFile()
        file.writeText(
            """
            docker/gui/jsp:
              expanded: false
              icon: smile.svg
            printGroup-docker-server-gui:
              icon: smile.svg
            """.trimIndent() + "\n"
        )

        assertEquals(
            TaskViewConfig(
                mapOf(
                    "docker/gui/jsp" to TaskViewNodeConfig(expanded = false, icon = "smile.svg"),
                    // Tasks sind Blattknoten und haben daher keinen Expand-Zustand.
                    "printGroup-docker-server-gui" to TaskViewNodeConfig(icon = "smile.svg")
                )
            ),
            TaskViewConfigYaml.readOrEmpty(file)
        )
    }

    @Test
    fun `icon survives a write triggered by an expand change`() {
        val file = tempDir.resolve("roundtrip.yaml").toFile()
        val config = TaskViewConfig(
            mapOf("docker/gui/jsp" to TaskViewNodeConfig(expanded = true, icon = "smile.svg"))
        )

        TaskViewConfigYaml.write(file, config)

        assertEquals(config, TaskViewConfigYaml.readOrEmpty(file))
        assertTrue(file.readText().contains("icon: smile.svg"))
    }

    @Test
    fun `blank or empty mapping entries are ignored instead of pretending to be configured`() {
        val file = tempDir.resolve("blank.yaml").toFile()
        file.writeText("docker/gui:\n  icon: '   '\nhelp: {}\n")

        assertEquals(TaskViewConfig(), TaskViewConfigYaml.readOrEmpty(file))
    }

    @Test
    fun `createStarterIfMissing creates a file only when absent`() {
        val file = tempDir.resolve("default.yaml").toFile()
        assertFalse(file.exists())

        TaskViewConfigYaml.createStarterIfMissing(file)
        assertTrue(file.exists())
        val contentAfterFirstCreate = file.readText()

        file.writeText("docker/gui: false\n")
        TaskViewConfigYaml.createStarterIfMissing(file)

        assertEquals("docker/gui: false\n", file.readText())
        assertTrue(contentAfterFirstCreate.lines().all { it.isBlank() || it.trimStart().startsWith("#") })
    }

    @Test
    fun `starter file has no groups wrapper key and is flat`() {
        val file = tempDir.resolve("default.yaml").toFile()
        TaskViewConfigYaml.createStarterIfMissing(file)

        file.appendText("docker/gui/jsp: false\n")

        assertEquals(
            TaskViewConfig(mapOf("docker/gui/jsp" to TaskViewNodeConfig(expanded = false))),
            TaskViewConfigYaml.readOrEmpty(file)
        )
    }

    @Test
    fun `malformed yaml falls back to empty config instead of throwing`() {
        val file = tempDir.resolve("broken.yaml").toFile()
        file.writeText("docker/gui: [this, is, not, closed")

        assertEquals(TaskViewConfig(), TaskViewConfigYaml.readOrEmpty(file))
    }
}
