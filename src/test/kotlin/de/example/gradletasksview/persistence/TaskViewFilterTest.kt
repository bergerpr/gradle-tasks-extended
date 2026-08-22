package de.example.gradletasksview.persistence

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Listen aus `custom.yaml` **ergaenzen** die aus `default.yaml`, statt sie zu ersetzen -
 * sonst koennte eine persoenliche Datei eine Team-Vorgabe unbemerkt verschwinden lassen
 * (siehe REQ-075).
 */
class TaskViewFilterTest {

    @Test
    fun `exclude lists are unioned instead of replaced`() {
        val default = TaskViewFilter(exclude = listOf(":legacy"))
        val custom = TaskViewFilter(exclude = listOf(":sandbox"))

        assertEquals(listOf(":legacy", ":sandbox"), (default + custom).exclude)
    }

    @Test
    fun `duplicate entries collapse`() {
        val default = TaskViewFilter(exclude = listOf(":legacy"))
        val custom = TaskViewFilter(exclude = listOf(":legacy"))

        assertEquals(listOf(":legacy"), (default + custom).exclude)
    }

    @Test
    fun `include lists of the same module add up`() {
        val default = TaskViewFilter(include = mapOf(":legacy" to listOf("docker/gui")))
        val custom = TaskViewFilter(include = mapOf(":legacy" to listOf("build")))

        assertEquals(mapOf(":legacy" to listOf("docker/gui", "build")), (default + custom).include)
    }

    @Test
    fun `include entries for different modules are kept side by side`() {
        val default = TaskViewFilter(include = mapOf(":legacy" to listOf("docker/gui")))
        val custom = TaskViewFilter(include = mapOf(":sandbox" to emptyList()))

        assertEquals(
            mapOf(":legacy" to listOf("docker/gui"), ":sandbox" to emptyList()),
            (default + custom).include
        )
    }
}
