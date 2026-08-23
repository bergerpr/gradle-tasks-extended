package de.berger.gradletasksview.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.berger.gradletasksview.GradleTasksViewBundle
import de.berger.gradletasksview.model.GradleTaskViewMode
import de.berger.gradletasksview.persistence.GradleTaskViewSettings
import de.berger.gradletasksview.persistence.GradleTasksViewConfigStore
import de.berger.gradletasksview.ui.GradleTaskViewRefresher

/**
 * Feststellbutton fuer die Modulgliederung (siehe REQ-074).
 *
 * Bewusst **kein** vierter Eintrag im Modus-Dropdown: Die Modulebene ist orthogonal zu `Sort`
 * und `Nested` und laesst sich mit beiden kombinieren. Ein vierter Modus koennte das nicht
 * ausdruecken.
 *
 * In `Default` ist die Modulebene dagegen fester Bestandteil der Ansicht (REQ-077); der Button
 * wird dort ausgegraut und angehakt dargestellt, statt wirkungslos anklickbar zu bleiben. Der
 * **gespeicherte** Zustand bleibt davon unberuehrt: Wer in `Nested` die Module eingeschaltet
 * hat, findet sie dort nach einem Ausflug nach `Default` unveraendert vor - deshalb wird hier
 * nur die Anzeige uebersteuert und nie [GradleTaskViewRefresher.setShowModules] aufgerufen.
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
        if (isFixedOn(project)) return true
        return project.service<GradleTasksViewConfigStore>().showModules()
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val project = e.project ?: return
        project.service<GradleTaskViewRefresher>().setShowModules(state)
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val fixed = e.project?.let { isFixedOn(it) } == true
        e.presentation.isEnabled = !fixed
        e.presentation.description = GradleTasksViewBundle.message(
            if (fixed) "action.showModules.description.fixed" else "action.showModules.description"
        )
    }

    private fun isFixedOn(project: Project): Boolean =
        GradleTaskViewSettings.getInstance(project).state.selectedMode == GradleTaskViewMode.DEFAULT

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}
