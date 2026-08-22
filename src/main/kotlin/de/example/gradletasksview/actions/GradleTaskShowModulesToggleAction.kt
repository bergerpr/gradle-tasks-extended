package de.example.gradletasksview.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.components.service
import de.example.gradletasksview.GradleTasksViewBundle
import de.example.gradletasksview.persistence.GradleTasksViewConfigStore
import de.example.gradletasksview.ui.GradleTaskViewRefresher

/**
 * Feststellbutton fuer die Modulgliederung (siehe REQ-074).
 *
 * Bewusst **kein** vierter Eintrag im Modus-Dropdown: Die Modulebene ist orthogonal zu
 * Default/Sort/Nested und laesst sich mit jedem der drei kombinieren, woraus sechs
 * Kombinationen entstehen. Ein vierter Modus koennte das nicht ausdruecken.
 *
 * Der Zustand liegt in derselben zweistufigen Konfiguration wie alles andere: Vorgabe aus
 * `default.yaml`, persoenliche Abweichung in `custom.yaml`, Standard aus.
 */
class GradleTaskShowModulesToggleAction : ToggleAction(
    { GradleTasksViewBundle.message("action.showModules.text") },
    { GradleTasksViewBundle.message("action.showModules.description") },
    AllIcons.Nodes.Module
) {
    override fun isSelected(e: AnActionEvent): Boolean {
        val project = e.project ?: return false
        return project.service<GradleTasksViewConfigStore>().showModules()
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val project = e.project ?: return
        project.service<GradleTaskViewRefresher>().setShowModules(state)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}
