package de.example.gradletasksview.persistence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `custom.yaml` haelt nur echte Abweichungen fest. Stimmt ein Knoten wieder mit dem
 * aufgeloesten Wert ueberein, wird sein Feld auf `null` gesetzt - und was dann leer ist, darf
 * nicht als Rumpf in der Datei zurueckbleiben (OPEN-009, AP-3).
 */
class TaskViewConfigPruningTest {

    @Test
    fun `empty node entries disappear`() {
        val config = TaskViewConfig(
            groups = mapOf(
                "docker/gui" to TaskViewNodeConfig(),
                "docker/gui/jsp" to TaskViewNodeConfig(expanded = true)
            ),
            tasks = mapOf("buildImage" to TaskViewNodeConfig())
        )

        val pruned = config.withoutEmptyEntries()

        assertEquals(mapOf("docker/gui/jsp" to TaskViewNodeConfig(expanded = true)), pruned.groups)
        assertTrue(pruned.tasks.isEmpty())
    }

    @Test
    fun `a module block that only held empty entries disappears with them`() {
        val config = TaskViewConfig(
            modules = mapOf(
                ":app" to TaskViewModuleConfig(groups = mapOf("docker/gui" to TaskViewNodeConfig())),
                ":lib" to TaskViewModuleConfig(node = TaskViewNodeConfig(expanded = false))
            )
        )

        val pruned = config.withoutEmptyEntries()

        assertEquals(setOf(":lib"), pruned.modules.keys)
    }

    @Test
    fun `a module block with its own state survives losing an entry`() {
        val config = TaskViewConfig(
            modules = mapOf(
                ":app" to TaskViewModuleConfig(
                    node = TaskViewNodeConfig(expanded = true),
                    groups = mapOf("docker/gui" to TaskViewNodeConfig())
                )
            )
        )

        val pruned = config.withoutEmptyEntries()

        assertEquals(TaskViewNodeConfig(expanded = true), pruned.modules.getValue(":app").node)
        assertTrue(pruned.modules.getValue(":app").groups.isEmpty())
    }

    @Test
    fun `settings are untouched`() {
        val config = TaskViewConfig(settings = TaskViewSettings(showModules = true))

        assertEquals(TaskViewSettings(showModules = true), config.withoutEmptyEntries().settings)
    }
}
