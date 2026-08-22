package de.example.gradletasksview.ui

import de.example.gradletasksview.model.GradleTaskDescriptor
import de.example.gradletasksview.model.GradleTaskViewMode
import de.example.gradletasksview.model.NestedFolderNode
import de.example.gradletasksview.model.buildNestedTree
import de.example.gradletasksview.model.buildSortedGroups
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * Baut aus einer (bereits gefilterten) Task-Liste das `DefaultTreeModel` fuer
 * den jeweils aktiven [GradleTaskViewMode]. Reine In-Memory-Transformation ohne
 * PSI-Zugriff - unabhaengig von der Codebasengroesse, nur von der Task-Anzahl.
 */
object GradleTasksTreeModelBuilder {

    fun build(mode: GradleTaskViewMode, tasks: List<GradleTaskDescriptor>): DefaultTreeModel {
        val root = DefaultMutableTreeNode("root")

        when (mode) {
            GradleTaskViewMode.DEFAULT -> buildDefault(root, tasks)
            GradleTaskViewMode.SORT -> buildSort(root, tasks)
            GradleTaskViewMode.NESTED -> buildNested(root, tasks)
        }

        return DefaultTreeModel(root)
    }

    /** Reihenfolge, wie sie von Gradle/External System geliefert wird - keine eigene Sortierung. */
    private fun buildDefault(root: DefaultMutableTreeNode, tasks: List<GradleTaskDescriptor>) {
        val groups = LinkedHashMap<String, MutableList<GradleTaskDescriptor>>()
        tasks.forEach { task ->
            groups.getOrPut(task.groupName.orEmpty()) { mutableListOf() } += task
        }

        groups.forEach { (groupPath, groupTasks) ->
            val groupNode = DefaultMutableTreeNode(GroupHeaderNodeData(groupPath))
            groupTasks.forEach { task -> groupNode.add(DefaultMutableTreeNode(TaskNodeData(task))) }
            root.add(groupNode)
        }
    }

    private fun buildSort(root: DefaultMutableTreeNode, tasks: List<GradleTaskDescriptor>) {
        buildSortedGroups(tasks).forEach { (groupPath, groupTasks) ->
            val groupNode = DefaultMutableTreeNode(GroupHeaderNodeData(groupPath))
            groupTasks.forEach { task -> groupNode.add(DefaultMutableTreeNode(TaskNodeData(task))) }
            root.add(groupNode)
        }
    }

    private fun buildNested(root: DefaultMutableTreeNode, tasks: List<GradleTaskDescriptor>) {
        val nestedRoot = buildNestedTree(tasks)
        nestedRoot.childrenTasks.forEach { task -> root.add(DefaultMutableTreeNode(TaskNodeData(task))) }
        nestedRoot.childrenFolders.values.forEach { folder -> root.add(buildFolderNode(folder)) }
    }

    private fun buildFolderNode(folder: NestedFolderNode): DefaultMutableTreeNode {
        val node = DefaultMutableTreeNode(FolderNodeData(folder))
        // Tasks stehen in jedem Ordner vor Unterordnern (siehe Plan.md, Abschnitt "Nested").
        folder.childrenTasks.forEach { task -> node.add(DefaultMutableTreeNode(TaskNodeData(task))) }
        folder.childrenFolders.values.forEach { child -> node.add(buildFolderNode(child)) }
        return node
    }
}
