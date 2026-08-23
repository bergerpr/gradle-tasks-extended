package de.berger.gradletasksview.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Vorrangregeln beim Ausblenden (REQ-075). Sie sind reine Modelllogik, und genau dort lohnen
 * Tests am meisten: Ein Fehler aeussert sich sonst nur als "ein Task fehlt" und faellt oft
 * erst auf, wenn jemand ihn sucht.
 *
 * Die Faelle sind dieselben, die frueher ueber `exclude`/`include` beschrieben waren - sie
 * sind der Nachweis, dass der `:legacy`-Fall des Testprojekts unveraendert aussieht.
 * Die Struktur der Fixture entspricht `../gradle-tasks-extended-testproject`.
 */
class NodeVisibilityTest {

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

    private fun visible(vararg rules: VisibilityRule): List<String> =
        filterHiddenTasks(tasks, rules.toList()).map { it.taskPath }

    private fun module(
        path: String,
        hidden: Boolean,
        modeSpecific: Boolean = false,
        custom: Boolean = false
    ) = VisibilityRule(VisibilityTarget.Module(path), null, hidden, modeSpecific, custom)

    private fun group(
        path: String,
        hidden: Boolean,
        inModule: String? = null,
        modeSpecific: Boolean = false,
        custom: Boolean = false
    ) = VisibilityRule(VisibilityTarget.Group(path), inModule, hidden, modeSpecific, custom)

    private fun task(
        name: String,
        hidden: Boolean,
        inModule: String? = null,
        custom: Boolean = false
    ) = VisibilityRule(VisibilityTarget.Task(name), inModule, hidden, false, custom)

    @Test
    fun `without any rule everything stays visible`() {
        assertEquals(tasks.map { it.taskPath }, visible())
    }

    @Test
    fun `hiding a module removes it including its submodules`() {
        assertEquals(
            listOf(
                ":assembleAll", ":lib:buildImage",
                ":legacy:legacyBuild", ":legacy:legacyPrintGui", ":legacy:legacyDeploy"
            ),
            visible(module(":app", hidden = true))
        )
    }

    @Test
    fun `a group entry inside the module brings back only that path`() {
        // Das ist der frueher als exclude plus include geschriebene Fall des Testprojekts.
        assertEquals(
            listOf(
                ":assembleAll", ":app:buildImage", ":app:printAppJsp", ":app:core:coreCheck",
                ":lib:buildImage", ":legacy:legacyPrintGui"
            ),
            visible(module(":legacy", hidden = true), group("docker/gui", hidden = false, inModule = ":legacy"))
        )
    }

    @Test
    fun `a group path brings back its subpaths as well`() {
        // "docker" holt sowohl "docker/gui" als auch "docker/gui/jsp" und "docker/server/gui"
        // zurueck - genauso, wie ein Ordner im Baum seine Unterordner enthaelt.
        assertEquals(
            listOf(":assembleAll", ":lib:buildImage", ":legacy:legacyPrintGui"),
            visible(
                module(":app", hidden = true),
                module(":legacy", hidden = true),
                group("docker", hidden = false, inModule = ":legacy")
            )
        )
    }

    @Test
    fun `the longer module path wins over the shorter one`() {
        assertEquals(
            listOf(
                ":assembleAll", ":app:core:coreCheck", ":lib:buildImage",
                ":legacy:legacyBuild", ":legacy:legacyPrintGui", ":legacy:legacyDeploy"
            ),
            visible(module(":app", hidden = true), module(":app:core", hidden = false))
        )
    }

    @Test
    fun `the longer group path wins over the shorter one`() {
        assertEquals(
            listOf(
                ":assembleAll", ":app:printAppJsp", ":app:core:coreCheck",
                ":legacy:legacyBuild", ":legacy:legacyPrintGui", ":legacy:legacyDeploy"
            ),
            visible(group("docker", hidden = true), group("docker/gui", hidden = false))
        )
    }

    @Test
    fun `a task entry beats the group it sits in`() {
        assertEquals(
            listOf(
                ":assembleAll", ":app:printAppJsp", ":app:core:coreCheck",
                ":legacy:legacyBuild", ":legacy:legacyPrintGui", ":legacy:legacyDeploy"
            ),
            visible(task("buildImage", hidden = true))
        )
    }

    @Test
    fun `a module scoped entry does not reach into submodules`() {
        // Der Geltungsbereich ist genau dieser Modulblock; ":app:core" hat einen eigenen.
        assertEquals(
            listOf(
                ":assembleAll", ":app:buildImage", ":app:printAppJsp", ":app:core:coreCheck",
                ":lib:buildImage", ":legacy:legacyBuild", ":legacy:legacyPrintGui", ":legacy:legacyDeploy"
            ),
            visible(group("verification", hidden = true, inModule = ":app"))
        )
    }

    @Test
    fun `a module scoped entry wins over a module independent one`() {
        assertEquals(
            listOf(
                ":assembleAll", ":app:buildImage", ":app:printAppJsp", ":app:core:coreCheck",
                ":legacy:legacyBuild", ":legacy:legacyPrintGui", ":legacy:legacyDeploy"
            ),
            visible(
                group("docker/server/gui", hidden = false),
                group("docker/server/gui", hidden = true, inModule = ":lib")
            )
        )
    }

    @Test
    fun `a mode specific rule wins over a mode independent one of the same specificity`() {
        assertEquals(
            listOf(":assembleAll", ":app:buildImage", ":app:printAppJsp", ":app:core:coreCheck", ":lib:buildImage"),
            visible(module(":legacy", hidden = false), module(":legacy", hidden = true, modeSpecific = true))
        )
    }

    @Test
    fun `custom yaml wins over default yaml at the same specificity`() {
        // Das ist der bewusste Bruch mit der frueheren Vereinigungsregel: Eine persoenliche
        // Einstellung kann eine Team-Vorgabe zuruecknehmen (siehe OPEN-009).
        assertEquals(
            tasks.map { it.taskPath },
            visible(
                module(":legacy", hidden = true, custom = false),
                module(":legacy", hidden = false, custom = true)
            )
        )
    }

    @Test
    fun `hiding the root project hides everything`() {
        assertEquals(emptyList(), visible(module(":", hidden = true)))
    }

    @Test
    fun `isNodeHidden answers for folders and modules, not just tasks`() {
        val rules = listOf(group("docker/gui", hidden = true, inModule = ":app"))

        assertTrue(isNodeHidden(rules, modulePath = ":app", groupName = "docker/gui"))
        assertTrue(isNodeHidden(rules, modulePath = ":app", groupName = "docker/gui/jsp"))
        assertFalse(isNodeHidden(rules, modulePath = ":lib", groupName = "docker/gui"))
        // Eine ausgeblendete Gruppe blendet nicht das Modul aus, in dem sie liegt.
        assertFalse(isNodeHidden(rules, modulePath = ":app"))
    }
}
