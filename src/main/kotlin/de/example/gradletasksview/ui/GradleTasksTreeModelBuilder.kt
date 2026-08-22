package de.example.gradletasksview.ui

import de.example.gradletasksview.model.GradleTaskDescriptor
import de.example.gradletasksview.model.GradleTaskViewMode
import de.example.gradletasksview.model.ModuleTreeNode
import de.example.gradletasksview.model.NestedFolderNode
import de.example.gradletasksview.model.buildModuleTree
import de.example.gradletasksview.model.buildNestedTree
import de.example.gradletasksview.model.buildSortedGroups
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * Baut aus einer (bereits gefilterten) Task-Liste das `DefaultTreeModel` fuer
 * den jeweils aktiven [GradleTaskViewMode]. Reine In-Memory-Transformation ohne
 * PSI-Zugriff - unabhaengig von der Codebasengroesse, nur von der Task-Anzahl.
 *
 * Die Modulgliederung (`showModules`) ist **orthogonal** zum Modus: Sie schiebt lediglich eine
 * Modulebene nach aussen, waehrend die Gliederung darunter unveraendert die des jeweiligen
 * Modus bleibt. Es entstehen also sechs Kombinationen, kein vierter Modus (siehe REQ-074).
 */
object GradleTasksTreeModelBuilder {

    fun build(
        mode: GradleTaskViewMode,
        tasks: List<GradleTaskDescriptor>,
        showModules: Boolean = false
    ): DefaultTreeModel {
        val root = DefaultMutableTreeNode("root")

        if (showModules) {
            addModuleNodes(root, buildModuleTree(tasks), mode)
        } else {
            addModeContent(root, mode, tasks)
        }

        return DefaultTreeModel(root)
    }

    /**
     * Modul zuerst mit seinem eigenen Inhalt, danach die Untermodule - dieselbe Reihenfolge wie
     * "Tasks vor Unterordnern" im Nested-Modus (siehe REQ-004).
     */
    private fun addModuleNodes(parent: DefaultMutableTreeNode, module: ModuleTreeNode, mode: GradleTaskViewMode) {
        val moduleNode = DefaultMutableTreeNode(ModuleNodeData(module.gradlePath, module.moduleName))
        addModeContent(moduleNode, mode, module.tasks)
        module.childrenModules.values.forEach { child -> addModuleNodes(moduleNode, child, mode) }
        parent.add(moduleNode)
    }

    private fun addModeContent(
        parent: DefaultMutableTreeNode,
        mode: GradleTaskViewMode,
        tasks: List<GradleTaskDescriptor>
    ) {
        when (mode) {
            GradleTaskViewMode.DEFAULT -> buildDefault(parent, tasks)
            GradleTaskViewMode.SORT -> buildSort(parent, tasks)
            GradleTaskViewMode.NESTED -> buildNested(parent, tasks)
        }
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
        // Tasks stehen in jedem Ordner vor Unterordnern (siehe REQ-004).
        folder.childrenTasks.forEach { task -> node.add(DefaultMutableTreeNode(TaskNodeData(task))) }
        folder.childrenFolders.values.forEach { child -> node.add(buildFolderNode(child)) }
        return node
    }
}
