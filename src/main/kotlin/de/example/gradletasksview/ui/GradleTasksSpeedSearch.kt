package de.example.gradletasksview.ui

import com.intellij.ui.TreeSpeedSearch
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

/**
 * Filterung ueber `TreeSpeedSearch`. Wird auf dem eigenen `JTree` installiert (siehe REQ-013).
 *
 * Tasks werden gegen ihren **qualifizierten** Pfad (`:app:buildImage`) gematcht, nicht nur
 * gegen den blanken Namen. Der Name ist darin als Teilzeichenkette enthalten, die bisherige
 * Suche funktioniert also unveraendert - zusaetzlich laesst sich damit aber auf ein Modul
 * eingrenzen, die naheliegendste Filterung in einem grossen Build (siehe REQ-013).
 */
object GradleTasksSpeedSearch {

    fun installOn(tree: JTree): TreeSpeedSearch =
        TreeSpeedSearch.installOn(tree, false) { path ->
            when (val data = (path.lastPathComponent as? DefaultMutableTreeNode)?.userObject) {
                is TaskNodeData -> data.descriptor.taskPath
                is FolderNodeData -> data.folder.displayName
                is GroupHeaderNodeData -> data.groupPath
                is ModuleNodeData -> data.gradlePath
                else -> ""
            }
        }
}
