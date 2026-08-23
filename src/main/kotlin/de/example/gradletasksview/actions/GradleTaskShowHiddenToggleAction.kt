package de.example.gradletasksview.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.components.service
import de.example.gradletasksview.GradleTasksViewBundle
import de.example.gradletasksview.ui.GradleTaskViewRefresher

/**
 * Zeigt ausgeblendete Knoten ausgegraut mit an (REQ-078).
 *
 * Ohne ihn gaebe es keinen Weg zurueck: Ein ausgeblendeter Knoten ist unsichtbar, und
 * "Reset to Default" (REQ-072) wuerde Aufklappzustand, Icons und Modulgliederung mit
 * zuruecknehmen - fuer das Wiedereinblenden eines einzelnen Ordners ein grober Behelf.
 *
 * Der Zustand ist reine Ansichtssache und wird bewusst **nicht** persistiert: Er gehoert zum
 * Aufraeumen, nicht zur Konfiguration, und beim naechsten Start soll der Baum wieder so
 * aussehen, wie er eingerichtet wurde.
 */
class GradleTaskShowHiddenToggleAction : ToggleAction(
    { GradleTasksViewBundle.message("action.showHidden.text") },
    { GradleTasksViewBundle.message("action.showHidden.description") },
    AllIcons.Actions.Show
) {
    override fun isSelected(e: AnActionEvent): Boolean =
        e.project?.service<GradleTaskViewRefresher>()?.showHidden == true

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        e.project?.service<GradleTaskViewRefresher>()?.setShowHidden(state)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}
