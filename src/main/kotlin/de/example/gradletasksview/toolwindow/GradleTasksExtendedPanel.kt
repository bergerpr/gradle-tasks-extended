package de.example.gradletasksview.toolwindow

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import de.example.gradletasksview.actions.GradleTaskCollapseAllAction
import de.example.gradletasksview.actions.GradleTaskExpandAllAction
import de.example.gradletasksview.actions.GradleTaskResetExpandStateAction
import de.example.gradletasksview.actions.GradleTaskViewModeActionGroup
import de.example.gradletasksview.ui.GradleTasksTree
import de.example.gradletasksview.ui.GradleTaskViewRefresher
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * Wurzel-Panel der eigenen ToolWindow-Ansicht: Toolbar mit dem Modus-Dropdown
 * (siehe Plan.md, Abschnitt "Toolbar-Dropdown") ueber dem eigenen [GradleTasksTree].
 */
class GradleTasksExtendedPanel(project: Project) : JPanel(BorderLayout()) {

    val tree = GradleTasksTree(project)

    init {
        val actionGroup = DefaultActionGroup().apply {
            add(GradleTaskViewModeActionGroup())
            addSeparator()
            add(GradleTaskExpandAllAction())
            add(GradleTaskCollapseAllAction())
            addSeparator()
            add(GradleTaskResetExpandStateAction())
        }

        val toolbar: ActionToolbar = ActionManager.getInstance()
            .createActionToolbar(ActionPlaces.TOOLWINDOW_CONTENT, actionGroup, true)
        // Muss der Baum sein, nicht das Panel: Toolbar-Aktionen lesen ihre Daten (u. a.
        // PlatformDataKeys.CONTEXT_COMPONENT, ueber das actions/GradleTasksTreeContextActions.kt
        // den Baum ermittelt) aus dem targetComponent. Mit dem Panel als targetComponent
        // liefen Expand All/Collapse All zuvor ins Leere, da e.tree() dann null lieferte.
        toolbar.targetComponent = tree

        add(toolbar.component, BorderLayout.NORTH)
        add(JBScrollPane(tree), BorderLayout.CENTER)

        project.service<GradleTaskViewRefresher>().attach(tree)
    }
}
