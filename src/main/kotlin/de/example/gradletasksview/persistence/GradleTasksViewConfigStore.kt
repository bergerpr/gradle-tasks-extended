package de.example.gradletasksview.persistence

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import de.example.gradletasksview.model.GradleTaskViewMode
import de.example.gradletasksview.model.ModuleFilterRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.Icon

/**
 * Zweistufige, dateibasierte Konfiguration der Baumansicht - Expand/Collapse-Zustand von
 * Modul-/Gruppen-/Ordnerknoten, eigene Icons sowie die Ansichtseinstellungen (Modulgliederung
 * und Exclude/Include):
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
 * Aufloesung pro Knoten, von spezifisch nach allgemein: modulbezogener Eintrag aus
 * `custom.yaml` > modulbezogener Eintrag aus `default.yaml` > modulunabhaengiger Eintrag aus
 * `custom.yaml` > modulunabhaengiger Eintrag aus `default.yaml` > eingebauter Standard
 * (aufgeklappt bzw. Plugin-Icon). Der spezifischere Schluessel gewinnt also vor der
 * persoenlichen Datei - sonst koennte eine einmal global gesetzte Vorliebe eine bewusst pro
 * Modul gepflegte Team-Vorgabe nie zur Geltung kommen lassen.
 *
 * Die Exclude-/Include-**Listen** folgen einer anderen Regel: Sie ergaenzen einander, statt
 * sich zu ersetzen (siehe [TaskViewFilter.plus] und REQ-075).
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
     * Alle im Modus [mode] wirksamen Ausschluss-/Wiedereinschlussregeln, flachgeklopft fuer die
     * Modelllogik. Die Listen aus `custom.yaml` ergaenzen die aus `default.yaml`.
     */
    fun moduleFilterRules(mode: GradleTaskViewMode): List<ModuleFilterRule> {
        val global = defaultState.settings.filter + customState.settings.filter
        val forMode = defaultState.settings.modeFilters[mode].orEmpty() +
            customState.settings.modeFilters[mode].orEmpty()
        return global.toRules(modeSpecific = false) + forMode.toRules(modeSpecific = true)
    }

    private fun TaskViewFilter?.orEmpty(): TaskViewFilter = this ?: TaskViewFilter()

    private fun TaskViewFilter.toRules(modeSpecific: Boolean): List<ModuleFilterRule> =
        exclude.map { ModuleFilterRule(it, include = false, modeSpecific = modeSpecific) } +
            include.map { (module, groupPaths) ->
                ModuleFilterRule(module, groupPaths = groupPaths, include = true, modeSpecific = modeSpecific)
            }

    fun setExpanded(key: TaskViewConfigKey, expanded: Boolean) {
        // Ein eventuell vorhandenes Icon des Eintrags muss erhalten bleiben - sonst wuerde
        // das erste Auf-/Zuklappen eine in custom.yaml gepflegte Icon-Zuordnung loeschen.
        customState = when {
            key.nodeKey == null && key.modulePath != null -> {
                val module = customState.modules[key.modulePath] ?: TaskViewModuleConfig()
                customState.copy(
                    modules = customState.modules +
                        (key.modulePath to module.copy(node = module.node.copy(expanded = expanded)))
                )
            }

            key.nodeKey != null && key.modulePath != null -> {
                val module = customState.modules[key.modulePath] ?: TaskViewModuleConfig()
                val previous = module.groups[key.nodeKey] ?: TaskViewNodeConfig()
                customState.copy(
                    modules = customState.modules + (key.modulePath to module.copy(
                        groups = module.groups + (key.nodeKey to previous.copy(expanded = expanded))
                    ))
                )
            }

            key.nodeKey != null -> {
                val previous = customState.entries[key.nodeKey] ?: TaskViewNodeConfig()
                customState.copy(entries = customState.entries + (key.nodeKey to previous.copy(expanded = expanded)))
            }

            else -> return
        }
        persistCustomState()
    }

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
        val nodeKey = key.nodeKey ?: return listOfNotNull(
            key.modulePath?.let { customState.modules[it]?.node },
            key.modulePath?.let { defaultState.modules[it]?.node }
        )

        return listOfNotNull(
            key.modulePath?.let { customState.modules[it]?.groups?.get(nodeKey) },
            key.modulePath?.let { defaultState.modules[it]?.groups?.get(nodeKey) },
            customState.entries[nodeKey],
            defaultState.entries[nodeKey]
        )
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
