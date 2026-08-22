package de.example.gradletasksview.persistence

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File

/**
 * Konfiguration eines einzelnen Baumknotens.
 *
 * @property expanded Auf-/Zuklappzustand eines Gruppen-/Ordnerknotens. `null` bedeutet
 *   "nicht konfiguriert" - dann gilt die naechste Ebene der Aufloesung bzw. der Standard
 *   "aufgeklappt". Bei Task-Eintraegen immer `null`, da Tasks Blattknoten sind.
 * @property icon Dateiname eines eigenen Icons relativ zu
 *   `gradle/gradle_tasks_extended/icons/`, das das eingebaute Plugin-Icon ersetzt.
 *   `null` bedeutet "eingebautes Icon verwenden".
 */
data class TaskViewNodeConfig(val expanded: Boolean? = null, val icon: String? = null)

/**
 * Inhalt einer `default.yaml`/`custom.yaml`.
 *
 * Schluessel ist entweder ein Gruppen-/Ordnerpfad (`docker/gui/jsp`, oder eine flache Gruppe
 * wie `help` in Default/Sort) oder ein Taskname (`printGroup-docker-server-gui`). Beide teilen
 * sich denselben Namensraum; welcher Eintrag greift, entscheidet der Knotentyp beim Rendern,
 * ein Gruppenpfad kann also nie versehentlich ein Task-Icon setzen.
 *
 * Pfade, die hier nicht vorkommen, gelten als aufgeklappt und verwenden das eingebaute Icon.
 */
data class TaskViewConfig(val entries: Map<String, TaskViewNodeConfig> = emptyMap())

private val STARTER_DEFAULT_YAML = """
    # Gradle Tasks Extended - Standard-Konfiguration fuer den Expand/Collapse-Zustand
    # von Gruppen/Ordnern (gilt in allen drei Ansichtsmodi: Default, Sort und Nested)
    # sowie fuer eigene Icons. Diese Datei kann versioniert und im Team geteilt werden.
    #
    # Kurzform - nur Expand/Collapse, ein Eintrag pro Zeile:
    #   docker/gui/jsp: false
    #
    # Langform - Abbildung mit den optionalen Schluesseln "expanded" und "icon":
    #   docker/gui/jsp:
    #     expanded: false
    #     icon: smile.svg
    #
    # "icon" ist ein Dateiname relativ zu gradle/gradle_tasks_extended/icons/ und ersetzt
    # das eingebaute Icon des Plugins. Existiert daneben eine Datei mit dem Zusatz "_dark"
    # (z. B. smile_dark.svg), wird diese bei dunklen Themes automatisch bevorzugt.
    #
    # Als Schluessel dient bei Gruppen/Ordnern der volle Pfad, bei Tasks der Taskname:
    #   printGroup-docker-server-gui:
    #     icon: smile.svg
    #
    # Nicht aufgefuehrte Eintraege gelten als aufgeklappt und nutzen das eingebaute Icon.
""".trimIndent() + "\n"

/**
 * Liest und schreibt die flachen YAML-Konfigurationsdateien (kein verschachtelter
 * Wrapper-Schluessel wie `groups:`). Fehlerhafte oder von Hand kaputt editierte Dateien
 * fuehren zu einem leeren Zustand statt zu einem Absturz, da es sich um eine von Menschen
 * editierbare externe Datei handelt (Systemgrenze).
 *
 * Das Format ist rueckwaertskompatibel: Ein Wert darf weiterhin ein blosser Boolean sein
 * (Kurzform, nur Expand-Zustand) oder eine Abbildung mit `expanded`/`icon` (Langform).
 */
object TaskViewConfigYaml {

    private const val KEY_EXPANDED = "expanded"
    private const val KEY_ICON = "icon"

    private val dumperOptions = DumperOptions().apply {
        defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
    }

    fun readOrEmpty(file: File): TaskViewConfig {
        if (!file.exists()) return TaskViewConfig()
        return try {
            val root = file.inputStream().use { Yaml().load<Map<String, Any?>>(it) } ?: return TaskViewConfig()
            TaskViewConfig(root.mapNotNull { (key, value) -> parseEntry(key, value) }.toMap())
        } catch (e: Exception) {
            TaskViewConfig()
        }
    }

    private fun parseEntry(key: String, value: Any?): Pair<String, TaskViewNodeConfig>? = when (value) {
        is Boolean -> key to TaskViewNodeConfig(expanded = value)
        is Map<*, *> -> {
            val config = TaskViewNodeConfig(
                expanded = value[KEY_EXPANDED] as? Boolean,
                // Leere oder rein aus Leerzeichen bestehende Angaben wie "icon:" bewusst als
                // "nicht gesetzt" behandeln statt als Dateinamen "" weiterzureichen.
                icon = (value[KEY_ICON] as? String)?.trim()?.takeIf { it.isNotEmpty() }
            )
            // Ein Eintrag ohne jede verwertbare Angabe wird verworfen, damit er sich nicht
            // wie ein gesetzter Wert verhaelt.
            if (config == TaskViewNodeConfig()) null else key to config
        }

        else -> null
    }

    fun write(file: File, config: TaskViewConfig) {
        file.parentFile?.mkdirs()
        // Kurzform beibehalten, solange ein Eintrag nur einen Expand-Zustand hat: Die Dateien
        // sind zum Lesen und Editieren von Hand gedacht, und die ueberwiegende Mehrheit der
        // automatisch geschriebenen Eintraege hat kein Icon.
        val serialisable = config.entries.mapNotNull { (key, entry) ->
            when {
                // Explizite Typangabe noetig: Die Abbildung mischt Boolean- und String-Werte.
                entry.icon != null -> key to buildMap<String, Any> {
                    entry.expanded?.let { put(KEY_EXPANDED, it) }
                    put(KEY_ICON, entry.icon)
                }

                entry.expanded != null -> key to entry.expanded
                else -> null
            }
        }.toMap()

        file.writer().use { Yaml(dumperOptions).dump(serialisable, it) }
    }

    /** Legt [file] nur an, falls sie noch nicht existiert - eine bereits vorhandene Datei wird nie ueberschrieben. */
    fun createStarterIfMissing(file: File) {
        if (file.exists()) return
        file.parentFile?.mkdirs()
        file.writeText(STARTER_DEFAULT_YAML)
    }
}
