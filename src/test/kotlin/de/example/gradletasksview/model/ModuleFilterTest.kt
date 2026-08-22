package de.example.gradletasksview.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Deckt die Vorrangregeln aus REQ-075 ab. Sie sind reine Modelllogik und genau dort lohnen
 * Tests am meisten: Ein Fehler aeussert sich sonst nur als "ein Task fehlt" und faellt oft
 * erst auf, wenn jemand ihn sucht.
 *
 * Die Struktur der Fixture entspricht `../gradle-tasks-extended-testproject`.
 */
class ModuleFilterTest {

    private fun task(name: String, module: String, group: String?) = GradleTaskDescriptor(
        taskName = name,
        taskPath = "$module:$name".replace("::", ":"),
        groupName = group,
        description = null,
        externalProjectPath = "/project",
        gradleProjectPath = module,
        moduleName = module.substringAfterLast(':').ifEmpty { "root" },
        stableId = buildStableId("/project", module, name)
    )

    private val tasks = listOf(
        task("assembleAll", ":", "build"),
        task("buildImage", ":app", "docker/server/gui"),
        task("printAppJsp", ":app", "docker/gui/jsp"),
        task("coreCheck", ":app:core", "verification"),
        task("buildImage", ":lib", "docker/server/gui"),
        task("legacyBuild", ":legacy", "build"),
        task("legacyPrintGui", ":legacy", "docker/gui"),
        task("legacyDeploy", ":legacy", "deployment")
    )

    private fun visible(vararg rules: ModuleFilterRule): List<String> =
        filterTasksByModule(tasks, rules.toList()).map { it.taskPath }

    private fun exclude(modulePath: String, modeSpecific: Boolean = false) =
        ModuleFilterRule(modulePath, include = false, modeSpecific = modeSpecific)

    private fun include(modulePath: String, vararg groupPaths: String, modeSpecific: Boolean = false) =
        ModuleFilterRule(modulePath, groupPaths.toList(), include = true, modeSpecific = modeSpecific)

    @Test
    fun `without any rule everything stays visible`() {
        assertEquals(tasks.map { it.taskPath }, visible())
    }

    @Test
    fun `excluding a module removes it including its submodules`() {
        assertEquals(
            listOf(":assembleAll", ":lib:buildImage", ":legacy:legacyBuild", ":legacy:legacyPrintGui", ":legacy:legacyDeploy"),
            visible(exclude(":app"))
        )
    }

    @Test
    fun `include brings back only the named group paths of an excluded module`() {
        assertEquals(
            listOf(":assembleAll", ":app:buildImage", ":app:printAppJsp", ":app:core:coreCheck", ":lib:buildImage", ":legacy:legacyPrintGui"),
            visible(exclude(":legacy"), include(":legacy", "docker/gui"))
        )
    }

    @Test
    fun `an included group path also brings back its subpaths`() {
        // "docker" holt sowohl "docker/gui" als auch "docker/gui/jsp" und "docker/server/gui"
        // zurueck - genauso, wie ein Ordner im Baum seine Unterordner enthaelt.
        assertEquals(
            listOf(":assembleAll", ":lib:buildImage", ":legacy:legacyPrintGui"),
            visible(exclude(":app"), exclude(":legacy"), include(":legacy", "docker"))
        )
    }

    @Test
    fun `the longer path wins over a shorter one`() {
        assertEquals(
            listOf(":assembleAll", ":app:core:coreCheck", ":lib:buildImage", ":legacy:legacyBuild", ":legacy:legacyPrintGui", ":legacy:legacyDeploy"),
            visible(exclude(":app"), include(":app:core"))
        )
    }

    @Test
    fun `include wins over exclude on the very same path`() {
        assertEquals(
            listOf(":assembleAll", ":app:buildImage", ":app:printAppJsp", ":app:core:coreCheck", ":lib:buildImage", ":legacy:legacyBuild", ":legacy:legacyPrintGui", ":legacy:legacyDeploy"),
            visible(exclude(":legacy"), include(":legacy"))
        )
    }

    @Test
    fun `an include without a matching exclude changes nothing`() {
        assertEquals(tasks.map { it.taskPath }, visible(include(":legacy", "docker/gui")))
    }

    @Test
    fun `a mode specific rule wins over a global one of the same specificity`() {
        assertEquals(
            listOf(":assembleAll", ":app:buildImage", ":app:printAppJsp", ":app:core:coreCheck", ":lib:buildImage"),
            visible(include(":legacy"), exclude(":legacy", modeSpecific = true))
        )
    }

    @Test
    fun `include lists of equal specificity add up instead of replacing each other`() {
        assertEquals(
            listOf(":assembleAll", ":app:buildImage", ":app:printAppJsp", ":app:core:coreCheck", ":lib:buildImage", ":legacy:legacyBuild", ":legacy:legacyPrintGui"),
            visible(
                exclude(":legacy"),
                include(":legacy", "docker/gui"),
                include(":legacy", "build", modeSpecific = true)
            )
        )
    }

    @Test
    fun `excluding the root project hides everything`() {
        assertEquals(emptyList(), visible(exclude(":")))
    }
}
