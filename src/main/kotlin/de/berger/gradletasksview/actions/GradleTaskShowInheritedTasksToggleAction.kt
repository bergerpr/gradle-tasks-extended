package de.berger.gradletasksview.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.components.service
import de.berger.gradletasksview.GradleTasksViewBundle
import de.berger.gradletasksview.persistence.GradleTasksViewConfigStore
import de.berger.gradletasksview.ui.GradleTaskViewRefresher

/**
 * Feststellbutton fuer die geerbten Sammelzeilen der Unterprojekte (REQ-088).
 *
 * Das Gegenstueck zu `Show Inherited Tasks` in der originalen Gradle-Ansicht, samt deren
 * Vorgabe: **eingeschaltet**. Ein Projekt, das seine Plugins zentral fuer alle Unterprojekte
 * setzt, haette auf Wurzelebene sonst gar keine Tasks - dort ist jede Zeile eine geerbte.
 *
 * Anders als "Show Hidden" (REQ-078) wird der Zustand persistiert: Er beschreibt, wie der Baum
 * dauerhaft aussehen soll, und liegt deshalb in derselben zweistufigen Konfiguration wie die
 * Modulgliederung - Vorgabe aus `default.yaml`, persoenliche Abweichung in `custom.yaml`.
 */
class GradleTaskShowInheritedTasksToggleAction : ToggleAction(
    { GradleTasksViewBundle.message("action.showInheritedTasks.text") },
    { GradleTasksViewBundle.message("action.showInheritedTasks.description") },
    AllIcons.Hierarchy.Supertypes
) {
    override fun isSelected(e: AnActionEvent): Boolean =
        e.project?.service<GradleTasksViewConfigStore>()?.showInheritedTasks() == true

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        e.project?.service<GradleTaskViewRefresher>()?.setShowInheritedTasks(state)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}
