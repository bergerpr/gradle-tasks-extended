package de.example.gradletasksview.persistence

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File

/**
 * Expand/Collapse-Zustand pro Gruppen-/Ordnerpfad (z. B. `docker/gui/jsp`, oder eine
 * flache Gruppe wie `help` in Default/Sort). Pfade, die in [groups] nicht vorkommen,
 * gelten als aufgeklappt (siehe [GradleTasksExpandConfigStore]).
 */
data class GroupExpandState(val groups: Map<String, Boolean> = emptyMap())

private val STARTER_DEFAULT_YAML = """
    # Gradle Tasks Extended - Standard-Konfiguration fuer den Expand/Collapse-Zustand
    # von Gruppen/Ordnern (gilt in allen drei Ansichtsmodi: Default, Sort und Nested).
    # Jede Zeile ist "<Gruppen-/Ordnerpfad>: true|false". Nicht aufgefuehrte Pfade
    # gelten als aufgeklappt. Diese Datei kann versioniert und im Team geteilt werden.
    #
    # Beispiel:
    # docker/gui/jsp: false
""".trimIndent() + "\n"

/**
 * Liest und schreibt eine flache YAML-Datei der Form `<Ordnerpfad>: <bool>` (kein
 * verschachtelter Wrapper-Schluessel). Fehlerhafte oder fremd editierte Dateien fuehren
 * zu einem leeren Zustand statt zu einem Absturz, da es sich um eine von Menschen
 * editierbare externe Datei handelt (Systemgrenze).
 */
object GroupExpandYaml {

    private val dumperOptions = DumperOptions().apply {
        defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
    }

    fun readOrEmpty(file: File): GroupExpandState {
        if (!file.exists()) return GroupExpandState()
        return try {
            @Suppress("UNCHECKED_CAST")
            val root = file.inputStream().use { Yaml().load<Map<String, Any?>>(it) } ?: return GroupExpandState()
            val groups = root.mapNotNull { (path, value) -> (value as? Boolean)?.let { path to it } }.toMap()
            GroupExpandState(groups)
        } catch (e: Exception) {
            GroupExpandState()
        }
    }

    fun write(file: File, state: GroupExpandState) {
        file.parentFile?.mkdirs()
        file.writer().use { Yaml(dumperOptions).dump(state.groups, it) }
    }

    /** Legt [file] nur an, falls sie noch nicht existiert - eine bereits vorhandene Datei wird nie ueberschrieben. */
    fun createStarterIfMissing(file: File) {
        if (file.exists()) return
        file.parentFile?.mkdirs()
        file.writeText(STARTER_DEFAULT_YAML)
    }
}
