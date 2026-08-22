package de.example.gradletasksview.ui

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener
import com.intellij.openapi.project.Project
import de.example.gradletasksview.model.GradleTaskDataProvider
import de.example.gradletasksview.model.GradleTaskViewMode
import de.example.gradletasksview.persistence.GradleTaskViewSettings
import de.example.gradletasksview.persistence.GradleTasksViewConfigStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Orchestriert das Laden, Filtern und Neuaufbauen der Gradle-Task-Ansicht.
 * `Service.Level.PROJECT`: jedes Projektfenster hat eine eigene Instanz und einen
 * eigenen [tree]/State - niemals als Application-Level-Singleton mit
 * projektuebergreifendem State implementieren (siehe REQ-042).
 *
 * Der Konstruktorparameter [cs] wird von der Plattform automatisch mit einem
 * projekt-gebundenen `CoroutineScope` injiziert (Standardmechanismus fuer
 * `@Service`-Klassen seit IntelliJ Platform 2023.3+).
 */
@Service(Service.Level.PROJECT)
class GradleTaskViewRefresher(private val project: Project, private val cs: CoroutineScope) {

    private val dataProvider = GradleTaskDataProvider(project)
    private var tree: GradleTasksTree? = null
    private var filterText: String = ""
    private var refreshJob: Job? = null

    init {
        project.messageBus.connect(project).subscribe(
            ProjectDataImportListener.TOPIC,
            object : ProjectDataImportListener {
                override fun onImportFinished(projectPath: String?) {
                    scheduleRefresh(debounce = true)
                }
            }
        )
    }

    fun attach(tree: GradleTasksTree) {
        this.tree = tree
        tree.onFilterTextChanged = { text -> onFilterTextChanged(text) }
        // Absicherung fuer ein beim Projektstart bereits geoeffnetes (wiederhergestelltes)
        // ToolWindow: der erste Refresh koennte auftreten, bevor der Baum ueberhaupt
        // sichtbar ist.
        tree.onFirstShown = { scheduleInitialRefreshWithRetry() }
        scheduleInitialRefreshWithRetry()
    }

    fun switchMode(mode: GradleTaskViewMode) {
        // Der Wechsel zu Default setzt bewusst KEINE Expand-Abweichungen mehr zurueck:
        // die dateibasierte Expand/Collapse-Konfiguration gilt inzwischen fuer alle
        // Modi (Default/Sort/Nested) gleichermassen, ein automatischer Reset beim
        // Moduswechsel wuerde das direkt wieder zunichtemachen. Zuruecksetzen erfolgt
        // ausschliesslich ueber den expliziten "Reset to Default"-Button/[resetExpandStateToDefault].
        GradleTaskViewSettings.getInstance(project).switchToMode(mode)
        scheduleRefresh(debounce = false)
    }

    /** Setzt alle persoenlichen Expand/Collapse-Abweichungen zurueck auf `default.yaml`. */
    fun resetExpandStateToDefault() {
        project.service<GradleTasksViewConfigStore>().resetToDefault()
        scheduleRefresh(debounce = false)
    }

    private fun onFilterTextChanged(text: String) {
        filterText = text
        scheduleRefresh(debounce = true)
    }

    /**
     * Debounced Rebuild (siehe REQ-031):
     * ein neu gestarteter Refresh bricht einen vorherigen, noch wartenden Refresh ab,
     * statt bei jedem einzelnen Event einen vollstaendigen Rebuild auszuloesen.
     */
    private fun scheduleRefresh(debounce: Boolean) {
        refreshJob?.cancel()
        refreshJob = cs.launch {
            if (debounce) delay(DEBOUNCE_MILLIS)
            doRefresh()
        }
    }

    /**
     * Fuer den allerersten Refresh nach dem Anzeigen des ToolWindows: das bereits
     * importierte Gradle-Projekt ist ueber [com.intellij.openapi.externalSystem.service.project.manage.ProjectDataManager]
     * nicht zuverlaessig sofort verfuegbar, wenn das ToolWindow sehr frueh beim
     * Projektstart automatisch wiederhergestellt wird (kein neuer Sync -> kein
     * `ProjectDataImportListener.onImportFinished`-Event, auf das man warten koennte).
     * Liefert [GradleTaskDataProvider.loadTasks] daher zunaechst eine leere Liste,
     * wird mit steigendem Abstand automatisch erneut versucht, statt dauerhaft leer
     * zu bleiben, bis der Benutzer selbst z. B. den Modus wechselt.
     */
    private fun scheduleInitialRefreshWithRetry() {
        refreshJob?.cancel()
        refreshJob = cs.launch {
            for (delayMillis in INITIAL_RETRY_DELAYS_MILLIS) {
                val foundTasks = doRefresh()
                if (foundTasks) return@launch
                delay(delayMillis)
            }
            doRefresh()
        }
    }

    /** @return `true`, wenn nach dem Rebuild mindestens ein Task angezeigt wird. */
    private suspend fun doRefresh(): Boolean {
        val currentTree = tree ?: return false
        val settings = GradleTaskViewSettings.getInstance(project)
        val mode = settings.state.selectedMode

        // Erlaubt es, default.yaml von Hand zu editieren (z. B. um ein Icon zuzuordnen) und
        // das Ergebnis ohne IDE-Neustart zu sehen. Liest nur dann wirklich neu ein, wenn sich
        // der Zeitstempel einer der beiden Dateien geaendert hat.
        project.service<GradleTasksViewConfigStore>().reloadIfChanged()

        val allTasks = readAction { dataProvider.loadTasks() }
        val filtered = if (filterText.isBlank()) {
            allTasks
        } else {
            allTasks.filter { it.taskName.contains(filterText, ignoreCase = true) }
        }

        val model = GradleTasksTreeModelBuilder.build(mode, filtered)

        withContext(Dispatchers.EDT) {
            val previousSelection = currentTree.stateManager.captureSelectedStableId()
            currentTree.model = model
            currentTree.stateManager.applyPersistedExpansionState()
            currentTree.stateManager.restoreSelection(previousSelection)
            // Defensiv: falls das ToolWindow beim Refresh noch nicht sichtbar/realisiert war
            // (z. B. beim Wiederherstellen eines beim Projektstart bereits geoeffneten
            // ToolWindows), erzwingt dies einen vollstaendigen Re-Layout/Repaint, sobald
            // die Komponente tatsaechlich angezeigt wird.
            currentTree.revalidate()
            currentTree.repaint()
        }

        return allTasks.isNotEmpty()
    }

    private companion object {
        const val DEBOUNCE_MILLIS = 300L

        /** Wartezeiten zwischen den automatischen Wiederholungsversuchen beim ersten Laden. */
        val INITIAL_RETRY_DELAYS_MILLIS = listOf(500L, 1000L, 2000L, 4000L)
    }
}
