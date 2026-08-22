package de.example.gradletasksview.model

import de.example.gradletasksview.util.ROOT_MODULE_PATH
import de.example.gradletasksview.util.isModulePath
import de.example.gradletasksview.util.isModuleWithin
import de.example.gradletasksview.util.moduleSegments
import de.example.gradletasksview.util.normalizeModulePath
import de.example.gradletasksview.util.parentModulePath
import de.example.gradletasksview.util.qualifiedTaskPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GradleModulePathUtilTest {

    @Test
    fun `a leading colon marks a key as a module path`() {
        assertTrue(isModulePath(":app"))
        assertTrue(isModulePath(ROOT_MODULE_PATH))
        assertFalse(isModulePath("docker/gui/jsp"))
        assertFalse(isModulePath("buildImage"))
    }

    @Test
    fun `segments of the root project are empty`() {
        assertEquals(emptyList(), moduleSegments(ROOT_MODULE_PATH))
        assertEquals(listOf("app"), moduleSegments(":app"))
        assertEquals(listOf("app", "core"), moduleSegments(":app:core"))
    }

    @Test
    fun `hand written keys are normalised to the canonical form`() {
        assertEquals(":app:core", normalizeModulePath("app:core"))
        assertEquals(":app:core", normalizeModulePath(":app::core:"))
        assertEquals(ROOT_MODULE_PATH, normalizeModulePath(""))
        assertEquals(ROOT_MODULE_PATH, normalizeModulePath(":"))
    }

    @Test
    fun `the root project has no parent`() {
        assertNull(parentModulePath(ROOT_MODULE_PATH))
        assertEquals(ROOT_MODULE_PATH, parentModulePath(":app"))
        assertEquals(":app", parentModulePath(":app:core"))
    }

    @Test
    fun `containment is compared segment-wise, not as a string prefix`() {
        assertTrue(isModuleWithin(":app:core", ":app"))
        assertTrue(isModuleWithin(":app", ":app"))
        assertTrue(isModuleWithin(":app", ROOT_MODULE_PATH))
        // Ohne segmentweisen Vergleich waere ":applet" faelschlich ein Untermodul von ":app".
        assertFalse(isModuleWithin(":applet", ":app"))
        assertFalse(isModuleWithin(":app", ":app:core"))
    }

    @Test
    fun `task paths are qualified with the owning module`() {
        assertEquals(":buildImage", qualifiedTaskPath(ROOT_MODULE_PATH, "buildImage"))
        assertEquals(":app:buildImage", qualifiedTaskPath(":app", "buildImage"))
        assertEquals(":app:core:test", qualifiedTaskPath(":app:core", "test"))
    }
}
