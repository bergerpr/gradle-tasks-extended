package de.example.gradletasksview.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.ide.CopyPasteManager
import de.example.gradletasksview.GradleTasksViewBundle
import de.example.gradletasksview.persistence.GradleTasksViewConfigStore
import de.example.gradletasksview.persistence.TaskViewConfigKey
import de.example.gradletasksview.ui.GradleTaskViewRefresher
import de.example.gradletasksview.ui.GradleTasksTree
import java.awt.datatransfer.StringSelection

/**
 * Nachgebaute Kontextmenue-Aktionen fuer die eigene [GradleTasksTree]
 * (siehe REQ-011 und ADR-001). Alle Aktionen
 * lesen die Selektion direkt vom Tree, statt an eine fremde Action-Instanz zu delegieren.
 *
 * Die Ausfuehrungsarten gehoeren bewusst nicht dazu: Sie kommen als Kontext-Aktionen von der
 * Plattform selbst (siehe [GradleTaskRunActionGroup] und REQ-079).
 */
private fun AnActionEvent.tree(): GradleTasksTree? =
    getData(PlatformDataKeys.CONTEXT_COMPONENT) as? GradleTasksTree

class GradleTaskCopyNameAction : AnAction({ GradleTasksViewBundle.message("action.copyName.text") }) {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.tree()?.selectedTaskDescriptors()?.isNotEmpty() == true
    }

    override fun actionPerformed(e: AnActionEvent) {
        val text = e.tree()?.selectedTaskDescriptors()?.joinToString("\n") { it.taskName } ?: return
        if (text.isNotEmpty()) CopyPasteManager.getInstance().setContents(StringSelection(text))
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}

class GradleTaskCopyPathAction : AnAction({ GradleTasksViewBundle.message("action.copyPath.text") }) {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.tree()?.selectedTaskDescriptors()?.isNotEmpty() == true
    }

    override fun actionPerformed(e: AnActionEvent) {
        val text = e.tree()?.selectedTaskDescriptors()?.joinToString("\n") { it.taskPath } ?: return
        if (text.isNotEmpty()) CopyPasteManager.getInstance().setContents(StringSelection(text))
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}

class GradleTaskCopyFolderPathAction : AnAction({ GradleTasksViewBundle.message("action.copyFolderPath.text") }) {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.tree()?.selectedFolder() != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val text = e.tree()?.selectedFolder()?.fullPath ?: return
        CopyPasteManager.getInstance().setContents(StringSelection(text))
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}

class GradleTaskExpandAllAction : AnAction(
    { GradleTasksViewBundle.message("action.expandAll.text") },
    AllIcons.Actions.Expandall
) {
    /**
     * Bewusst eine `while`-Schleife mit jedes Mal neu gelesenem `rowCount`: Beim Aufklappen
     * einer Zeile schiebt der Baum deren Kinder als neue Zeilen dahinter, `rowCount` waechst
     * also waehrend des Durchlaufs. Ein Kotlin-Bereich `0 until tree.rowCount` wertet die
     * Obergrenze **einmal** aus und deckt damit nur die anfangs sichtbaren Zeilen ab - genau
     * das Symptom "Expand All klappt pro Klick nur eine weitere Ebene auf".
     */
    override fun actionPerformed(e: AnActionEvent) {
        val tree = e.tree() ?: return
        var row = 0
        while (row < tree.rowCount) {
            tree.expandRow(row)
            row++
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}

class GradleTaskCollapseAllAction : AnAction(
    { GradleTasksViewBundle.message("action.collapseAll.text") },
    AllIcons.Actions.Collapseall
) {
    override fun actionPerformed(e: AnActionEvent) {
        e.tree()?.let { tree -> for (row in tree.rowCount - 1 downTo 0) tree.collapseRow(row) }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}

/**
 * Blendet die selektierten Knoten aus - oder wieder ein, wenn sie es bereits sind (REQ-078).
 *
 * Ein Eintrag entsteht genau an der Stelle, an der der Knoten steht: Ist die Modulgliederung
 * an, im Block seines Moduls, sonst modulunabhaengig. Derselbe Gruppenpfad in einem anderen
 * Modul bleibt damit sichtbar.
 *
 * Die Aktion wirkt auf die **gesamte** Selektion, wie die uebrigen Eintraege des
 * Kontextmenues. Knoten ohne Konfigurationsschluessel - der `Tasks`-Knoten des `Default`-Modus
 * (REQ-077) - werden dabei uebersprungen, statt die Aktion fuer die ganze Auswahl zu sperren.
 */
class GradleTaskToggleHiddenAction : AnAction() {

    override fun update(e: AnActionEvent) {
        val keys = e.tree()?.selectedConfigKeys().orEmpty()
        e.presentation.isEnabled = keys.isNotEmpty()
        e.presentation.text = GradleTasksViewBundle.message(
            if (allHidden(e, keys)) "action.hideNode.show.text" else "action.hideNode.hide.text"
        )
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val tree = e.tree() ?: return
        val keys = tree.selectedConfigKeys()
        if (keys.isEmpty()) return

        val store = project.service<GradleTasksViewConfigStore>()
        val hide = !allHidden(e, keys)
        keys.forEach { store.setHidden(it, hide) }
        project.service<GradleTaskViewRefresher>().requestRefresh()
    }

    /**
     * Nur wenn **alle** selektierten Knoten ausgeblendet sind, bietet die Aktion das
     * Wiedereinblenden an. Bei gemischter Auswahl ist "ausblenden" die naheliegendere Absicht,
     * und sie laesst sich anschliessend geschlossen wieder zuruecknehmen.
     */
    private fun allHidden(e: AnActionEvent, keys: List<TaskViewConfigKey>): Boolean {
        if (keys.isEmpty()) return false
        val store = e.project?.service<GradleTasksViewConfigStore>() ?: return false
        return keys.all { store.isHidden(it) }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}
