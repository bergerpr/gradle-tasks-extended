package de.berger.gradletasksview.toolwindow

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import de.berger.gradletasksview.actions.GradleTaskCollapseAllAction
import de.berger.gradletasksview.actions.GradleTaskConfigMenuActionGroup
import de.berger.gradletasksview.actions.GradleTaskExpandAllAction
import de.berger.gradletasksview.actions.GradleTaskResetExpandStateAction
import de.berger.gradletasksview.actions.GradleTaskShowConfigHelpAction
import de.berger.gradletasksview.actions.GradleTaskShowHiddenToggleAction
import de.berger.gradletasksview.actions.GradleTaskShowInheritedTasksToggleAction
import de.berger.gradletasksview.actions.GradleTaskShowModulesToggleAction
import de.berger.gradletasksview.actions.GradleTaskViewModeActionGroup
import de.berger.gradletasksview.ui.GradleTasksTree
import de.berger.gradletasksview.ui.GradleTaskViewRefresher
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * Wurzel-Panel der eigenen ToolWindow-Ansicht: Toolbar mit dem Modus-Dropdown
 * (siehe REQ-001), dem Feststellbutton fuer die Modulgliederung (REQ-074) und den beiden
 * Konfigurationsknoepfen ganz rechts (REQ-080, REQ-084) ueber dem eigenen [GradleTasksTree].
 */
class GradleTasksExtendedPanel(project: Project) : JPanel(BorderLayout()) {

    val tree = GradleTasksTree(project)

    init {
        val actionGroup = DefaultActionGroup().apply {
            add(GradleTaskViewModeActionGroup())
            // Direkt neben dem Modus-Dropdown, weil beide zusammen bestimmen, wie der Baum
            // gegliedert ist - aber als eigener Feststellbutton, da orthogonal (siehe REQ-074).
            add(GradleTaskShowModulesToggleAction())
            // Direkt daneben, weil auch dieser Knopf bestimmt, welche Zeilen ueberhaupt im
            // Baum stehen - hier die geerbten Sammelzeilen der Unterprojekte (REQ-088).
            add(GradleTaskShowInheritedTasksToggleAction())
            add(GradleTaskShowHiddenToggleAction())
            addSeparator()
            add(GradleTaskExpandAllAction())
            add(GradleTaskCollapseAllAction())
            addSeparator()
            add(GradleTaskResetExpandStateAction())
            addSeparator()
            // Zahnrad und Info ganz rechts, das Info-Symbol rechts neben dem Zahnrad: Beide
            // fuehren nicht in den Baum, sondern zur Konfiguration dahinter (REQ-080). Das
            // Zahnrad ist ein Klappmenue aus "Configuration" und "Reload" (REQ-084).
            add(GradleTaskConfigMenuActionGroup())
            add(GradleTaskShowConfigHelpAction())
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
