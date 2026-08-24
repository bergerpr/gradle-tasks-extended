package de.berger.gradletasksview.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Sichtbarkeit der ueber `execute` konfigurierten Knoten (REQ-085, ADR-023).
 *
 * Der erste Test ist der eigentliche Zweck der ganzen Einrichtung: Der Task verschwindet an
 * seinem Originalplatz und bleibt an der konfigurierten Stelle stehen.
 */
class VirtualTaskVisibilityTest {

    private val buildImage = GradleTaskDescriptor(
        taskName = "buildImage",
        taskPath = ":app:buildImage",
        groupName = "docker/server/gui",
        description = null,
        externalProjectPath = "/project",
        gradleProjectPath = ":app",
        moduleName = "app",
        stableId = buildStableId("/project", ":app", "buildImage")
    )

    private val nodes = listOf(buildImage) + expandVirtualTasks(
        listOf(buildImage),
        listOf(VirtualTaskDefinition("ci/nightly", "Alle Images bauen", ":app:buildImage"))
    ).tasks

    private fun visible(vararg rules: VisibilityRule): List<String> =
        filterHiddenTasks(nodes, rules.toList()).map { "${it.groupName}/${it.displayName}" }

    @Test
    fun `hiding the referenced task leaves the configured node standing`() {
        val visible = visible(VisibilityRule(VisibilityTarget.Task("buildImage"), hidden = true))

        assertEquals(listOf("ci/nightly/Alle Images bauen"), visible)
    }

    @Test
    fun `the configured node is hidden through its own entry`() {
        val visible = visible(
            VisibilityRule(VisibilityTarget.VirtualTask("ci/nightly", "Alle Images bauen"), hidden = true)
        )

        assertEquals(listOf("docker/server/gui/buildImage"), visible)
    }

    @Test
    fun `an entry naming another node does not match`() {
        val visible = visible(
            VisibilityRule(VisibilityTarget.VirtualTask("ci/weekly", "Alle Images bauen"), hidden = true),
            VisibilityRule(VisibilityTarget.VirtualTask("ci/nightly", "Anderer Name"), hidden = true)
        )

        assertEquals(listOf("docker/server/gui/buildImage", "ci/nightly/Alle Images bauen"), visible)
    }

    @Test
    fun `hiding the group takes the configured node with it`() {
        // Er liegt sichtbar in dieser Gruppe - eine ausgeblendete Gruppe nimmt ihren Teilbaum
        // mit, unabhaengig davon, woher ihre Knoten stammen.
        val visible = visible(VisibilityRule(VisibilityTarget.Group("ci"), hidden = true))

        assertEquals(listOf("docker/server/gui/buildImage"), visible)
    }

    @Test
    fun `hiding the module takes both`() {
        val visible = visible(VisibilityRule(VisibilityTarget.Module(":app"), hidden = true))

        assertEquals(emptyList(), visible)
    }

    @Test
    fun `a personal entry can bring the configured node back`() {
        val visible = visible(
            VisibilityRule(VisibilityTarget.Group("ci"), hidden = true),
            VisibilityRule(
                VisibilityTarget.VirtualTask("ci/nightly", "Alle Images bauen"),
                hidden = false,
                fromCustomFile = true
            )
        )

        assertEquals(listOf("docker/server/gui/buildImage", "ci/nightly/Alle Images bauen"), visible)
    }
}
