package de.example.gradletasksview.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.components.service
import de.example.gradletasksview.model.GradleTaskViewMode
import de.example.gradletasksview.persistence.GradleTaskViewSettings
import de.example.gradletasksview.ui.GradleTaskViewRefresher

class SetGradleTaskViewModeAction(private val mode: GradleTaskViewMode) : ToggleAction({ mode.name }) {

    override fun isSelected(e: AnActionEvent): Boolean {
        val project = e.project ?: return false
        return GradleTaskViewSettings.getInstance(project).state.selectedMode == mode
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        if (!state) return
        val project = e.project ?: return
        project.service<GradleTaskViewRefresher>().switchMode(mode)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}
