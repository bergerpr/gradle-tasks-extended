package de.example.gradletasksview.model

import kotlin.test.Test
import kotlin.test.assertNotEquals

class GradleTaskDescriptorTest {

    @Test
    fun `stableId differs for identical task names in different subprojects`() {
        val idA = buildStableId(
            externalProjectPath = "/root",
            gradleProjectPath = ":module-a",
            taskName = "build"
        )
        val idB = buildStableId(
            externalProjectPath = "/root",
            gradleProjectPath = ":module-b",
            taskName = "build"
        )

        assertNotEquals(idA, idB)
    }

    @Test
    fun `stableId differs for identical task and module names in different composite builds`() {
        val idA = buildStableId(
            externalProjectPath = "/composite-a/module",
            gradleProjectPath = ":module",
            taskName = "build"
        )
        val idB = buildStableId(
            externalProjectPath = "/composite-b/module",
            gradleProjectPath = ":module",
            taskName = "build"
        )

        assertNotEquals(idA, idB)
    }
}
