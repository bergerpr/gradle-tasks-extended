package de.example.gradletasksview.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import de.example.gradletasksview.GradleTasksViewBundle
import de.example.gradletasksview.ui.GradleTaskViewRefresher

/**
 * Setzt alle persoenlichen Expand/Collapse-Abweichungen (`custom.yaml`) zurueck, sodass
 * wieder ausschliesslich `default.yaml` (bzw. "aufgeklappt", falls dort nicht aufgefuehrt)
 * gilt. Entspricht demselben Reset, der auch beim Wechsel in den `Default`-Modus erfolgt,
 * hier aber unabhaengig vom aktuellen Modus per Knopfdruck.
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
