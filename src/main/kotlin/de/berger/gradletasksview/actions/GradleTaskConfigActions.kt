package de.berger.gradletasksview.actions

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.RevealFileAction
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import de.berger.gradletasksview.GradleTasksViewBundle
import de.berger.gradletasksview.persistence.GradleTasksViewConfigStore
import de.berger.gradletasksview.ui.GradleTaskViewRefresher
import de.berger.gradletasksview.ui.GradleTasksConfigHelpDialog
import java.io.File

/**
 * Das Zahnrad in der Toolbar ist ein Klappmenue statt eines einzelnen Knopfes (REQ-084):
 * "Configuration" oeffnet wie bisher die `default.yaml`, "Reload" baut die Ansicht mit frisch
 * eingelesener Konfiguration neu auf.
 *
 * Der Reload-Eintrag ist der eigentliche Grund fuer das Menue: Eine von Hand geaenderte
 * Konfiguration - vor allem eine neue Icon-Datei - wurde bisher erst wirksam, nachdem das
 * Projekt entladen und neu geladen wurde. Beide Eintraege gehoeren zur selben Sache, deshalb
 * teilen sie sich einen Knopf, statt die Toolbar um ein weiteres Symbol zu verbreitern.
 */
class GradleTaskConfigMenuActionGroup : DefaultActionGroup(
    { GradleTasksViewBundle.message("action.configMenu.text") },
    { GradleTasksViewBundle.message("action.configMenu.description") },
    AllIcons.General.Settings,
) {
    init {
        templatePresentation.isPopupGroup = true
        add(GradleTaskOpenDefaultConfigAction())
        add(GradleTaskReloadViewAction())
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}

/**
 * Erster Eintrag des Zahnrad-Menues: oeffnet `default.yaml` im Editor (REQ-080).
 *
 * Bewusst die Team-Vorgabe und nicht `custom.yaml`: Letztere schreibt das Plugin selbst und
 * enthaelt nur Abweichungen, die sich beim naechsten Auf- oder Zuklappen ohnehin aendern -
 * von Hand gepflegt wird die Vorgabe. Fehlt die Datei, wird sie mit dem kommentierten
 * Startinhalt angelegt, statt die Aktion ins Leere laufen zu lassen.
 */
class GradleTaskOpenDefaultConfigAction : AnAction(
    { GradleTasksViewBundle.message("action.openDefaultConfig.text") },
    { GradleTasksViewBundle.message("action.openDefaultConfig.description") },
    AllIcons.General.Settings,
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = project.service<GradleTasksViewConfigStore>().ensureDefaultConfigExists()
        openInEditor(project, file)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}

/**
 * Zweiter Eintrag des Zahnrad-Menues: liest `default.yaml` und `custom.yaml` neu ein, wirft die
 * zwischengespeicherten Icons weg und baut die Ansicht neu auf (REQ-084).
 *
 * Bewusst ein erzwungenes Neueinlesen und nicht der Zeitstempelvergleich aus
 * `reloadIfChanged()`: Eine neue Datei im `icons/`-Verzeichnis aendert keinen der beiden
 * YAML-Zeitstempel, das Icon bliebe also das alte - genau der Fall, fuer den dieser Eintrag da
 * ist.
 */
class GradleTaskReloadViewAction : AnAction(
    { GradleTasksViewBundle.message("action.reloadView.text") },
    { GradleTasksViewBundle.message("action.reloadView.description") },
    AllIcons.Actions.Refresh,
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        project.service<GradleTaskViewRefresher>().reloadConfigurationAndRefresh()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}

/**
 * Info-Knopf rechts daneben: zeigt, wo die Konfiguration liegt und wie sie aufgebaut ist
 * (REQ-080). Die Pfade stammen aus demselben Store, aus dem auch gelesen wird, und sind im
 * Dialog anklickbar.
 */
class GradleTaskShowConfigHelpAction : AnAction(
    { GradleTasksViewBundle.message("action.configHelp.text") },
    { GradleTasksViewBundle.message("action.configHelp.description") },
    AllIcons.General.Information,
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        GradleTasksConfigHelpDialog(project).show()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}

/**
 * Oeffnet eine Datei im Editor bzw. ein Verzeichnis im Dateimanager. Die Aufloesung laeuft
 * ueber `refreshAndFindFileByNioPath`, weil die Datei gerade erst angelegt worden sein kann
 * und das virtuelle Dateisystem sie sonst noch nicht kennt.
 */
internal fun openInEditor(project: Project, file: File) {
    if (file.isDirectory) {
        RevealFileAction.openDirectory(file)
        return
    }
    val virtualFile = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(file.toPath()) ?: return
    FileEditorManager.getInstance(project).openFile(virtualFile, true)
}
