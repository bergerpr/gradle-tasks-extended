package de.berger.gradletasksview.ui

import kotlin.test.Test
import kotlin.test.assertEquals

/** Deckt das Format der Screenshot-Indexdatei ab (REQ-081). */
class ScreenshotIndexTest {

    @Test
    fun `liest dateiname und bildunterschrift getrennt durch den senkrechten strich`() {
        val entries = ScreenshotIndex.parse("01-views.png | Three view modes")

        assertEquals(listOf(Screenshot("01-views.png", "Three view modes")), entries)
    }

    @Test
    fun `nimmt ohne bildunterschrift den dateinamen`() {
        assertEquals(listOf(Screenshot("a.png", "a.png")), ScreenshotIndex.parse("a.png"))
    }

    @Test
    fun `ueberspringt kommentare und leerzeilen und behaelt die reihenfolge`() {
        val text = """
            # Kommentar
            zweitens.png | B

            erstens.png | A
        """.trimIndent()

        assertEquals(listOf("zweitens.png", "erstens.png"), ScreenshotIndex.parse(text).map { it.fileName })
    }

    @Test
    fun `liefert fuer eine reine kommentardatei nichts`() {
        assertEquals(emptyList(), ScreenshotIndex.parse("# nur ein Hinweis\n\n"))
    }
}
