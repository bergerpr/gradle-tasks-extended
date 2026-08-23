package de.berger.gradletasksview.execution

import de.berger.gradletasksview.model.GradleTaskDescriptor
import de.berger.gradletasksview.model.buildStableId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Deckt die Einstellungen ab, aus denen sowohl die Ausfuehrung als auch der eigene
 * `RunConfigurationProducer` ihre Run-Konfiguration bauen (REQ-079).
 */
class GradleTaskExecutionSettingsTest {

    private fun descriptor(name: String, projectPath: String, gradlePath: String = ":app") =
        GradleTaskDescriptor(
            taskName = name,
            taskPath = "$gradlePath:$name",
            groupName = "build",
            description = "beschreibt $name",
            externalProjectPath = projectPath,
            gradleProjectPath = gradlePath,
            moduleName = "app",
            stableId = buildStableId(projectPath, gradlePath, name),
        )

    @Test
    fun `uebergibt blanke tasknamen und das projektverzeichnis`() {
        val settings = gradleTaskExecutionSettings(listOf(descriptor("buildImage", "/projekt/app")))

        assertEquals("/projekt/app", settings?.externalProjectPath)
        assertEquals(listOf("buildImage"), settings?.taskNames)
        assertEquals(listOf("beschreibt buildImage"), settings?.taskDescriptions)
    }

    @Test
    fun `fasst mehrere tasks desselben projekts zu einem aufruf zusammen`() {
        val settings = gradleTaskExecutionSettings(
            listOf(descriptor("clean", "/projekt/app"), descriptor("buildImage", "/projekt/app")),
        )

        assertEquals(listOf("clean", "buildImage"), settings?.taskNames)
    }

    @Test
    fun `liefert nichts fuer tasks aus verschiedenen projekten`() {
        val settings = gradleTaskExecutionSettings(
            listOf(descriptor("clean", "/projekt/app"), descriptor("clean", "/projekt/lib", ":lib")),
        )

        assertNull(settings)
    }

    @Test
    fun `liefert nichts ohne auswahl`() {
        assertNull(gradleTaskExecutionSettings(emptyList()))
    }
}
