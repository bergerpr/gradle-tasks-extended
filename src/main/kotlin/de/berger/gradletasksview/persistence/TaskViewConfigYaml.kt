package de.berger.gradletasksview.persistence

import de.berger.gradletasksview.model.GradleTaskViewMode
import de.berger.gradletasksview.util.normalizeModulePath
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File

private val STARTER_DEFAULT_YAML = """
    # Gradle Tasks Extended - default configuration of the task tree: expand state, custom
    # icons, tooltips and view settings. This file is meant to be checked into version
    # control and shared with the team; the plugin never overwrites it.
    #
    # The top level has exactly four keys:
    #
    #   settings:   view settings
    #   groups:     group and folder paths, apply in every module
    #   tasks:      task names, apply in every module
    #   modules:    one block per Gradle project path
    #
    # Groups and tasks have separate namespaces, because a name like "build" can be either
    # of them in Gradle.
    #
    # --- Node entries -------------------------------------------------------------------
    #
    # An entry is either a mapping or - if it only sets the expand state - a bare boolean:
    #
    #   groups:
    #     docker/gui: false
    #     docker/gui/jsp:
    #       expanded: true
    #       icon: smile.svg
    #       description: Everything that builds the JSP frontend image
    #
    # Possible fields:
    #   expanded   expand state (no effect on tasks)
    #   icon       file name relative to gradle/gradle_tasks_extended/icons/. If a sibling
    #              file with the suffix "_dark" exists (e.g. smile_dark.svg), it is
    #              preferred automatically under dark themes.
    #   description
    #              tooltip of a module, group or folder node. Without it, those nodes have
    #              no tooltip at all. Tasks ignore it - they show their Gradle description.
    #              The text is written once and applies to every view mode, so it is not
    #              read inside "modes".
    #   modes      values that differ per view mode (default, sort, nested)
    #
    # Nodes that are not listed count as expanded, use the built-in icon and have no tooltip.
    #
    # --- Nodes of your own ---------------------------------------------------------------
    #
    # A group entry may contain "tasks". An entry there with "execute" becomes a row of its
    # own that runs an existing Gradle task - it creates no Gradle task, and neither the name
    # nor the group has to exist beforehand:
    #
    #   groups:
    #     ci/nightly:
    #       tasks:
    #         Build all images:
    #           execute: ":app:buildImage"   # qualified: exactly this module
    #           icon: nightly.svg            #   ":assembleAll": the root project
    #                                        #   "buildImage": every module that has the task
    #
    # Such a row is addressed by its entry, not by the task it runs: hiding that task removes
    # it from its original group only, and this row stays. An entry without "execute" defines
    # nothing - it only configures a row defined elsewhere.
    #
    # --- Module blocks ------------------------------------------------------------------
    #
    #   modules:
    #     ":":                  # root project
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
    # Fields directly inside the block apply to the module node itself, while "groups" and
    # "tasks" apply inside that module only - they take precedence over the entries of the
    # same name at the top level. Module keys stay absolute: ":app:core" sits next to
    # ":app", even though the tree shows it below.
    #
    # --- View settings ------------------------------------------------------------------
    #
    #   settings:
    #     showModules: true          # default for the module grouping toggle
    #     showInheritedTasks: true   # default for the "Show Inherited Tasks" toggle
    #
    # A module node also carries the tasks of its subprojects, unless it has a task of that
    # name itself - the same rows the IDE's own Gradle view shows. They run in every
    # subproject that has the task, are shown in italics, and can be switched off here or
    # from the toolbar. Default is on, as in the original view.
    #
    # --- Hiding nodes -------------------------------------------------------------------
    #
    # "hidden" hides a node together with everything below it and always applies, even with
    # module grouping switched off. The more precise entry wins, so a "hidden: false" takes
    # back a more general one:
    #
    #   modules:
    #     ":legacy":
    #       hidden: true        # hide the module and its submodules ...
    #       groups:
    #         docker/gui:
    #           hidden: false   # ... except this group path
    #     ":sandbox":
    #       modes:
    #         sort:
    #           hidden: true    # hide in Sort mode only
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
    private const val KEY_DESCRIPTION = "description"
    private const val KEY_HIDDEN = "hidden"
    private const val KEY_EXECUTE = "execute"
    private const val KEY_MODES = "modes"

    private const val KEY_SHOW_MODULES = "showModules"
    private const val KEY_SHOW_INHERITED_TASKS = "showInheritedTasks"

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
            // Ohne das tasks:/groups: des Blocks - die gehoeren dem Modul, nicht seinem Knoten.
            node = nodeConfigOf(map).copy(tasks = emptyMap()),
            groups = parseNodeMap(map[KEY_GROUPS]),
            tasks = parseNodeMap(map[KEY_TASKS])
        ).takeUnless { it.isEmpty }
    }

    private fun parseSettings(map: Map<*, *>?): TaskViewSettings = TaskViewSettings(
        showModules = map?.get(KEY_SHOW_MODULES) as? Boolean,
        showInheritedTasks = map?.get(KEY_SHOW_INHERITED_TASKS) as? Boolean
    )

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
        // Dasselbe fuer die Beschreibung: Ein leerer Text waere ein leerer Tooltip, und ein
        // leerer Tooltip ist schlechter als gar keiner (REQ-082).
        description = (map[KEY_DESCRIPTION] as? String)?.trim()?.takeIf { it.isNotEmpty() },
        hidden = map[KEY_HIDDEN] as? Boolean,
        execute = (map[KEY_EXECUTE] as? String)?.trim()?.takeIf { it.isNotEmpty() },
        // Nur bei einem Gruppeneintrag von Bedeutung; anderswo geschrieben bleibt der Wert
        // schlicht wirkungslos, statt beim Lesen die ganze Datei zu verwerfen (REQ-085).
        tasks = parseNodeMap(map[KEY_TASKS]),
        modes = parseNodeModes(map[KEY_MODES])
    )

    /**
     * Modusbezogene Abweichungen eines Knoteneintrags. Ein solcher Eintrag traegt selbst keine
     * [KEY_MODES] mehr - eine zweite Verschachtelung haette keine Bedeutung und wird beim
     * Lesen verworfen.
     *
     * [KEY_DESCRIPTION] wird hier ebenfalls verworfen: Der Erlaeuterungstext eines Knotens soll
     * an genau einer Stelle stehen und in allen Ansichtsmodi derselbe sein (REQ-082).
     *
     * Ebenso [KEY_EXECUTE] und ein verschachteltes [KEY_TASKS]: Ob ein Knoten ueberhaupt
     * existiert, ist keine Frage des Ansichtsmodus - nur seine Sichtbarkeit ist es, und dafuer
     * gibt es `hidden` (REQ-085).
     */
    private fun parseNodeModes(value: Any?): Map<GradleTaskViewMode, TaskViewNodeConfig> {
        val map = value as? Map<*, *> ?: return emptyMap()
        val result = linkedMapOf<GradleTaskViewMode, TaskViewNodeConfig>()
        map.forEach { (rawKey, rawValue) ->
            val mode = (rawKey as? String)?.let(::parseMode) ?: return@forEach
            parseNodeConfig(rawValue)?.allModes?.copy(description = null, execute = null, tasks = emptyMap())
                ?.takeUnless { it.isEmpty }
                ?.let { result[mode] = it }
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
        settings.showInheritedTasks?.let { put(KEY_SHOW_INHERITED_TASKS, it) }
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
        entry.icon == null && entry.description == null && entry.hidden == null &&
            entry.execute == null && entry.tasks.isEmpty() && entry.modes.isEmpty() -> entry.expanded
        else -> nodeFields(entry)
    }

    private fun nodeFields(entry: TaskViewNodeConfig): Map<String, Any> = buildMap {
        entry.expanded?.let { put(KEY_EXPANDED, it) }
        entry.icon?.let { put(KEY_ICON, it) }
        entry.description?.let { put(KEY_DESCRIPTION, it) }
        entry.hidden?.let { put(KEY_HIDDEN, it) }
        entry.execute?.let { put(KEY_EXECUTE, it) }
        serialiseNodeMap(entry.tasks)?.let { put(KEY_TASKS, it) }
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
