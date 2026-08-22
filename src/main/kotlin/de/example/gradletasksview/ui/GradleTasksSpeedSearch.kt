package de.example.gradletasksview.ui

import com.intellij.ui.TreeSpeedSearch
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

/**
 * Filterung ueber `TreeSpeedSearch`, matched gegen `taskName` bzw. den
 * Anzeigenamen von Gruppen-/Ordnerknoten. Wird auf dem eigenen `JTree`
 * installiert (siehe REQ-013).
 */
object GradleTasksSpeedSearch {

    fun installOn(tree: JTree): TreeSpeedSearch =
        TreeSpeedSearch.installOn(tree, false) { path ->
            when (val data = (path.lastPathComponent as? DefaultMutableTreeNode)?.userObject) {
                is TaskNodeData -> data.descriptor.taskName
                is FolderNodeData -> data.folder.displayName
                is GroupHeaderNodeData -> data.groupPath
                else -> ""
            }
        }
}
