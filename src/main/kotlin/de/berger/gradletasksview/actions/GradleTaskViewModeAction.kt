package de.berger.gradletasksview.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.KeepPopupOnPerform
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.components.service
import de.berger.gradletasksview.model.GradleTaskViewMode
import de.berger.gradletasksview.persistence.GradleTaskViewSettings
import de.berger.gradletasksview.ui.GradleTaskViewRefresher

/**
 * Ein Modus-Eintrag des Toolbar-Dropdowns. Bewusst eine [ToggleAction], damit der aktive
 * Modus angehakt dargestellt wird.
 *
 * Die drei Eintraege verhalten sich wie Radioknoepfe: Genau einer ist gewaehlt, und nach der
 * Wahl gibt es im Popup nichts mehr zu tun. Deshalb [KeepPopupOnPerform.Never] - ohne das
 * bleibt das Popup offen, weil die Plattform Toggle-Eintraege standardmaessig als
 * Mehrfachauswahl behandelt und offen laesst (sinnvoll bei unabhaengigen Haken, hier nicht).
 */
class SetGradleTaskViewModeAction(private val mode: GradleTaskViewMode) : ToggleAction({ mode.name }) {

    init {
        templatePresentation.keepPopupOnPerform = KeepPopupOnPerform.Never
    }

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
