package de.berger.gradletasksview.ui

import java.awt.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Deckt die Zerlegung des YAML-Beispiels ab (REQ-080). Die Farben werden hineingereicht,
 * deshalb kommt der Test ohne laufende IDE aus.
 */
class YamlSnippetHighlighterTest {

    private val palette = YamlHighlightPalette(
        key = Color(0x00, 0x00, 0x80),
        string = Color(0x00, 0x80, 0x00),
        number = Color(0x00, 0x00, 0xFF),
        comment = Color(0x80, 0x80, 0x80),
    )

    @Test
    fun `faerbt schluessel wert und kommentar getrennt ein`() {
        val html = YamlSnippetHighlighter.toHtml("  showModules: true          # Schalter", palette)

        assertEquals(
            "  <span style=\"color: #000080;\">showModules</span>:" +
                " <span style=\"color: #0000ff;\">true</span>          " +
                "<span style=\"color: #808080;\"># Schalter</span>",
            html,
        )
    }

    @Test
    fun `behandelt einen schluessel in anfuehrungszeichen als ganzes`() {
        val html = YamlSnippetHighlighter.toHtml("""  ":app:core":""", palette)

        assertTrue(html.contains("<span style=\"color: #000080;\">&quot;:app:core&quot;</span>:") ||
            html.contains("<span style=\"color: #000080;\">\":app:core\"</span>:"), html)
    }

    @Test
    fun `laesst eine zeile ohne doppelpunkt unveraendert`() {
        assertEquals("nur text", YamlSnippetHighlighter.toHtml("nur text", palette))
    }

    @Test
    fun `maskiert spitze klammern damit sie nicht als markup gelesen werden`() {
        val html = YamlSnippetHighlighter.toHtml("  icon: <name>_dark.svg", palette)

        assertTrue(html.endsWith(": &lt;name&gt;_dark.svg"), html)
    }

    @Test
    fun `laesst fehlende farben weg statt leere spans zu erzeugen`() {
        val ohneFarben = YamlHighlightPalette(null, null, null, null)

        assertEquals("  showModules: true # Schalter", YamlSnippetHighlighter.toHtml("  showModules: true # Schalter", ohneFarben))
    }
}
