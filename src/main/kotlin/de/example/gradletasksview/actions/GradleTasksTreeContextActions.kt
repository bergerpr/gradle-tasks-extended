package de.example.gradletasksview.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import de.example.gradletasksview.GradleTasksViewBundle
import de.example.gradletasksview.ui.GradleTasksTree
import java.awt.datatransfer.StringSelection

/**
 * Nachgebaute Kontextmenue-Aktionen fuer die eigene [GradleTasksTree]
 * (siehe Plan.md, Abschnitt "Erhalt der bestehenden Funktionen"). Alle Aktionen
 * lesen die Selektion direkt vom Tree, statt an eine fremde Action-Instanz zu delegieren.
 */
private fun AnActionEvent.tree(): GradleTasksTree? =
    getData(PlatformDataKeys.CONTEXT_COMPONENT) as? GradleTasksTree

class GradleTaskExecuteAction : AnAction(
    { GradleTasksViewBundle.message("action.execute.text") },
    { GradleTasksViewBundle.message("action.execute.description") }
) {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.tree()?.selectedTaskDescriptors()?.isNotEmpty() == true
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.tree()?.runSelectedTask()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}

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
    override fun actionPerformed(e: AnActionEvent) {
        e.tree()?.let { tree -> for (row in 0 until tree.rowCount) tree.expandRow(row) }
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
