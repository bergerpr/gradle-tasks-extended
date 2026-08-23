package de.berger.gradletasksview.model

import de.berger.gradletasksview.util.normalizeGroupPath
import de.berger.gradletasksview.util.normalizeGroupSegments
import kotlin.test.Test
import kotlin.test.assertEquals

class GradleTaskPathUtilTest {

    @Test
    fun `simple group path is split into segments`() {
        assertEquals(listOf("docker", "gui"), normalizeGroupSegments("docker/gui"))
    }

    @Test
    fun `leading trailing and duplicate slashes are removed`() {
        assertEquals(listOf("docker", "gui"), normalizeGroupSegments("/docker//gui/"))
    }

    @Test
    fun `segments are trimmed but otherwise left untouched`() {
        assertEquals(listOf("docker", "gui react"), normalizeGroupSegments(" docker / gui react "))
    }

    @Test
    fun `null or blank group falls back to Other`() {
        assertEquals(listOf(UNNAMED_GROUP_DISPLAY_NAME), normalizeGroupSegments(null))
        assertEquals(listOf(UNNAMED_GROUP_DISPLAY_NAME), normalizeGroupSegments("   "))
        assertEquals(listOf(UNNAMED_GROUP_DISPLAY_NAME), normalizeGroupSegments("///"))
    }

    @Test
    fun `normalizeGroupPath joins segments back with a single slash`() {
        assertEquals("docker/gui/jsp", normalizeGroupPath("/docker//gui/jsp"))
    }
}
