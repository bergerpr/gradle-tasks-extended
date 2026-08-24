package de.berger.gradletasksview.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Aufweitung der ueber `execute` konfigurierten Knoten (REQ-085). Reine Modelllogik, also ohne
 * laufende IDE pruefbar - und die Stelle, an der die beiden heiklen Zusagen haengen: dass ein
 * solcher Knoten wirklich den referenzierten Task ausfuehrt und dass er eine eigene Identitaet
 * bekommt.
 *
 * Die Fixture entspricht `../gradle-tasks-extended-testproject`, inklusive des dort bewusst
 * doppelt vergebenen Tasknamens `buildImage`.
 */
class VirtualTaskNodesTest {

    private fun task(name: String, module: String, group: String?) = GradleTaskDescriptor(
        taskName = name,
        taskPath = "$module:$name".replace("::", ":"),
        groupName = group,
        description = "was $name tut",
        externalProjectPath = "/project",
        gradleProjectPath = module,
        moduleName = module.substringAfterLast(':').ifEmpty { "root" },
        stableId = buildStableId("/project", module, name)
    )

    private val tasks = listOf(
        task("assembleAll", ":", "build"),
        task("buildImage", ":app", "docker/server/gui"),
        task("buildImage", ":lib", "docker/server/gui"),
        task("coreCheck", ":app:core", "verification")
    )

    @Test
    fun `a qualified reference creates exactly one node`() {
        val expansion = expandVirtualTasks(
            tasks,
            listOf(VirtualTaskDefinition("ci/nightly", "Alle Images bauen", ":app:buildImage"))
        )

        assertEquals(listOf("Alle Images bauen"), expansion.tasks.map { it.displayName })
        assertEquals(listOf(":app"), expansion.tasks.map { it.gradleProjectPath })
        assertTrue(expansion.unresolved.isEmpty())
    }

    @Test
    fun `a bare name creates a node in every module that has the task`() {
        val expansion = expandVirtualTasks(
            tasks,
            listOf(VirtualTaskDefinition("ci/nightly", "Images", "buildImage"))
        )

        assertEquals(listOf(":app", ":lib"), expansion.tasks.map { it.gradleProjectPath })
    }

    @Test
    fun `a module scoped entry only applies inside that module`() {
        val expansion = expandVirtualTasks(
            tasks,
            listOf(VirtualTaskDefinition("ci/nightly", "Images", "buildImage", scopeModulePath = ":lib"))
        )

        assertEquals(listOf(":lib"), expansion.tasks.map { it.gradleProjectPath })
    }

    @Test
    fun `the node executes the referenced task, not its own name`() {
        val expansion = expandVirtualTasks(
            tasks,
            listOf(VirtualTaskDefinition("ci/nightly", "Alle Images bauen", ":app:buildImage"))
        )
        val node = expansion.tasks.single()

        // Genau davon haengt ab, dass Doppelklick und Kontextmenue funktionieren: Ausgefuehrt
        // wird aus taskName + externalProjectPath (ADR-018).
        assertEquals("buildImage", node.taskName)
        assertEquals("/project", node.externalProjectPath)
        assertEquals(":app:buildImage", node.taskPath)
        // Einsortiert wird er dagegen im konfigurierten Pfad.
        assertEquals("ci/nightly", node.groupName)
        assertEquals("ci/nightly", node.virtualGroupPath)
        assertTrue(node.isVirtual)
    }

    @Test
    fun `the node gets its own identity so selection cannot land on the original`() {
        val referenced = tasks.first { it.taskPath == ":app:buildImage" }
        val expansion = expandVirtualTasks(
            tasks,
            listOf(VirtualTaskDefinition("ci/nightly", "Alle Images bauen", ":app:buildImage"))
        )

        assertTrue(expansion.tasks.single().stableId != referenced.stableId)
    }

    @Test
    fun `a module scoped definition replaces the module independent one instead of doubling it`() {
        val expansion = expandVirtualTasks(
            tasks,
            listOf(
                VirtualTaskDefinition("ci/nightly", "Images", "buildImage"),
                VirtualTaskDefinition("ci/nightly", "Images", ":app:coreCheck", scopeModulePath = ":app")
            )
        )

        // In :app gewinnt die genauere Definition, :lib behaelt die allgemeine - in beiden
        // Modulen steht die Zeile aber genau einmal.
        assertEquals(listOf(":app", ":lib"), expansion.tasks.map { it.gradleProjectPath }.sorted())
        assertEquals(1, expansion.tasks.count { it.gradleProjectPath == ":app" })
    }

    @Test
    fun `a root project task is addressed with a single leading colon`() {
        // Ein Task des Hauptprojekts heisst ":assembleAll" - der Teil vor dem Doppelpunkt ist
        // leer und meint damit das Wurzelprojekt, nicht "irgendein Modul".
        val alsoInApp = task("assembleAll", ":app", "build")
        val expansion = expandVirtualTasks(
            tasks + alsoInApp,
            listOf(VirtualTaskDefinition("ci", "Alles bauen", ":assembleAll"))
        )

        assertEquals(listOf(":"), expansion.tasks.map { it.gradleProjectPath })
        assertEquals(":assembleAll", expansion.tasks.single().taskPath)
    }

    @Test
    fun `an unresolvable reference produces no node at all`() {
        val definition = VirtualTaskDefinition("ci/nightly", "Tippfehler", ":app:buldImage")

        val expansion = expandVirtualTasks(tasks, listOf(definition))

        // Ein Knoten, der beim Doppelklick ins Leere liefe, waere schlimmer als ein fehlender.
        assertTrue(expansion.tasks.isEmpty())
        assertEquals(listOf(definition), expansion.unresolved)
    }

    @Test
    fun `the group path is normalised like any other group`() {
        val expansion = expandVirtualTasks(
            tasks,
            listOf(VirtualTaskDefinition("/ci//nightly/", "Images", ":app:buildImage"))
        )

        assertEquals("ci/nightly", expansion.tasks.single().groupName)
    }

    @Test
    fun `references are split into module and task name`() {
        assertEquals(":app" to "buildImage", parseTaskReference(":app:buildImage"))
        assertEquals(":app:core" to "check", parseTaskReference(":app:core:check"))
        // Fuehrender Doppelpunkt ohne Modul meint das Wurzelprojekt.
        assertEquals(":" to "assembleAll", parseTaskReference(":assembleAll"))
        // Ohne Doppelpunkt: jedes Modul, das den Task hat.
        assertEquals(null to "buildImage", parseTaskReference("buildImage"))
        assertNull(parseTaskReference("   "))
        assertNull(parseTaskReference(":"))
    }
}
