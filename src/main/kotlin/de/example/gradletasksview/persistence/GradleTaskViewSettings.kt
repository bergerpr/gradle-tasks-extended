package de.example.gradletasksview.persistence

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.example.gradletasksview.model.GradleTaskViewMode

/**
 * Projektbezogene Persistenz der Modusauswahl (siehe Plan.md, Abschnitt "Persistenz").
 * `Service.Level.PROJECT`, da Gradle-Gruppenstrukturen projektspezifisch sind - niemals
 * als Application-Level-Singleton implementieren.
 *
 * Der Expand/Collapse-Zustand selbst wird nicht mehr hier, sondern dateibasiert ueber
 * [GradleTasksViewConfigStore] verwaltet (`default.yaml`/`custom.yaml`).
 */
@State(
    name = "GradleTasksViewSettings",
    storages = [Storage("gradleTasksView.xml")]
)
@Service(Service.Level.PROJECT)
class GradleTaskViewSettings : PersistentStateComponent<GradleTaskViewSettingsState> {

    private var myState = GradleTaskViewSettingsState()

    override fun getState(): GradleTaskViewSettingsState = myState

    override fun loadState(state: GradleTaskViewSettingsState) {
        myState = state
    }

    fun switchToMode(mode: GradleTaskViewMode) {
        myState.selectedMode = mode
    }

    companion object {
        fun getInstance(project: Project): GradleTaskViewSettings = project.service()
    }
}
