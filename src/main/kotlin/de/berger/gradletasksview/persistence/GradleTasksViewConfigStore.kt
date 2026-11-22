package de.berger.gradletasksview.persistence

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import de.berger.gradletasksview.model.GradleTaskViewMode
import de.berger.gradletasksview.model.VirtualTaskDefinition
import de.berger.gradletasksview.model.VisibilityRule
import de.berger.gradletasksview.model.isNodeHidden
import de.berger.gradletasksview.model.VisibilityTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.Icon

/**
 * Zweistufige, dateibasierte Konfiguration der Baumansicht - Aufklappzustand von
 * Modul-/Gruppen-/Ordnerknoten, eigene Icons, Tooltips, ausgeblendete Knoten und die Vorgaben
 * fuer die Modulgliederung und die geerbten Sammelzeilen:
 *
 * - `<Projektwurzel>/gradle/gradle_tasks_extended/default.yaml`: Team-weite Vorgabe,
 *   die versioniert werden kann. Wird nur angelegt, wenn sie noch nicht existiert -
 *   eine bereits vorhandene Datei wird niemals durch das Plugin ueberschrieben.
 * - `<Projektwurzel>/gradle/gradle_tasks_extended/custom.yaml`: Persoenliche
 *   Abweichungen, die entstehen, sobald der Benutzer selbst eine Gruppe/einen Ordner
 *   auf-/zuklappt oder die Modulgliederung umschaltet. Wird ausschliesslich vom Plugin
 *   geschrieben.
 * - `<Projektwurzel>/gradle/gradle_tasks_extended/icons/`: Ablageort fuer eigene
 *   Icon-Dateien, auf die die YAML-Dateien per Dateinamen verweisen.
 *
 * Aufloesung pro Knoten und Feld, sortiert nach drei Kriterien in dieser Reihenfolge:
 * **Modulbezug vor modulunabhaengig**, dann **modusbezogen vor modusuebergreifend**, dann
 * **`custom.yaml` vor `default.yaml`**. Der spezifischere Schluessel gewinnt also vor der
 * persoenlichen Datei - sonst koennte eine einmal global gesetzte Vorliebe eine bewusst pro
 * Modul gepflegte Team-Vorgabe nie zur Geltung kommen lassen. Greift nichts, gilt der
 * eingebaute Standard (aufgeklappt bzw. Plugin-Icon).
 *
 * Fuer die Sichtbarkeit gilt dieselbe Reihenfolge, nur zusaetzlich um die Genauigkeit des
 * Eintrags erweitert (Task vor Ordner vor Modul) - sie wird in [VisibilityRule] flachgeklopft
 * und im Modell ausgewertet.
 *
 * `Service.Level.PROJECT`, da die Konfigurationsdateien projektbezogen sind (siehe
 * REQ-042 zu mehreren offenen Projekten).
 */
@Service(Service.Level.PROJECT)
class GradleTasksViewConfigStore(private val project: Project, private val cs: CoroutineScope) {

    private val configDir: File
        get() = File(project.basePath ?: ".", "gradle/gradle_tasks_extended")

    private val defaultFile: File get() = File(configDir, "default.yaml")
    private val customFile: File get() = File(configDir, "custom.yaml")
    private val iconDir: File get() = File(configDir, "icons")

    /**
     * Die drei Orte der Konfiguration nach aussen, fuer die Toolbar-Aktionen "Edit Default
     * Configuration" und "Configuration Help" (REQ-080). Sie zeigen dieselben Pfade an, die
     * dieser Store benutzt - sonst koennte die Hilfe auf ein Verzeichnis verweisen, aus dem
     * gar nicht gelesen wird.
     */
    val defaultConfigFile: File get() = defaultFile
    val customConfigFile: File get() = customFile
    val iconsDirectory: File get() = iconDir

    /**
     * Legt `default.yaml` mit dem kommentierten Startinhalt an, falls sie fehlt, und liefert
     * sie zurueck. Vorhandene Dateien bleiben unberuehrt (REQ-070).
     */
    fun ensureDefaultConfigExists(): File = defaultFile.also(TaskViewConfigYaml::createStarterIfMissing)

    @Volatile private var defaultState: TaskViewConfig = TaskViewConfig()
    @Volatile private var customState: TaskViewConfig = TaskViewConfig()

    /** Zeitstempel der zuletzt eingelesenen Dateien, um in [reloadIfChanged] unnoetiges Parsen zu sparen. */
    @Volatile private var loadedStamps: Pair<Long, Long> = 0L to 0L

    /**
     * Anzahl der noch laufenden Schreibvorgaenge auf [customFile]. Solange einer laeuft, ist der
     * Zustand im Speicher neuer als der auf der Platte - ein Neueinlesen wuerde die gerade
     * getaetigte Aenderung verwerfen oder, schlimmer, eine halb geschriebene Datei sehen und
     * als leer verwerfen. Das wird erreichbar, seit der Feststellbutton unmittelbar nach dem
     * Schreiben einen Refresh ausloest.
     */
    private val pendingWrites = AtomicInteger()

    /**
     * Zwischenspeicher fuer aus Dateien geladene Icons. Der Schluessel enthaelt bewusst auch
     * die Theme-Helligkeit, damit ein Wechsel zwischen hellem und dunklem Theme zur Laufzeit
     * nicht die Variante des jeweils anderen Themes weiterverwendet.
     */
    private val iconCache = ConcurrentHashMap<String, Optional>()

    /** Kleiner Ersatz fuer `java.util.Optional`, damit auch "gibt es nicht" zwischengespeichert wird. */
    private class Optional(val icon: Icon?)

    init {
        // Kleine, lokale Dateien - synchrones Lesen im Service-Init ist unproblematisch
        // (Light Services werden bereits abseits des EDT konstruiert).
        try {
            TaskViewConfigYaml.createStarterIfMissing(defaultFile)
            reload()
        } catch (e: Exception) {
            LOG.warn("Konnte Konfiguration unter $configDir nicht laden", e)
        }
    }

    fun isExpanded(key: TaskViewConfigKey): Boolean = resolve(key) { it.expanded } ?: true

    /**
     * Eigenes Icon fuer [key] oder `null`, wenn keines konfiguriert ist bzw. die Datei fehlt -
     * dann bleibt es beim eingebauten Icon des Plugins.
     */
    fun iconFor(key: TaskViewConfigKey): Icon? {
        val fileName = resolve(key) { it.icon } ?: return null
        val cacheKey = "$fileName|bright=${JBColor.isBright()}"
        return iconCache.getOrPut(cacheKey) { Optional(loadIcon(fileName)) }.icon
    }

    /**
     * Erlaeuternder Text fuer [key] oder `null`, wenn keiner konfiguriert ist. Er wird als
     * Tooltip des Knotens angezeigt; ohne Eintrag hat der Knoten gar keinen Tooltip (REQ-082).
     *
     * Gilt nur fuer Modul-, Gruppen- und Ordnerknoten - ein Task zeigt weiterhin die
     * Beschreibung, die Gradle selbst mitliefert, und fragt hier gar nicht erst nach.
     */
    fun descriptionFor(key: TaskViewConfigKey): String? = resolve(key) { it.description }

    /**
     * Liest beide Dateien neu ein, ohne auf einen geaenderten Zeitstempel zu warten, und wirft
     * den Icon-Zwischenspeicher weg.
     *
     * Gebraucht wird das fuer "Reload" im Zahnrad-Menue (REQ-084): Eine neu ins
     * `icons/`-Verzeichnis gelegte Datei aendert den Zeitstempel keiner der beiden
     * YAML-Dateien, [reloadIfChanged] wuerde also nichts tun und das Icon bliebe bis zum
     * naechsten IDE-Start das alte.
     */
    fun forceReload() {
        try {
            // Laeuft gerade ein Schreibvorgang auf custom.yaml, ist der Zustand im Speicher
            // neuer als der auf der Platte - dann nur den Icon-Zwischenspeicher wegwerfen,
            // statt die gerade getaetigte Aenderung wieder einzulesen (siehe [reloadIfChanged]).
            if (pendingWrites.get() > 0) iconCache.clear() else reload()
        } catch (e: Exception) {
            LOG.warn("Konnte Konfiguration unter $configDir nicht neu laden", e)
        }
    }

    /**
     * Zustand des Feststellbuttons fuer die Modulgliederung. Standard ist **aus**, damit sich
     * fuer bestehende Projekte ohne Konfiguration nichts aendert.
     */
    fun showModules(): Boolean =
        customState.settings.showModules ?: defaultState.settings.showModules ?: false

    fun setShowModules(showModules: Boolean) {
        reloadIfChanged()
        // Nur festhalten, was von der Vorgabe abweicht - siehe [setExpanded]. Kopiert wird
        // der bestehende Einstellungsblock, damit das Umschalten nicht die daneben stehende
        // Einstellung mitloescht.
        val fallback = defaultState.settings.showModules ?: false
        customState = customState.copy(
            settings = customState.settings.copy(showModules = showModules.takeIf { it != fallback })
        )
        persistCustomState()
    }

    /**
     * Zeigt die geerbten Sammelzeilen der Unterprojekte mit an (REQ-088). Standard ist **an**,
     * so wie `showInheritedTasks` in der originalen Gradle-Ansicht: Ein Projekt, das seine
     * Plugins zentral fuer alle Unterprojekte setzt, hat auf Wurzelebene sonst gar keine
     * Tasks.
     */
    fun showInheritedTasks(): Boolean =
        customState.settings.showInheritedTasks ?: defaultState.settings.showInheritedTasks ?: true

    fun setShowInheritedTasks(showInheritedTasks: Boolean) {
        reloadIfChanged()
        val fallback = defaultState.settings.showInheritedTasks ?: true
        customState = customState.copy(
            settings = customState.settings.copy(
                showInheritedTasks = showInheritedTasks.takeIf { it != fallback }
            )
        )
        persistCustomState()
    }

    /**
     * Alle im Modus [mode] wirksamen Sichtbarkeitsregeln, flachgeklopft fuer die Modelllogik.
     * Die Reihenfolge der Liste spielt keine Rolle - welche Regel gewinnt, entscheidet der
     * Vergleich in [de.berger.gradletasksview.model.filterHiddenTasks].
     */
    fun visibilityRules(mode: GradleTaskViewMode): List<VisibilityRule> = visibilityRules(mode, skipCustomEntryFor = null)

    /**
     * @param skipCustomEntryFor laesst den Eintrag aus `custom.yaml` aus, den dieser
     *   Schluessel selbst adressiert - die Grundlage dafuer, nur echte Abweichungen zu
     *   speichern (siehe [setHidden]).
     */
    private fun visibilityRules(
        mode: GradleTaskViewMode,
        skipCustomEntryFor: TaskViewConfigKey?
    ): List<VisibilityRule> =
        rulesOf(defaultState, mode, fromCustomFile = false, skip = null) +
            rulesOf(customState, mode, fromCustomFile = true, skip = skipCustomEntryFor)

    /** Ist der von [key] beschriebene Knoten im aktuellen Modus ausgeblendet? */
    fun isHidden(key: TaskViewConfigKey): Boolean = isHidden(key, skipOwnCustomEntry = false)

    private fun isHidden(key: TaskViewConfigKey, skipOwnCustomEntry: Boolean): Boolean {
        val mode = GradleTaskViewSettings.getInstance(project).state.selectedMode
        val rules = visibilityRules(mode, skipCustomEntryFor = key.takeIf { skipOwnCustomEntry })
        return isNodeHidden(
            rules,
            modulePath = when (key) {
                is TaskViewConfigKey.Module -> key.modulePath
                is TaskViewConfigKey.Group -> key.modulePath
                is TaskViewConfigKey.Task -> key.modulePath
                is TaskViewConfigKey.VirtualTask -> key.modulePath
            },
            groupName = (key as? TaskViewConfigKey.Group)?.groupPath
                ?: (key as? TaskViewConfigKey.VirtualTask)?.groupPath,
            taskName = (key as? TaskViewConfigKey.Task)?.taskName,
            virtualTaskName = (key as? TaskViewConfigKey.VirtualTask)?.name
        )
    }

    /**
     * Blendet den von [key] beschriebenen Knoten aus oder wieder ein.
     *
     * Wie [setExpanded] wird nur festgehalten, was von dem abweicht, was ohne diesen Eintrag
     * ohnehin gaelte. Ein Knoten, der bereits durch ein ausgeblendetes Modul verschwindet,
     * bekommt also keinen eigenen Eintrag, und ein Wiedereinblenden traegt `hidden: false`
     * genau dann ein, wenn eine allgemeinere Regel dagegensteht.
     */
    fun setHidden(key: TaskViewConfigKey, hidden: Boolean) {
        reloadIfChanged()
        val withoutOwnEntry = isHidden(key, skipOwnCustomEntry = true)
        customState = if (hidden == withoutOwnEntry) {
            customState.withNode(key) { it.copy(hidden = null) }.withoutEmptyEntries()
        } else {
            customState.withNode(key) { it.copy(hidden = hidden) }
        }
        persistCustomState()
    }

    private fun rulesOf(
        config: TaskViewConfig,
        mode: GradleTaskViewMode,
        fromCustomFile: Boolean,
        skip: TaskViewConfigKey?
    ): List<VisibilityRule> = buildList {
        config.groups.forEach { (path, entry) ->
            if (skip != TaskViewConfigKey.group(path, null)) {
                addRules(entry, VisibilityTarget.Group(path), scope = null, mode, fromCustomFile)
            }
            addVirtualTaskRules(entry, groupPath = path, scope = null, mode, fromCustomFile, skip)
        }
        config.tasks.forEach { (name, entry) ->
            if (skip != TaskViewConfigKey.task(name, null)) {
                addRules(entry, VisibilityTarget.Task(name), scope = null, mode, fromCustomFile)
            }
        }
        config.modules.forEach { (path, block) ->
            // Der Modulknoten selbst: Sein Geltungsbereich ist das Modul, das er benennt -
            // ein zusaetzlicher Scope waere dieselbe Angabe doppelt.
            if (skip != TaskViewConfigKey.module(path)) {
                addRules(block.node, VisibilityTarget.Module(path), scope = null, mode, fromCustomFile)
            }
            block.groups.forEach { (group, entry) ->
                if (skip != TaskViewConfigKey.group(group, path)) {
                    addRules(entry, VisibilityTarget.Group(group), scope = path, mode, fromCustomFile)
                }
                addVirtualTaskRules(entry, groupPath = group, scope = path, mode, fromCustomFile, skip)
            }
            block.tasks.forEach { (task, entry) ->
                if (skip != TaskViewConfigKey.task(task, path)) {
                    addRules(entry, VisibilityTarget.Task(task), scope = path, mode, fromCustomFile)
                }
            }
        }
    }

    /**
     * Sichtbarkeitsregeln der ueber `execute` konfigurierten Knoten eines Gruppeneintrags
     * (REQ-085). Sie zielen auf Gruppenpfad + Name des Eintrags, nie auf den ausgefuehrten
     * Task - deshalb nimmt ein `hidden` auf diesen Task sie nicht mit (ADR-023).
     */
    private fun MutableList<VisibilityRule>.addVirtualTaskRules(
        groupEntry: TaskViewNodeConfig,
        groupPath: String,
        scope: String?,
        mode: GradleTaskViewMode,
        fromCustomFile: Boolean,
        skip: TaskViewConfigKey?
    ) {
        groupEntry.tasks.forEach { (name, entry) ->
            if (skip != TaskViewConfigKey.virtualTask(groupPath, name, scope)) {
                addRules(entry, VisibilityTarget.VirtualTask(groupPath, name), scope, mode, fromCustomFile)
            }
        }
    }

    /**
     * Alle ueber `execute` konfigurierten Knoten beider Dateien (REQ-085).
     *
     * Der Verweis selbst kommt aus der normalen Aufloesung, damit eine persoenliche
     * `custom.yaml` das `execute` der Team-Vorgabe ersetzen kann. Ein Eintrag ohne Verweis
     * definiert keinen Knoten, sondern konfiguriert nur einen anderswo definierten - genau die
     * Form, die beim Ausblenden in `custom.yaml` landet.
     */
    fun virtualTaskDefinitions(): List<VirtualTaskDefinition> {
        // Nur Eintraege, die selbst ein execute tragen, sind Definitionen. Ein Eintrag ohne
        // execute konfiguriert einen anderswo definierten Knoten - genau die Form, die beim
        // Ausblenden in custom.yaml landet, und die wuerde sonst eine zweite Zeile erzeugen.
        val addresses = LinkedHashSet<Triple<String?, String, String>>()
        listOf(defaultState, customState).forEach { config ->
            config.groups.forEach { (group, entry) ->
                entry.tasks.forEach { (name, node) -> if (node.execute != null) addresses += Triple(null, group, name) }
            }
            config.modules.forEach { (module, block) ->
                block.groups.forEach { (group, entry) ->
                    entry.tasks.forEach { (name, node) ->
                        if (node.execute != null) addresses += Triple(module, group, name)
                    }
                }
            }
        }

        return addresses.mapNotNull { (scope, group, name) ->
            val execute = resolve(TaskViewConfigKey.virtualTask(group, name, scope)) { it.execute }
                ?: return@mapNotNull null
            VirtualTaskDefinition(groupPath = group, displayName = name, execute = execute, scopeModulePath = scope)
        }
    }

    private fun MutableList<VisibilityRule>.addRules(
        entry: TaskViewNodeConfig,
        target: VisibilityTarget,
        scope: String?,
        mode: GradleTaskViewMode,
        fromCustomFile: Boolean
    ) {
        entry.hidden?.let {
            add(VisibilityRule(target, scope, hidden = it, modeSpecific = false, fromCustomFile = fromCustomFile))
        }
        entry.modes[mode]?.hidden?.let {
            add(VisibilityRule(target, scope, hidden = it, modeSpecific = true, fromCustomFile = fromCustomFile))
        }
    }

    /**
     * Haelt den Aufklappzustand von [key] fest - aber nur, wenn er von dem abweicht, was ohne
     * diesen Eintrag ohnehin gaelte. Stimmt er wieder mit ihm ueberein, verschwindet der
     * Eintrag stattdessen.
     *
     * Sonst schriebe ein einziges "Collapse All" eine Wand aus Eintraegen in die Datei, die
     * fast alle nur den Standard wiederholen. Der Preis ist bewusst in Kauf genommen: Aendert
     * sich spaeter die `default.yaml`, verschiebt sich der persoenliche Zustand mit - er ist
     * als Abweichung davon definiert, nicht als Momentaufnahme.
     *
     * Die uebrigen Felder des Eintrags bleiben unberuehrt, sonst wuerde das erste
     * Auf-/Zuklappen eine in `custom.yaml` gepflegte Icon-Zuordnung loeschen.
     */
    fun setExpanded(key: TaskViewConfigKey, expanded: Boolean) {
        reloadIfChanged()
        val withoutOwnEntry = resolve(key, skipCustomEntry = true) { it.expanded } ?: true
        customState = if (expanded == withoutOwnEntry) {
            customState.withNode(key) { it.copy(expanded = null) }.withoutEmptyEntries()
        } else {
            customState.withNode(key) { it.copy(expanded = expanded) }
        }
        persistCustomState()
    }

    /**
     * Aendert genau den Eintrag, den [key] adressiert, und legt ihn an, falls es ihn noch
     * nicht gibt. Der Ort in der Datei ergibt sich allein aus der Form des Schluessels.
     */
    private fun TaskViewConfig.withNode(
        key: TaskViewConfigKey,
        update: (TaskViewNodeConfig) -> TaskViewNodeConfig
    ): TaskViewConfig = when (key) {
        is TaskViewConfigKey.Module -> withModule(key.modulePath) { it.copy(node = update(it.node)) }

        is TaskViewConfigKey.Group -> if (key.modulePath == null) {
            copy(groups = groups.updated(key.groupPath, update))
        } else {
            withModule(key.modulePath) { it.copy(groups = it.groups.updated(key.groupPath, update)) }
        }

        is TaskViewConfigKey.Task -> if (key.modulePath == null) {
            copy(tasks = tasks.updated(key.taskName, update))
        } else {
            withModule(key.modulePath) { it.copy(tasks = it.tasks.updated(key.taskName, update)) }
        }

        // Der Eintrag sitzt im Gruppeneintrag, nicht im tasks:-Namensraum - dort stehen die
        // Namen echter Gradle-Tasks (REQ-085).
        is TaskViewConfigKey.VirtualTask -> if (key.modulePath == null) {
            copy(groups = groups.updatedNested(key.groupPath, key.name, update))
        } else {
            withModule(key.modulePath) { it.copy(groups = it.groups.updatedNested(key.groupPath, key.name, update)) }
        }
    }

    private fun TaskViewConfig.withModule(
        modulePath: String,
        update: (TaskViewModuleConfig) -> TaskViewModuleConfig
    ): TaskViewConfig = copy(
        modules = modules + (modulePath to update(modules[modulePath] ?: TaskViewModuleConfig()))
    )

    private fun Map<String, TaskViewNodeConfig>.updated(
        key: String,
        update: (TaskViewNodeConfig) -> TaskViewNodeConfig
    ): Map<String, TaskViewNodeConfig> = this + (key to update(this[key] ?: TaskViewNodeConfig()))

    /** Aendert einen Knoten im `tasks:` eines Gruppeneintrags und legt beide bei Bedarf an. */
    private fun Map<String, TaskViewNodeConfig>.updatedNested(
        groupPath: String,
        name: String,
        update: (TaskViewNodeConfig) -> TaskViewNodeConfig
    ): Map<String, TaskViewNodeConfig> = updated(groupPath) { group ->
        group.copy(tasks = group.tasks.updated(name, update))
    }

    /**
     * Liest beide Dateien neu ein, falls sich ihr Zeitstempel geaendert hat. Dadurch wirkt eine
     * von Hand editierte `default.yaml` beim naechsten Refresh der Ansicht, ohne dass die IDE
     * neu gestartet werden muss.
     *
     * Jede Aenderung an `custom.yaml` (`setExpanded`, `setHidden`, `setShowModules`,
     * `setShowInheritedTasks`) ruft das zuerst auf: Sie schreibt den Zustand im Speicher
     * vollstaendig zurueck, und war die Datei seit dem letzten Refresh von Hand geaendert
     * worden, haette schon ein einziges Auf- oder Zuklappen diese Aenderung ueberschrieben
     * (REQ-076).
     */
    fun reloadIfChanged() {
        if (pendingWrites.get() > 0) return
        if (currentStamps() == loadedStamps) return
        try {
            reload()
        } catch (e: Exception) {
            LOG.warn("Konnte Konfiguration unter $configDir nicht neu laden", e)
        }
    }

    /**
     * Loescht alle persoenlichen Abweichungen - danach gilt wieder ausschliesslich [defaultFile].
     * Das umfasst neben Expand-Zustand und Icons auch den Feststellbutton der Modulgliederung
     * und die persoenlichen Exclude-/Include-Listen, denn sie stehen in derselben Datei
     * (siehe REQ-072).
     */
    fun resetToDefault() {
        customState = TaskViewConfig()
        iconCache.clear()
        pendingWrites.incrementAndGet()
        cs.launch(Dispatchers.IO) {
            try {
                if (customFile.exists()) customFile.delete()
                loadedStamps = currentStamps()
            } finally {
                pendingWrites.decrementAndGet()
            }
        }
    }

    /**
     * Kandidaten fuer [key], vom spezifischsten zum allgemeinsten. Der erste Kandidat, der
     * ueberhaupt einen Wert fuer die gefragte Eigenschaft hat, gewinnt - ein Eintrag, der nur
     * ein Icon setzt, verdeckt also keinen Expand-Zustand einer allgemeineren Ebene.
     */
    private fun <T> resolve(
        key: TaskViewConfigKey,
        skipCustomEntry: Boolean = false,
        property: (TaskViewNodeConfig) -> T?
    ): T? = candidatesFor(key, skipCustomEntry).firstNotNullOfOrNull(property)

    /**
     * @param skipCustomEntry laesst den Eintrag aus `custom.yaml` aus, den [key] **selbst**
     *   adressiert. Damit laesst sich fragen, was ohne ihn gaelte - die Grundlage dafuer, nur
     *   echte Abweichungen zu speichern.
     */
    private fun candidatesFor(key: TaskViewConfigKey, skipCustomEntry: Boolean = false): List<TaskViewNodeConfig> {
        val mode = GradleTaskViewSettings.getInstance(project).state.selectedMode
        val ownScopeIsModule = when (key) {
            is TaskViewConfigKey.Module -> true
            is TaskViewConfigKey.Group -> key.modulePath != null
            is TaskViewConfigKey.Task -> key.modulePath != null
            is TaskViewConfigKey.VirtualTask -> key.modulePath != null
        }

        val scoped = listOfNotNull(
            entryOf(customState, key, scoped = true)?.takeUnless { skipCustomEntry && ownScopeIsModule },
            entryOf(defaultState, key, scoped = true)
        )
        val global = listOfNotNull(
            entryOf(customState, key, scoped = false)?.takeUnless { skipCustomEntry && !ownScopeIsModule },
            entryOf(defaultState, key, scoped = false)
        )

        // Modulbezug vor modulunabhaengig, darin modusbezogen vor modusuebergreifend,
        // darin custom.yaml vor default.yaml.
        return scoped.mapNotNull { it.modes[mode] } + scoped.map { it.allModes } +
            global.mapNotNull { it.modes[mode] } + global.map { it.allModes }
    }

    /**
     * Der Eintrag, den [key] in [config] adressiert - entweder der modulbezogene
     * ([scoped]` = true`) oder der modulunabhaengige. Ein Modulknoten hat keine
     * modulunabhaengige Form, ein Schluessel ohne Modulbezug keine modulbezogene.
     */
    private fun entryOf(config: TaskViewConfig, key: TaskViewConfigKey, scoped: Boolean): TaskViewNodeConfig? =
        when (key) {
            is TaskViewConfigKey.Module -> if (scoped) config.modules[key.modulePath]?.node else null

            is TaskViewConfigKey.Group -> if (scoped) {
                key.modulePath?.let { config.modules[it]?.groups?.get(key.groupPath) }
            } else {
                config.groups[key.groupPath]
            }

            is TaskViewConfigKey.Task -> if (scoped) {
                key.modulePath?.let { config.modules[it]?.tasks?.get(key.taskName) }
            } else {
                config.tasks[key.taskName]
            }

            is TaskViewConfigKey.VirtualTask -> if (scoped) {
                key.modulePath?.let { config.modules[it]?.groups?.get(key.groupPath)?.tasks?.get(key.name) }
            } else {
                config.groups[key.groupPath]?.tasks?.get(key.name)
            }
        }

    private fun reload() {
        val stamps = currentStamps()
        defaultState = TaskViewConfigYaml.readOrEmpty(defaultFile)
        customState = TaskViewConfigYaml.readOrEmpty(customFile)
        loadedStamps = stamps
        iconCache.clear()
    }

    private fun currentStamps(): Pair<Long, Long> = defaultFile.lastModified() to customFile.lastModified()

    private fun loadIcon(fileName: String): Icon? = try {
        val dir = iconDir.canonicalFile
        val requested = File(dir, fileName).canonicalFile
        // Die YAML-Dateien werden von Hand gepflegt und koennen aus einem geteilten Repository
        // stammen: Nur Dateien innerhalb des Icon-Verzeichnisses zulassen, damit ein Eintrag
        // wie "../../../etc/passwd" nicht ausserhalb greift.
        if (!requested.path.startsWith(dir.path + File.separator)) {
            LOG.warn("Icon '$fileName' liegt ausserhalb von $dir und wird ignoriert")
            null
        } else {
            val file = if (JBColor.isBright()) requested else darkVariantOf(requested).takeIf { it.isFile } ?: requested
            if (!file.isFile) {
                LOG.warn("Konfiguriertes Icon '$fileName' existiert nicht unter $dir")
                null
            } else {
                IconLoader.findIcon(file.toURI().toURL())
            }
        }
    } catch (e: Exception) {
        LOG.warn("Konnte Icon '$fileName' nicht laden", e)
        null
    }

    /** `smile.svg` -> `smile_dark.svg`; wird nur verwendet, wenn die Datei tatsaechlich existiert. */
    private fun darkVariantOf(file: File): File {
        val name = file.name
        val dot = name.lastIndexOf('.')
        val darkName = if (dot < 0) "${name}_dark" else name.substring(0, dot) + "_dark" + name.substring(dot)
        return File(file.parentFile, darkName)
    }

    private fun persistCustomState() {
        val snapshot = customState
        pendingWrites.incrementAndGet()
        cs.launch(Dispatchers.IO) {
            try {
                TaskViewConfigYaml.write(customFile, snapshot)
                loadedStamps = currentStamps()
            } catch (e: Exception) {
                LOG.warn("Konnte $customFile nicht schreiben", e)
            } finally {
                pendingWrites.decrementAndGet()
            }
        }
    }

    companion object {
        private val LOG = Logger.getInstance(GradleTasksViewConfigStore::class.java)
    }
}
