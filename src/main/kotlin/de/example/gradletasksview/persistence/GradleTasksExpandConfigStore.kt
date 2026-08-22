package de.example.gradletasksview.persistence

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Zweistufige, dateibasierte Konfiguration des Expand/Collapse-Zustands von Gruppen-
 * bzw. Ordnerknoten - gilt fuer alle drei Ansichtsmodi (Default/Sort/Nested) gleichermassen:
 *
 * - `<Projektwurzel>/gradle/gradle_tasks_extended/default.yaml`: Team-weite Vorgabe,
 *   die versioniert werden kann. Wird nur angelegt, wenn sie noch nicht existiert -
 *   eine bereits vorhandene Datei wird niemals durch das Plugin ueberschrieben.
 * - `<Projektwurzel>/gradle/gradle_tasks_extended/custom.yaml`: Persoenliche
 *   Abweichungen, die entstehen, sobald der Benutzer selbst eine Gruppe/einen Ordner
 *   auf-/zuklappt. Wird ausschliesslich vom Plugin geschrieben.
 *
 * Aufloesung pro Gruppen-/Ordnerpfad: `custom.yaml` > `default.yaml` > aufgeklappt
 * (Standard, falls der Pfad in keiner der beiden Dateien vorkommt).
 *
 * `Service.Level.PROJECT`, da die Konfigurationsdateien projektbezogen sind (siehe
 * Plan.md, Abschnitt "Edge Cases" zu mehreren offenen Projekten).
 */
@Service(Service.Level.PROJECT)
class GradleTasksExpandConfigStore(private val project: Project, private val cs: CoroutineScope) {

    private val configDir: File
        get() = File(project.basePath ?: ".", "gradle/gradle_tasks_extended")

    private val defaultFile: File get() = File(configDir, "default.yaml")
    private val customFile: File get() = File(configDir, "custom.yaml")

    @Volatile private var defaultState: GroupExpandState = GroupExpandState()
    @Volatile private var customState: GroupExpandState = GroupExpandState()

    init {
        // Kleine, lokale Dateien - synchrones Lesen im Service-Init ist unproblematisch
        // (Light Services werden bereits abseits des EDT konstruiert).
        try {
            GroupExpandYaml.createStarterIfMissing(defaultFile)
            defaultState = GroupExpandYaml.readOrEmpty(defaultFile)
            customState = GroupExpandYaml.readOrEmpty(customFile)
        } catch (e: Exception) {
            LOG.warn("Konnte Expand-Konfiguration unter $configDir nicht laden", e)
        }
    }

    fun isExpanded(folderPath: String): Boolean =
        customState.groups[folderPath] ?: defaultState.groups[folderPath] ?: true

    fun setExpanded(folderPath: String, expanded: Boolean) {
        customState = GroupExpandState(customState.groups + (folderPath to expanded))
        persistCustomState()
    }

    /** Loescht alle persoenlichen Abweichungen - danach gilt wieder ausschliesslich [defaultFile]. */
    fun resetToDefault() {
        customState = GroupExpandState()
        cs.launch(Dispatchers.IO) {
            if (customFile.exists()) customFile.delete()
        }
    }

    private fun persistCustomState() {
        val snapshot = customState
        cs.launch(Dispatchers.IO) {
            try {
                GroupExpandYaml.write(customFile, snapshot)
            } catch (e: Exception) {
                LOG.warn("Konnte $customFile nicht schreiben", e)
            }
        }
    }

    companion object {
        private val LOG = Logger.getInstance(GradleTasksExpandConfigStore::class.java)
    }
}
