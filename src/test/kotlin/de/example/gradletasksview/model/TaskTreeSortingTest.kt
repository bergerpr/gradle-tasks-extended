package de.example.gradletasksview.model

import kotlin.test.Test
import kotlin.test.assertEquals

class TaskTreeSortingTest {

    private fun task(name: String, group: String?) = GradleTaskDescriptor(
        taskName = name,
        taskPath = name,
        groupName = group,
        description = null,
        externalProjectPath = "/project",
        gradleProjectPath = "/project",
        stableId = buildStableId("/project", "/project", name)
    )

    @Test
    fun `case-insensitive comparator is stable via original value tiebreak`() {
        val values = listOf("build", "Build", "apply", "Apply")
        val sorted = values.sortedWith(caseInsensitiveStableComparator)
        assertEquals(listOf("Apply", "apply", "Build", "build"), sorted)
    }

    @Test
    fun `tasks within a group are sorted by name case-insensitively`() {
        val tasks = listOf(task("run-jsp", "docker/gui/jsp"), task("build-jsp", "docker/gui/jsp"))
        val sorted = tasks.sortedWith(taskComparator)
        assertEquals(listOf("build-jsp", "run-jsp"), sorted.map { it.taskName })
    }

    @Test
    fun `Other group always sorts last regardless of alphabetical order`() {
        val groups = listOf("zzz", UNNAMED_GROUP_DISPLAY_NAME, "aaa")
        val sorted = groups.sortedWith(groupPathComparator)
        assertEquals(listOf("aaa", "zzz", UNNAMED_GROUP_DISPLAY_NAME), sorted)
    }
}
