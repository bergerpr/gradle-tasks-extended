package de.example.gradletasksview.persistence

import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupExpandYamlTest {

    private val tempDir = Files.createTempDirectory("group-expand-yaml-test")

    @AfterTest
    fun cleanup() {
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun `readOrEmpty returns empty state for missing file`() {
        val file = tempDir.resolve("missing.yaml").toFile()
        assertEquals(GroupExpandState(), GroupExpandYaml.readOrEmpty(file))
    }

    @Test
    fun `write then readOrEmpty round-trips group paths with slashes`() {
        val file = tempDir.resolve("state.yaml").toFile()
        val state = GroupExpandState(mapOf("docker/gui" to true, "docker/gui/jsp" to false))

        GroupExpandYaml.write(file, state)
        val loaded = GroupExpandYaml.readOrEmpty(file)

        assertEquals(state, loaded)
    }

    @Test
    fun `createStarterIfMissing creates a file only when absent`() {
        val file = tempDir.resolve("default.yaml").toFile()
        assertFalse(file.exists())

        GroupExpandYaml.createStarterIfMissing(file)
        assertTrue(file.exists())
        val contentAfterFirstCreate = file.readText()

        file.writeText("docker/gui: false\n")
        GroupExpandYaml.createStarterIfMissing(file)

        assertEquals("docker/gui: false\n", file.readText())
        assertTrue(contentAfterFirstCreate.lines().all { it.isBlank() || it.trimStart().startsWith("#") })
    }

    @Test
    fun `starter file has no groups wrapper key and is flat`() {
        val file = tempDir.resolve("default.yaml").toFile()
        GroupExpandYaml.createStarterIfMissing(file)

        file.appendText("docker/gui/jsp: false\n")
        val loaded = GroupExpandYaml.readOrEmpty(file)

        assertEquals(GroupExpandState(mapOf("docker/gui/jsp" to false)), loaded)
    }

    @Test
    fun `malformed yaml falls back to empty state instead of throwing`() {
        val file = tempDir.resolve("broken.yaml").toFile()
        file.writeText("docker/gui: [this, is, not, closed")

        assertEquals(GroupExpandState(), GroupExpandYaml.readOrEmpty(file))
    }
}
