package de.berger.gradletasksview.model

import de.berger.gradletasksview.util.normalizeGroupPath
import de.berger.gradletasksview.util.normalizeGroupSegments

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
 * Unterordnern (Rendering-Reihenfolge, siehe [de.berger.gradletasksview.ui]-Paket);
 * hier werden bereits beide Listen sortiert abgelegt.
 *
 * Ordner ohne eigene Tasks und mit genau einem Kind werden anschliessend zu einer Zeile
 * zusammengefasst (siehe [compact] und REQ-004).
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
    // Bewusst NACH dem Sortieren: Das Zusammenfassen verschmilzt einen Knoten nur mit seinem
    // einzigen Kind und kann deshalb die Reihenfolge unter Geschwistern nicht veraendern.
    // Umgekehrt muesste der Komparator sonst mit mehrsegmentigen Namen umgehen.
    return compactChildren(root)
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

/**
 * Ersetzt die Kindordner von [node] durch ihre zusammengefassten Fassungen. Die Schluessel der
 * Abbildung sind danach die angezeigten Namen, nicht mehr die einzelnen Segmente.
 */
private fun compactChildren(node: NestedFolderNode): NestedFolderNode {
    val compacted = node.childrenFolders.values.map { compact(it) }
    node.childrenFolders.clear()
    compacted.forEach { node.childrenFolders[it.compactedName] = it }
    return node
}

/**
 * Zieht [folder] mit seinem einzigen Kind zu einer Zeile zusammen, solange er keine eigenen
 * Tasks hat - transitiv, aus `docker` + `server` + `gui` wird also `docker/server/gui`.
 *
 * Zwei Bedingungen muessen beide zutreffen. Ein Ordner mit eigenen Tasks bleibt stehen, weil er
 * etwas zu zeigen hat; ein Ordner mit zwei Kindern ebenfalls, weil sich sonst der gemeinsame
 * Ast verdoppeln muesste (`server/gui` und `server/cli` als Geschwister). Es ist dieselbe
 * Regel, die die IDE in der Projektansicht als "Compact Middle Packages" verwendet.
 *
 * [NestedFolderNode.fullPath] bleibt der volle Pfad des tiefsten verschmolzenen Knotens -
 * damit greifen die Konfigurationsschluessel aus `default.yaml`/`custom.yaml` unveraendert
 * weiter (REQ-071).
 */
private fun compact(folder: NestedFolderNode): NestedFolderNode {
    var deepest = folder
    val segments = mutableListOf(folder.displayName)

    while (deepest.childrenTasks.isEmpty() && deepest.childrenFolders.size == 1) {
        deepest = deepest.childrenFolders.values.first()
        segments += deepest.displayName
    }

    val result = if (deepest === folder) {
        folder
    } else {
        NestedFolderNode(
            displayName = deepest.displayName,
            fullPath = deepest.fullPath,
            childrenTasks = deepest.childrenTasks,
            childrenFolders = deepest.childrenFolders,
            compactedName = segments.joinToString("/")
        )
    }

    return compactChildren(result)
}
