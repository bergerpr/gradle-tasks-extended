package de.example.gradletasksview.persistence

import de.example.gradletasksview.model.GradleTaskViewMode
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `GradleTaskViewSettings` benoetigt keinen Platform-Container fuer die reine
 * Zustandslogik (kein Zugriff auf `Project` im Konstruktor) und kann daher
 * direkt instanziiert werden. Der Expand/Collapse-Zustand wird separat ueber
 * [GradleTasksExpandConfigStore]/[GroupExpandYaml] verwaltet und dort getestet.
 */
class GradleTaskViewSettingsTest {

    @Test
    fun `default mode is DEFAULT`() {
        val settings = GradleTaskViewSettings()
        assertEquals(GradleTaskViewMode.DEFAULT, settings.state.selectedMode)
    }

    @Test
    fun `switchToMode updates the persisted mode`() {
        val settings = GradleTaskViewSettings()

        settings.switchToMode(GradleTaskViewMode.NESTED)
        assertEquals(GradleTaskViewMode.NESTED, settings.state.selectedMode)

        settings.switchToMode(GradleTaskViewMode.SORT)
        assertEquals(GradleTaskViewMode.SORT, settings.state.selectedMode)
    }
}
