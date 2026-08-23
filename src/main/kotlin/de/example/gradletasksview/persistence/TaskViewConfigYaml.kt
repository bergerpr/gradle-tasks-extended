package de.example.gradletasksview.persistence

import de.example.gradletasksview.model.GradleTaskViewMode
import de.example.gradletasksview.util.normalizeModulePath
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File

private val STARTER_DEFAULT_YAML = """
    # Gradle Tasks Extended - Standard-Konfiguration der Baumansicht: Aufklappzustand,
    # eigene Icons und Ansichtseinstellungen. Diese Datei kann versioniert und im Team
    # geteilt werden; das Plugin ueberschreibt sie nie.
    #
    # Die oberste Ebene hat genau vier Schluessel:
    #
    #   settings:   Ansichtseinstellungen
    #   groups:     Gruppen-/Ordnerpfade, gelten in allen Modulen
    #   tasks:      Tasknamen, gelten in allen Modulen
    #   modules:    Bloecke je Gradle-Projektpfad
    #
    # Gruppen und Tasks haben getrennte Namensraeume, weil ein Name wie "build" in Gradle
    # beides sein kann.
    #
    # --- Knoteneintraege ----------------------------------------------------------------
    #
    # Ein Eintrag ist entweder eine Abbildung oder - wenn er nur den Aufklappzustand setzt -
    # ein blanker Boolean:
    #
    #   groups:
    #     docker/gui: false
    #     docker/gui/jsp:
    #       expanded: true
    #       icon: smile.svg
    #
    # Moegliche Felder:
    #   expanded   Aufklappzustand (bei Tasks ohne Wirkung)
    #   icon       Dateiname relativ zu gradle/gradle_tasks_extended/icons/. Existiert
    #              daneben eine Datei mit dem Zusatz "_dark" (z. B. smile_dark.svg), wird
    #              sie bei dunklen Themes automatisch bevorzugt.
    #   modes      abweichende Werte je Ansichtsmodus (default, sort, nested)
    #
    # Nicht aufgefuehrte Knoten gelten als aufgeklappt und nutzen das eingebaute Icon.
    #
    # --- Modulbloecke -------------------------------------------------------------------
    #
    #   modules:
    #     ":":                  # Wurzelprojekt
    #       icon: smile.svg
    #     ":app":
    #       expanded: false
    #       groups:
    #         docker/gui/jsp:
    #           expanded: true
    #       tasks:
    #         buildImage:
    #           icon: smile.svg
    #
    # Die Felder direkt im Block gelten fuer den Modulknoten selbst, "groups" und "tasks"
    # nur innerhalb dieses Moduls - sie gehen den gleichnamigen Eintraegen auf oberster
    # Ebene vor. Modulschluessel bleiben absolut: ":app:core" steht neben ":app", auch wenn
    # es in der Ansicht darunter erscheint.
    #
    # --- Ansichtseinstellungen ----------------------------------------------------------
    #
    #   settings:
    #     showModules: true     # Vorgabe fuer den Feststellbutton der Modulgliederung
    #
    # --- Ausblenden ---------------------------------------------------------------------
    #
    # "hidden" blendet einen Knoten samt allem darunter aus und wirkt immer, auch bei
    # ausgeschalteter Modulgliederung. Der genauere Eintrag gewinnt, ein "hidden: false"
    # nimmt eine allgemeinere Ausblendung also wieder zurueck:
    #
    #   modules:
    #     ":legacy":
    #       hidden: true        # Modul samt Untermodulen ausblenden ...
    #       groups:
    #         docker/gui:
    #           hidden: false   # ... ausser diesem Gruppenpfad
    #     ":sandbox":
    #       modes:
    #         sort:
    #           hidden: true    # nur im Sort-Modus ausblenden
""".trimIndent() + "\n"

/**
 * Liest und schreibt die YAML-Konfigurationsdateien. Fehlerhafte oder von Hand kaputt
 * editierte Dateien fuehren zu einem leeren Zustand statt zu einem Absturz, da es sich um
 * eine von Menschen editierbare externe Datei handelt (Systemgrenze).
 *
 * Die oberste Ebene besteht aus vier festen Schluesseln ([KEY_SETTINGS], [KEY_GROUPS],
 * [KEY_TASKS], [KEY_MODULES]); darunter ist kein Schluessel mehr an einer Konvention zu
 * erkennen, sondern durch seinen Ort bestimmt (siehe OPEN-009). Ein Knoteneintrag darf ein
 * blosser Boolean sein (Kurzform, nur Aufklappzustand) oder eine Abbildung.
 *
 * Geschrieben wird durchgaengig in Block-Schreibweise, damit eine versionierte
 * `default.yaml` zeilenweise diffbar bleibt; die gleichbedeutende Flow-Schreibweise
 * (`{ expanded: false }`) wird beim Lesen selbstverstaendlich trotzdem akzeptiert - sie ist
 * dasselbe YAML.
 */
object TaskViewConfigYaml {

    private const val KEY_SETTINGS = "settings"
    private const val KEY_GROUPS = "groups"
    private const val KEY_TASKS = "tasks"
    private const val KEY_MODULES = "modules"

    private const val KEY_EXPANDED = "expanded"
    private const val KEY_ICON = "icon"
    private const val KEY_HIDDEN = "hidden"
    private const val KEY_MODES = "modes"

    private const val KEY_SHOW_MODULES = "showModules"

    private val dumperOptions = DumperOptions().apply {
        defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
    }

    fun readOrEmpty(file: File): TaskViewConfig {
        if (!file.exists()) return TaskViewConfig()
        return try {
            val root = file.inputStream().use { Yaml().load<Map<String, Any?>>(it) } ?: return TaskViewConfig()
            parse(root)
        } catch (e: Exception) {
            TaskViewConfig()
        }
    }

    private fun parse(root: Map<String, Any?>): TaskViewConfig = TaskViewConfig(
        settings = parseSettings(root[KEY_SETTINGS] as? Map<*, *>),
        groups = parseNodeMap(root[KEY_GROUPS]),
        tasks = parseNodeMap(root[KEY_TASKS]),
        modules = parseModules(root[KEY_MODULES])
    )

    private fun parseNodeMap(value: Any?): Map<String, TaskViewNodeConfig> {
        val map = value as? Map<*, *> ?: return emptyMap()
        val result = linkedMapOf<String, TaskViewNodeConfig>()
        map.forEach { (rawKey, rawValue) ->
            val key = (rawKey as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: return@forEach
            parseNodeConfig(rawValue)?.let { result[key] = it }
        }
        return result
    }

    private fun parseModules(value: Any?): Map<String, TaskViewModuleConfig> {
        val map = value as? Map<*, *> ?: return emptyMap()
        val result = linkedMapOf<String, TaskViewModuleConfig>()
        map.forEach { (rawKey, rawValue) ->
            val key = (rawKey as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: return@forEach
            parseModuleBlock(rawValue)?.let { result[normalizeModulePath(key)] = it }
        }
        return result
    }

    /**
     * Ein Modulblock ist eine Abbildung: Die Knotenfelder beschreiben den Modulknoten selbst,
     * [KEY_GROUPS] und [KEY_TASKS] gelten nur innerhalb dieses Moduls. Untermodule stehen
     * nicht hier, sondern als eigener absoluter Schluessel unter [KEY_MODULES].
     */
    private fun parseModuleBlock(value: Any?): TaskViewModuleConfig? {
        // Kurzform `":app": false` bleibt zulaessig und meint den Modulknoten selbst.
        if (value is Boolean) return TaskViewModuleConfig(node = TaskViewNodeConfig(expanded = value))
        val map = value as? Map<*, *> ?: return null

        return TaskViewModuleConfig(
            node = nodeConfigOf(map),
            groups = parseNodeMap(map[KEY_GROUPS]),
            tasks = parseNodeMap(map[KEY_TASKS])
        ).takeUnless { it.isEmpty }
    }

    private fun parseSettings(map: Map<*, *>?): TaskViewSettings =
        TaskViewSettings(showModules = map?.get(KEY_SHOW_MODULES) as? Boolean)

    /** Unbekannte Modusnamen werden ignoriert statt die ganze Datei zu verwerfen. */
    private fun parseMode(name: String): GradleTaskViewMode? =
        GradleTaskViewMode.entries.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

    private fun parseNodeConfig(value: Any?): TaskViewNodeConfig? = when (value) {
        is Boolean -> TaskViewNodeConfig(expanded = value)
        // Ein Eintrag ohne jede verwertbare Angabe wird verworfen, damit er sich nicht
        // wie ein gesetzter Wert verhaelt.
        is Map<*, *> -> nodeConfigOf(value).takeUnless { it.isEmpty }
        else -> null
    }

    private fun nodeConfigOf(map: Map<*, *>) = TaskViewNodeConfig(
        expanded = map[KEY_EXPANDED] as? Boolean,
        // Leere oder rein aus Leerzeichen bestehende Angaben wie "icon:" bewusst als
        // "nicht gesetzt" behandeln statt als Dateinamen "" weiterzureichen.
        icon = (map[KEY_ICON] as? String)?.trim()?.takeIf { it.isNotEmpty() },
        hidden = map[KEY_HIDDEN] as? Boolean,
        modes = parseNodeModes(map[KEY_MODES])
    )

    /**
     * Modusbezogene Abweichungen eines Knoteneintrags. Ein solcher Eintrag traegt selbst keine
     * [KEY_MODES] mehr - eine zweite Verschachtelung haette keine Bedeutung und wird beim
     * Lesen verworfen.
     */
    private fun parseNodeModes(value: Any?): Map<GradleTaskViewMode, TaskViewNodeConfig> {
        val map = value as? Map<*, *> ?: return emptyMap()
        val result = linkedMapOf<GradleTaskViewMode, TaskViewNodeConfig>()
        map.forEach { (rawKey, rawValue) ->
            val mode = (rawKey as? String)?.let(::parseMode) ?: return@forEach
            parseNodeConfig(rawValue)?.allModes?.takeUnless { it.isEmpty }?.let { result[mode] = it }
        }
        return result
    }

    fun write(file: File, config: TaskViewConfig) {
        file.parentFile?.mkdirs()

        val serialisable = linkedMapOf<String, Any>()
        if (!config.settings.isEmpty) serialisable[KEY_SETTINGS] = serialiseSettings(config.settings)
        serialiseNodeMap(config.groups)?.let { serialisable[KEY_GROUPS] = it }
        serialiseNodeMap(config.tasks)?.let { serialisable[KEY_TASKS] = it }

        val modules = config.modules.mapNotNull { (key, module) -> serialiseModule(module)?.let { key to it } }
        if (modules.isNotEmpty()) serialisable[KEY_MODULES] = modules.toMap()

        file.writer().use { Yaml(dumperOptions).dump(serialisable, it) }
    }

    private fun serialiseNodeMap(entries: Map<String, TaskViewNodeConfig>): Map<String, Any>? {
        val serialised = entries.mapNotNull { (key, entry) -> serialiseNode(entry)?.let { key to it } }
        return serialised.toMap().takeIf { it.isNotEmpty() }
    }

    private fun serialiseSettings(settings: TaskViewSettings): Map<String, Any> = buildMap {
        settings.showModules?.let { put(KEY_SHOW_MODULES, it) }
    }

    /**
     * Ein Modulblock wird nur dann zur Kurzform `":app": false` zusammengezogen, wenn er
     * ausser dem Aufklappzustand des Modulknotens nichts traegt - sonst braucht er ohnehin
     * eine Abbildung.
     */
    private fun serialiseModule(module: TaskViewModuleConfig): Any? {
        if (module.isEmpty) return null
        if (module.groups.isEmpty() && module.tasks.isEmpty()) return serialiseNode(module.node)

        return buildMap<String, Any> {
            putAll(nodeFields(module.node))
            serialiseNodeMap(module.groups)?.let { put(KEY_GROUPS, it) }
            serialiseNodeMap(module.tasks)?.let { put(KEY_TASKS, it) }
        }
    }

    /**
     * Kurzform, solange ein Eintrag nur einen Aufklappzustand hat: Die Dateien sind zum Lesen
     * und Editieren von Hand gedacht, und die ueberwiegende Mehrheit der automatisch
     * geschriebenen Eintraege traegt nichts weiter.
     */
    private fun serialiseNode(entry: TaskViewNodeConfig): Any? = when {
        entry.isEmpty -> null
        entry.icon == null && entry.hidden == null && entry.modes.isEmpty() -> entry.expanded
        else -> nodeFields(entry)
    }

    private fun nodeFields(entry: TaskViewNodeConfig): Map<String, Any> = buildMap {
        entry.expanded?.let { put(KEY_EXPANDED, it) }
        entry.icon?.let { put(KEY_ICON, it) }
        entry.hidden?.let { put(KEY_HIDDEN, it) }
        val modes = entry.modes.mapNotNull { (mode, config) ->
            nodeFields(config.allModes).takeIf { it.isNotEmpty() }?.let { mode.name.lowercase() to it }
        }
        if (modes.isNotEmpty()) put(KEY_MODES, modes.toMap())
    }

    /** Legt [file] nur an, falls sie noch nicht existiert - eine bereits vorhandene Datei wird nie ueberschrieben. */
    fun createStarterIfMissing(file: File) {
        if (file.exists()) return
        file.parentFile?.mkdirs()
        file.writeText(STARTER_DEFAULT_YAML)
    }
}
