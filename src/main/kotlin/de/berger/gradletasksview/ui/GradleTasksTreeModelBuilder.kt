package de.berger.gradletasksview.ui

import de.berger.gradletasksview.model.GradleTaskDescriptor
import de.berger.gradletasksview.model.GradleTaskViewMode
import de.berger.gradletasksview.model.ModuleTreeNode
import de.berger.gradletasksview.model.NestedFolderNode
import de.berger.gradletasksview.model.buildModuleTree
import de.berger.gradletasksview.model.buildNestedTree
import de.berger.gradletasksview.model.buildSortedGroups
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * Baut aus einer (bereits gefilterten) Task-Liste das `DefaultTreeModel` fuer
 * den jeweils aktiven [GradleTaskViewMode]. Reine In-Memory-Transformation ohne
 * PSI-Zugriff - unabhaengig von der Codebasengroesse, nur von der Task-Anzahl.
 *
 * Die Modulgliederung (`showModules`) ist **orthogonal** zu `Sort` und `Nested`: Sie schiebt
 * lediglich eine Modulebene nach aussen, waehrend die Gliederung darunter unveraendert die des
 * jeweiligen Modus bleibt. In `Default` ist die Modulebene dagegen fester Bestandteil der
 * Ansicht und der Parameter wird ignoriert - dort bildet sie zusammen mit dem `Tasks`-Knoten
 * die originale Gradle-Ansicht nach (siehe REQ-077). Es entstehen also fuenf Kombinationen,
 * kein vierter Modus (siehe REQ-074).
 */
object GradleTasksTreeModelBuilder {

    fun build(
        mode: GradleTaskViewMode,
        tasks: List<GradleTaskDescriptor>,
        showModules: Boolean = false
    ): DefaultTreeModel {
        val root = DefaultMutableTreeNode("root")

        if (showModules || mode == GradleTaskViewMode.DEFAULT) {
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
        addModuleContent(moduleNode, mode, module.tasks)
        module.childrenModules.values.forEach { child -> addModuleNodes(moduleNode, child, mode) }
        parent.add(moduleNode)
    }

    /**
     * In `Default` steht zwischen Modulknoten und Ordnerbaum der feste Knoten `Tasks`, wie im
     * Original. Ein Modul ohne eigene Tasks bekommt **keinen** - dort stuenden sonst eine leere
     * Zeile und ein Klick ins Nichts (siehe REQ-077).
     */
    private fun addModuleContent(
        moduleNode: DefaultMutableTreeNode,
        mode: GradleTaskViewMode,
        tasks: List<GradleTaskDescriptor>
    ) {
        if (mode != GradleTaskViewMode.DEFAULT) {
            addModeContent(moduleNode, mode, tasks)
            return
        }
        if (tasks.isEmpty()) return

        val tasksNode = DefaultMutableTreeNode(TasksNodeData)
        addModeContent(tasksNode, mode, tasks)
        moduleNode.add(tasksNode)
    }

    private fun addModeContent(
        parent: DefaultMutableTreeNode,
        mode: GradleTaskViewMode,
        tasks: List<GradleTaskDescriptor>
    ) {
        when (mode) {
            // Default und Nested teilen sich denselben Ordnerbaum; der Unterschied liegt eine
            // Ebene hoeher (feste Modulebene und Tasks-Knoten, siehe REQ-077).
            GradleTaskViewMode.DEFAULT, GradleTaskViewMode.NESTED -> buildNested(parent, tasks)
            GradleTaskViewMode.SORT -> buildSort(parent, tasks)
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
