package de.example.gradletasksview.persistence

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import de.example.gradletasksview.model.GradleTaskViewMode
import de.example.gradletasksview.model.VisibilityRule
import de.example.gradletasksview.model.VisibilityTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.Icon

/**
 * Zweistufige, dateibasierte Konfiguration der Baumansicht - Aufklappzustand von
 * Modul-/Gruppen-/Ordnerknoten, eigene Icons, ausgeblendete Knoten und die Vorgabe fuer die
 * Modulgliederung:
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
     * Zustand des Feststellbuttons fuer die Modulgliederung. Standard ist **aus**, damit sich
     * fuer bestehende Projekte ohne Konfiguration nichts aendert.
     */
    fun showModules(): Boolean =
        customState.settings.showModules ?: defaultState.settings.showModules ?: false

    fun setShowModules(showModules: Boolean) {
        val settings = customState.settings.copy(showModules = showModules)
        customState = customState.copy(settings = settings)
        persistCustomState()
    }

    /**
     * Alle im Modus [mode] wirksamen Sichtbarkeitsregeln, flachgeklopft fuer die Modelllogik.
     * Die Reihenfolge der Liste spielt keine Rolle - welche Regel gewinnt, entscheidet der
     * Vergleich in [de.example.gradletasksview.model.filterHiddenTasks].
     */
    fun visibilityRules(mode: GradleTaskViewMode): List<VisibilityRule> =
        rulesOf(defaultState, mode, fromCustomFile = false) + rulesOf(customState, mode, fromCustomFile = true)

    private fun rulesOf(
        config: TaskViewConfig,
        mode: GradleTaskViewMode,
        fromCustomFile: Boolean
    ): List<VisibilityRule> = buildList {
        config.groups.forEach { (path, entry) ->
            addRules(entry, VisibilityTarget.Group(path), scope = null, mode, fromCustomFile)
        }
        config.tasks.forEach { (name, entry) ->
            addRules(entry, VisibilityTarget.Task(name), scope = null, mode, fromCustomFile)
        }
        config.modules.forEach { (path, block) ->
            // Der Modulknoten selbst: Sein Geltungsbereich ist das Modul, das er benennt -
            // ein zusaetzlicher Scope waere dieselbe Angabe doppelt.
            addRules(block.node, VisibilityTarget.Module(path), scope = null, mode, fromCustomFile)
            block.groups.forEach { (group, entry) ->
                addRules(entry, VisibilityTarget.Group(group), scope = path, mode, fromCustomFile)
            }
            block.tasks.forEach { (task, entry) ->
                addRules(entry, VisibilityTarget.Task(task), scope = path, mode, fromCustomFile)
            }
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

    fun setExpanded(key: TaskViewConfigKey, expanded: Boolean) {
        // Die uebrigen Felder des Eintrags bleiben erhalten - sonst wuerde das erste
        // Auf-/Zuklappen eine in custom.yaml gepflegte Icon-Zuordnung loeschen.
        customState = customState.withNode(key) { it.copy(expanded = expanded) }
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

    /**
     * Liest beide Dateien neu ein, falls sich ihr Zeitstempel geaendert hat. Dadurch wirkt eine
     * von Hand editierte `default.yaml` beim naechsten Refresh der Ansicht, ohne dass die IDE
     * neu gestartet werden muss.
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
    private fun <T> resolve(key: TaskViewConfigKey, property: (TaskViewNodeConfig) -> T?): T? =
        candidatesFor(key).firstNotNullOfOrNull(property)

    private fun candidatesFor(key: TaskViewConfigKey): List<TaskViewNodeConfig> {
        val mode = GradleTaskViewSettings.getInstance(project).state.selectedMode
        val scoped = listOfNotNull(entryOf(customState, key, scoped = true), entryOf(defaultState, key, scoped = true))
        val global = listOfNotNull(entryOf(customState, key, scoped = false), entryOf(defaultState, key, scoped = false))

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
