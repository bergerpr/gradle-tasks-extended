package de.berger.gradletasksview

import com.intellij.openapi.diagnostic.logger
import java.util.Properties

/**
 * Die beim Build erzeugte `build-info.properties` aus dem Ressourcen-Wurzelverzeichnis
 * (Gradle-Aufgabe `generateBuildInfo`, siehe Kommentar dort). Sie versorgt den Info-Dialog mit
 * Version, Plugin-ID und Git-Commit (REQ-080).
 *
 * Der naheliegende Weg ueber den eigenen Plugin-Deskriptor scheidet aus: `PluginManagerCore
 * .getPlugin`, `PluginManager.getPluginByClass` und `findEnabledPlugin` sind alle mit
 * `@ApiStatus.Internal` markiert (ADR-008, REQ-052).
 *
 * Fehlt die Datei - etwa weil im Entwicklungsbetrieb ohne die Aufgabe gebaut wurde -, liefern
 * alle Felder [UNKNOWN], statt dass der Dialog scheitert.
 */
object GradleTasksBuildInfo {

    const val UNKNOWN: String = "?"

    private val properties: Properties = load()

    /** Plugin-Version aus `gradle.properties`, z. B. `0.1.0`. */
    val version: String get() = value("version")

    /** Anzeigename des Plugins, identisch mit `<name>` in der `plugin.xml`. */
    val pluginName: String get() = value("pluginName")

    val pluginId: String get() = value("pluginId")

    /** Kleinste unterstuetzte IDE-Build-Nummer, z. B. `262`. */
    val sinceBuild: String get() = value("sinceBuild")

    /** Produktcode und Version der Plattform, gegen die uebersetzt wurde, z. B. `IU 2026.2`. */
    val targetPlatform: String get() = value("targetPlatform")

    /** Herausgeber aus `gradle.properties`, identisch mit `<vendor>` in der `plugin.xml`. */
    val vendor: String get() = value("vendor")

    /** Kontaktadresse, leer wenn in `gradle.properties` nichts eingetragen ist. */
    val vendorEmail: String get() = properties.getProperty("vendorEmail").orEmpty()

    /** Kontakt-URL, leer wenn in `gradle.properties` nichts eingetragen ist. */
    val vendorUrl: String get() = properties.getProperty("vendorUrl").orEmpty()

    /** Kurzer Git-Commit des Standes, aus dem gebaut wurde. */
    val gitCommit: String get() = value("gitCommit")

    /** Git-Branch, auf dem gebaut wurde. */
    val gitBranch: String get() = value("gitBranch")

    /** `true`, wenn das Arbeitsverzeichnis beim Bauen nicht eingecheckte Aenderungen hatte. */
    val gitDirty: Boolean get() = properties.getProperty("gitDirty").toBoolean()

    /** Zeitpunkt, zu dem die Build-Infos zuletzt erzeugt wurden (ISO, ohne Zeitzone). */
    val buildTime: String get() = value("buildTime")

    private fun value(key: String): String = properties.getProperty(key)?.takeIf { it.isNotBlank() } ?: UNKNOWN

    private fun load(): Properties = Properties().apply {
        val stream = GradleTasksBuildInfo::class.java.getResourceAsStream("/build-info.properties")
        if (stream == null) {
            logger<GradleTasksBuildInfo>().warn(
                "build-info.properties fehlt im Plugin - der Info-Dialog zeigt statt der Werte '$UNKNOWN'."
            )
            return@apply
        }
        stream.use { load(it) }
    }
}
