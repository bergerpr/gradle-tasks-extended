package de.berger.gradletasksview.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import de.berger.gradletasksview.GradleTasksViewBundle
import de.berger.gradletasksview.ui.GradleTaskViewRefresher

/**
 * Setzt alle persoenlichen Abweichungen zurueck, indem `custom.yaml` geloescht wird - danach
 * gilt wieder ausschliesslich `default.yaml` (bzw. "aufgeklappt", falls dort nicht
 * aufgefuehrt).
 *
 * Das umfasst seit der Modulgliederung mehr als nur Expand/Collapse: Auch der Feststellbutton
 * und persoenliche Exclude-/Include-Listen stehen in derselben Datei und fallen damit auf die
 * Team-Vorgabe zurueck. Das ist konsistent - alles, was der Benutzer selbst gesetzt hat, liegt
 * an einem Ort - soll aber bewusst so heissen und nicht ueberraschen (siehe REQ-072).
 */
class GradleTaskResetExpandStateAction : AnAction(
    { GradleTasksViewBundle.message("action.resetExpandState.text") },
    { GradleTasksViewBundle.message("action.resetExpandState.description") },
    AllIcons.General.Reset
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        project.service<GradleTaskViewRefresher>().resetExpandStateToDefault()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}
