package de.example.gradletasksview.persistence

import de.example.gradletasksview.model.GradleTaskViewMode
import de.example.gradletasksview.util.isModulePath
import de.example.gradletasksview.util.normalizeModulePath
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File

private val STARTER_DEFAULT_YAML = """
    # Gradle Tasks Extended - Standard-Konfiguration fuer den Expand/Collapse-Zustand
    # von Modulen/Gruppen/Ordnern (gilt in allen drei Ansichtsmodi: Default, Sort und Nested)
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
    #   assembleAll:
    #     icon: smile.svg
    #
    # Nicht aufgefuehrte Eintraege gelten als aufgeklappt und nutzen das eingebaute Icon.
    #
    # --- Modulbezug --------------------------------------------------------------------
    #
    # Ein Schluessel mit fuehrendem ":" ist ein Gradle-Projektpfad und damit ein Modulblock;
    # das Wurzelprojekt heisst ":". Innerhalb des Blocks konfigurieren "expanded" und "icon"
    # den Modulknoten selbst, jeder andere Schluessel gilt als Gruppenpfad nur in diesem Modul:
    #   ":app":
    #     expanded: false
    #     docker/gui/jsp:
    #       expanded: true
    #
    # Modulschluessel bleiben immer absolut - ":app:core" steht auf oberster Ebene, auch wenn
    # es in der Ansicht unter ":app" erscheint.
    #
    # --- Ansichtseinstellungen ----------------------------------------------------------
    #
    # Der reservierte Schluessel "gradle_tasks_extended" traegt die Vorgabe fuer den
    # Feststellbutton der Modulgliederung sowie die Exclude-/Include-Listen:
    #   gradle_tasks_extended:
    #     showModules: true
    #     exclude:
    #       - ":legacy"           # ganzes Modul samt Untermodulen ausblenden
    #     include:
    #       ":legacy":            # ... bis auf diese Gruppenpfade
    #         - docker/gui
    #     modes:
    #       sort:
    #         exclude:
    #           - ":sandbox"      # nur im Sort-Modus zusaetzlich ausblenden
    #
    # Excludes wirken immer, auch bei ausgeschalteter Modulgliederung.
""".trimIndent() + "\n"

/**
 * Liest und schreibt die YAML-Konfigurationsdateien. Fehlerhafte oder von Hand kaputt
 * editierte Dateien fuehren zu einem leeren Zustand statt zu einem Absturz, da es sich um
 * eine von Menschen editierbare externe Datei handelt (Systemgrenze).
 *
 * Das Format ist rueckwaertskompatibel: Knoteneintraege stehen weiterhin flach auf oberster
 * Ebene (kein Wrapper-Schluessel wie `groups:`), und ein Wert darf ein blosser Boolean sein
 * (Kurzform, nur Expand-Zustand) oder eine Abbildung mit `expanded`/`icon` (Langform).
 * Hinzugekommen sind nur zwei Arten von Schluesseln auf derselben Ebene: der reservierte
 * [KEY_VIEW_SETTINGS] und Modulbloecke mit fuehrendem `:` (siehe ADR-014).
 */
object TaskViewConfigYaml {

    private const val KEY_EXPANDED = "expanded"
    private const val KEY_ICON = "icon"

    /**
     * Traegt bewusst den Namen des Plugins und nicht ein generisches Wort wie `view`: Eine
     * echte Gradle-Gruppe mit diesem Namen ist praktisch ausgeschlossen, eine Kollision mit
     * einem Knoteneintrag damit ebenfalls.
     */
    private const val KEY_VIEW_SETTINGS = "gradle_tasks_extended"

    private const val KEY_SHOW_MODULES = "showModules"
    private const val KEY_EXCLUDE = "exclude"
    private const val KEY_INCLUDE = "include"
    private const val KEY_MODES = "modes"

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

    private fun parse(root: Map<String, Any?>): TaskViewConfig {
        val entries = linkedMapOf<String, TaskViewNodeConfig>()
        val modules = linkedMapOf<String, TaskViewModuleConfig>()
        var settings = TaskViewSettings()

        root.forEach { (key, value) ->
            when {
                key == KEY_VIEW_SETTINGS -> settings = parseSettings(value as? Map<*, *> ?: return@forEach)
                isModulePath(key) -> parseModuleBlock(value)
                    ?.let { modules[normalizeModulePath(key)] = it }

                else -> parseNodeConfig(value)?.let { entries[key] = it }
            }
        }

        return TaskViewConfig(entries = entries, modules = modules, settings = settings)
    }

    /**
     * Ein Modulblock ist immer eine Abbildung. Die reservierten Schluessel [KEY_EXPANDED] und
     * [KEY_ICON] beschreiben den Modulknoten selbst, alles andere ist ein Gruppenpfad dieses
     * Moduls - Untermodule stehen nicht hier, sondern als eigener absoluter Schluessel auf
     * oberster Ebene.
     */
    private fun parseModuleBlock(value: Any?): TaskViewModuleConfig? {
        // Kurzform `":app": false` bleibt zulaessig und meint den Modulknoten selbst.
        if (value is Boolean) return TaskViewModuleConfig(node = TaskViewNodeConfig(expanded = value))
        val map = value as? Map<*, *> ?: return null

        val groups = linkedMapOf<String, TaskViewNodeConfig>()
        map.forEach { (rawKey, rawValue) ->
            val key = rawKey as? String ?: return@forEach
            if (key == KEY_EXPANDED || key == KEY_ICON) return@forEach
            parseNodeConfig(rawValue)?.let { groups[key] = it }
        }

        return TaskViewModuleConfig(node = nodeConfigOf(map), groups = groups).takeUnless { it.isEmpty }
    }

    private fun parseSettings(map: Map<*, *>): TaskViewSettings {
        val modes = linkedMapOf<GradleTaskViewMode, TaskViewFilter>()
        (map[KEY_MODES] as? Map<*, *>)?.forEach { (rawKey, rawValue) ->
            val mode = (rawKey as? String)?.let(::parseMode) ?: return@forEach
            val filter = parseFilter(rawValue as? Map<*, *> ?: return@forEach)
            if (!filter.isEmpty) modes[mode] = filter
        }

        return TaskViewSettings(
            showModules = map[KEY_SHOW_MODULES] as? Boolean,
            filter = parseFilter(map),
            modeFilters = modes
        )
    }

    /** Unbekannte Modusnamen werden ignoriert statt die ganze Datei zu verwerfen. */
    private fun parseMode(name: String): GradleTaskViewMode? =
        GradleTaskViewMode.entries.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

    private fun parseFilter(map: Map<*, *>): TaskViewFilter {
        val exclude = (map[KEY_EXCLUDE] as? List<*>).orEmpty()
            .mapNotNull { (it as? String)?.trim()?.takeIf(String::isNotEmpty) }
            .map(::normalizeModulePath)

        val include = linkedMapOf<String, List<String>>()
        (map[KEY_INCLUDE] as? Map<*, *>)?.forEach { (rawKey, rawValue) ->
            val module = (rawKey as? String)?.trim()?.takeIf(String::isNotEmpty) ?: return@forEach
            val groupPaths = (rawValue as? List<*>).orEmpty()
                .mapNotNull { (it as? String)?.trim()?.takeIf(String::isNotEmpty) }
            include[normalizeModulePath(module)] = groupPaths
        }

        return TaskViewFilter(exclude = exclude, include = include)
    }

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
        icon = (map[KEY_ICON] as? String)?.trim()?.takeIf { it.isNotEmpty() }
    )

    fun write(file: File, config: TaskViewConfig) {
        file.parentFile?.mkdirs()

        val serialisable = linkedMapOf<String, Any>()
        if (!config.settings.isEmpty) serialisable[KEY_VIEW_SETTINGS] = serialiseSettings(config.settings)
        // Kurzform beibehalten, solange ein Eintrag nur einen Expand-Zustand hat: Die Dateien
        // sind zum Lesen und Editieren von Hand gedacht, und die ueberwiegende Mehrheit der
        // automatisch geschriebenen Eintraege hat kein Icon.
        config.entries.forEach { (key, entry) -> serialiseNode(entry)?.let { serialisable[key] = it } }
        config.modules.forEach { (key, module) -> serialiseModule(module)?.let { serialisable[key] = it } }

        file.writer().use { Yaml(dumperOptions).dump(serialisable, it) }
    }

    private fun serialiseSettings(settings: TaskViewSettings): Map<String, Any> = buildMap {
        settings.showModules?.let { put(KEY_SHOW_MODULES, it) }
        putAll(serialiseFilter(settings.filter))
        val modes = settings.modeFilters
            .filterValues { !it.isEmpty }
            .map { (mode, filter) -> mode.name.lowercase() to serialiseFilter(filter) }
            .toMap()
        if (modes.isNotEmpty()) put(KEY_MODES, modes)
    }

    private fun serialiseFilter(filter: TaskViewFilter): Map<String, Any> = buildMap {
        if (filter.exclude.isNotEmpty()) put(KEY_EXCLUDE, filter.exclude)
        if (filter.include.isNotEmpty()) put(KEY_INCLUDE, filter.include)
    }

    /**
     * Ein Modulblock wird nur dann zur Kurzform `":app": false` zusammengezogen, wenn er weder
     * ein Icon noch eigene Gruppenpfade traegt - sonst brauchte er ohnehin eine Abbildung.
     */
    private fun serialiseModule(module: TaskViewModuleConfig): Any? {
        if (module.isEmpty) return null
        if (module.groups.isEmpty()) return serialiseNode(module.node)

        return buildMap<String, Any> {
            module.node.expanded?.let { put(KEY_EXPANDED, it) }
            module.node.icon?.let { put(KEY_ICON, it) }
            module.groups.forEach { (key, entry) -> serialiseNode(entry)?.let { put(key, it) } }
        }
    }

    private fun serialiseNode(entry: TaskViewNodeConfig): Any? = when {
        // Explizite Typangabe noetig: Die Abbildung mischt Boolean- und String-Werte.
        entry.icon != null -> buildMap<String, Any> {
            entry.expanded?.let { put(KEY_EXPANDED, it) }
            put(KEY_ICON, entry.icon)
        }

        entry.expanded != null -> entry.expanded
        else -> null
    }

    /** Legt [file] nur an, falls sie noch nicht existiert - eine bereits vorhandene Datei wird nie ueberschrieben. */
    fun createStarterIfMissing(file: File) {
        if (file.exists()) return
        file.parentFile?.mkdirs()
        file.writeText(STARTER_DEFAULT_YAML)
    }
}
