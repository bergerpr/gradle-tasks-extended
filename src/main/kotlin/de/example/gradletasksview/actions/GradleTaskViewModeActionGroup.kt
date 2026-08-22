package de.example.gradletasksview.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.DefaultActionGroup
import de.example.gradletasksview.GradleTasksViewBundle
import de.example.gradletasksview.model.GradleTaskViewMode

class GradleTaskViewModeActionGroup : DefaultActionGroup(
    { GradleTasksViewBundle.message("action.viewMode.group.text") },
    { "" },
    AllIcons.Actions.GroupBy
) {
    init {
        templatePresentation.isPopupGroup = true
        add(SetGradleTaskViewModeAction(GradleTaskViewMode.DEFAULT))
        add(SetGradleTaskViewModeAction(GradleTaskViewMode.SORT))
        add(SetGradleTaskViewModeAction(GradleTaskViewMode.NESTED))
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}
