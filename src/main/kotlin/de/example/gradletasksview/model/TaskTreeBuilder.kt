package de.example.gradletasksview.model

import de.example.gradletasksview.util.normalizeGroupPath
import de.example.gradletasksview.util.normalizeGroupSegments

/**
 * Baut die flache, sortierte Gruppenliste fuer den Sort-Modus: Gruppen nach
 * vollstaendigem Gruppenpfad, Tasks innerhalb jeder Gruppe nach Taskname.
 * Es werden keine virtuellen Ordnerknoten erzeugt.
 */
fun buildSortedGroups(tasks: List<GradleTaskDescriptor>): List<Pair<String, List<GradleTaskDescriptor>>> =
    tasks
        .groupBy { normalizeGroupPath(it.groupName) }
        .mapValues { (_, groupTasks) -> groupTasks.sortedWith(taskComparator) }
        .toList()
        .sortedWith(compareBy(groupPathComparator) { it.first })

/**
 * Baut den verschachtelten Ordnerbaum fuer den Nested-Modus aus den
 * `/`-getrennten Gruppenpfaden. Innerhalb eines Ordners erscheinen Tasks vor
 * Unterordnern (Rendering-Reihenfolge, siehe [de.example.gradletasksview.ui]-Paket);
 * hier werden bereits beide Listen sortiert abgelegt.
 */
fun buildNestedTree(tasks: List<GradleTaskDescriptor>): NestedFolderNode {
    val root = NestedFolderNode(displayName = "", fullPath = "")

    tasks.forEach { task ->
        val segments = normalizeGroupSegments(task.groupName)

        var current = root
        val pathParts = mutableListOf<String>()

        segments.forEach { segment ->
            pathParts += segment
            val fullPath = pathParts.joinToString("/")
            current = current.childrenFolders.getOrPut(segment) {
                NestedFolderNode(displayName = segment, fullPath = fullPath)
            }
        }

        current.childrenTasks += task
    }

    sortRecursively(root)
    return root
}

private fun sortRecursively(node: NestedFolderNode) {
    node.childrenTasks.sortWith(taskComparator)

    val sortedFolders = node.childrenFolders.values.sortedWith(folderComparator)
    node.childrenFolders.clear()
    sortedFolders.forEach { folder ->
        node.childrenFolders[folder.displayName] = folder
        sortRecursively(folder)
    }
}
