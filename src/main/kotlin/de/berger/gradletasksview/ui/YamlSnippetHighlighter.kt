package de.berger.gradletasksview.ui

import java.awt.Color

/**
 * Faerbt das YAML-Beispiel im Info-Dialog ein (REQ-080). Bewusst ein winziger eigener
 * Zeilenleser statt des YAML-Lexers der Plattform: Der haengt am optionalen YAML-Plugin, und
 * fuer ein statisches Beispiel aus Schluesseln, Wahrheitswerten und Kommentaren waere das eine
 * Abhaengigkeit fuer nichts.
 *
 * Die Farben kommen als [YamlHighlightPalette] herein, statt hier aus dem Farbschema gelesen
 * zu werden: So bleibt die Zerlegung reine Logik und ohne laufende IDE testbar (REQ-060),
 * waehrend der Dialog die Farben aus dem aktiven Editor-Schema beisteuert.
 */
object YamlSnippetHighlighter {

    /** Kommentar ab dem ersten `#`; im Beispiel kommt keines innerhalb eines Wertes vor. */
    private val COMMENT = Regex("#.*$")

    /** Fuehrende Einrueckung, Schluessel (auch in Anfuehrungszeichen), Doppelpunkt, Rest. */
    private val ENTRY = Regex("""^(\s*)("[^"]*"|[^:]+):(.*)$""")

    private val BOOLEAN = setOf("true", "false")

    fun toHtml(snippet: String, palette: YamlHighlightPalette): String =
        snippet.trimEnd().lines().joinToString("\n") { line -> renderLine(line, palette) }

    private fun renderLine(line: String, palette: YamlHighlightPalette): String {
        val commentAt = COMMENT.find(line)?.range?.first ?: line.length
        return renderCode(line.substring(0, commentAt), palette) +
            span(line.substring(commentAt), palette.comment)
    }

    private fun renderCode(code: String, palette: YamlHighlightPalette): String {
        val entry = ENTRY.matchEntire(code) ?: return escape(code)
        val (indent, key, rest) = entry.destructured
        return escape(indent) + span(key, palette.key) + ":" + renderValue(rest, palette)
    }

    private fun renderValue(rest: String, palette: YamlHighlightPalette): String {
        val value = rest.trim()
        val color = when {
            value in BOOLEAN -> palette.number
            value.startsWith('"') -> palette.string
            else -> null
        } ?: return escape(rest)

        // Abstaende vor und nach dem Wert bleiben ungefaerbt erhalten - an den Leerzeichen
        // dahinter haengt die Ausrichtung der Kommentarspalte im Beispiel.
        return escape(rest.substringBefore(value)) + span(value, color) + escape(rest.substringAfter(value))
    }

    private fun span(text: String, color: Color?): String = when {
        text.isEmpty() -> ""
        color == null -> escape(text)
        else -> "<span style=\"color: ${hex(color)};\">${escape(text)}</span>"
    }

    private fun hex(color: Color): String = "#%02x%02x%02x".format(color.red, color.green, color.blue)

    private fun escape(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}

/**
 * Vier Farben fuer das YAML-Beispiel; `null` heisst "ohne eigene Farbe lassen", falls das
 * Farbschema fuer einen Zweck nichts hergibt.
 */
data class YamlHighlightPalette(
    val key: Color?,
    val string: Color?,
    val number: Color?,
    val comment: Color?,
)
