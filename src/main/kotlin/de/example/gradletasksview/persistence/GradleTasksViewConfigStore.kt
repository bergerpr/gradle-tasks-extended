package de.example.gradletasksview.persistence

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.swing.Icon

/**
 * Zweistufige, dateibasierte Konfiguration der Baumansicht - Expand/Collapse-Zustand von
 * Gruppen-/Ordnerknoten sowie eigene Icons. Gilt fuer alle drei Ansichtsmodi
 * (Default/Sort/Nested) gleichermassen:
 *
 * - `<Projektwurzel>/gradle/gradle_tasks_extended/default.yaml`: Team-weite Vorgabe,
 *   die versioniert werden kann. Wird nur angelegt, wenn sie noch nicht existiert -
 *   eine bereits vorhandene Datei wird niemals durch das Plugin ueberschrieben.
 * - `<Projektwurzel>/gradle/gradle_tasks_extended/custom.yaml`: Persoenliche
 *   Abweichungen, die entstehen, sobald der Benutzer selbst eine Gruppe/einen Ordner
 *   auf-/zuklappt. Wird ausschliesslich vom Plugin geschrieben.
 * - `<Projektwurzel>/gradle/gradle_tasks_extended/icons/`: Ablageort fuer eigene
 *   Icon-Dateien, auf die die YAML-Dateien per Dateinamen verweisen.
 *
 * Aufloesung pro Schluessel: `custom.yaml` > `default.yaml` > eingebauter Standard
 * (aufgeklappt bzw. Plugin-Icon).
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

    fun isExpanded(key: String): Boolean =
        customState.entries[key]?.expanded ?: defaultState.entries[key]?.expanded ?: true

    /**
     * Eigenes Icon fuer [key] oder `null`, wenn keines konfiguriert ist bzw. die Datei fehlt -
     * dann bleibt es beim eingebauten Icon des Plugins.
     */
    fun iconFor(key: String): Icon? {
        val fileName = customState.entries[key]?.icon ?: defaultState.entries[key]?.icon ?: return null
        val cacheKey = "$fileName|bright=${JBColor.isBright()}"
        return iconCache.getOrPut(cacheKey) { Optional(loadIcon(fileName)) }.icon
    }

    fun setExpanded(key: String, expanded: Boolean) {
        // Ein eventuell vorhandenes Icon des Eintrags muss erhalten bleiben - sonst wuerde
        // das erste Auf-/Zuklappen eine in custom.yaml gepflegte Icon-Zuordnung loeschen.
        val previous = customState.entries[key] ?: TaskViewNodeConfig()
        customState = TaskViewConfig(customState.entries + (key to previous.copy(expanded = expanded)))
        persistCustomState()
    }

    /**
     * Liest beide Dateien neu ein, falls sich ihr Zeitstempel geaendert hat. Dadurch wirkt eine
     * von Hand editierte `default.yaml` beim naechsten Refresh der Ansicht, ohne dass die IDE
     * neu gestartet werden muss.
     */
    fun reloadIfChanged() {
        if (currentStamps() == loadedStamps) return
        try {
            reload()
        } catch (e: Exception) {
            LOG.warn("Konnte Konfiguration unter $configDir nicht neu laden", e)
        }
    }

    /** Loescht alle persoenlichen Abweichungen - danach gilt wieder ausschliesslich [defaultFile]. */
    fun resetToDefault() {
        customState = TaskViewConfig()
        iconCache.clear()
        cs.launch(Dispatchers.IO) {
            if (customFile.exists()) customFile.delete()
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
        cs.launch(Dispatchers.IO) {
            try {
                TaskViewConfigYaml.write(customFile, snapshot)
                loadedStamps = currentStamps()
            } catch (e: Exception) {
                LOG.warn("Konnte $customFile nicht schreiben", e)
            }
        }
    }

    companion object {
        private val LOG = Logger.getInstance(GradleTasksViewConfigStore::class.java)
    }
}
