package de.example.gradletasksview.ui

import de.example.gradletasksview.model.GradleTaskDescriptor
import de.example.gradletasksview.model.NestedFolderNode

/**
 * Nutzdaten der `DefaultMutableTreeNode`-Knoten im eigenen `JTree`.
 * - [GroupHeaderNodeData]: flache Gruppenzeile in Default/Sort (kein Ordner-Icon,
 *   nicht ein-/ausklappbar im Sinne des persistierten Expand-States).
 * - [FolderNodeData]: virtueller Ordnerknoten im Nested-Modus.
 * - [TaskNodeData]: ein einzelner Gradle-Task (Blattknoten in allen Modi).
 */
sealed interface GradleTasksTreeNodeData

/**
 * `toString()` wird bewusst auf den Anzeigenamen begrenzt statt auf den
 * generierten `data class`-Default zurueckzugreifen: `DefaultMutableTreeNode.toString()`
 * delegiert an `userObject.toString()`, und dieser Wert kann an mehreren Stellen
 * sichtbar werden (Accessibility, Logging, SpeedSearch-Fallback) - dort darf niemals
 * der vollstaendige, verschachtelte `GradleTaskDescriptor`/`NestedFolderNode`-Dump
 * auftauchen (z. B. `TaskNodeData(descriptor=GradleTaskDescriptor(taskName=...))`).
 */
data class GroupHeaderNodeData(val groupPath: String) : GradleTasksTreeNodeData {
    override fun toString(): String = groupPath
}

data class FolderNodeData(val folder: NestedFolderNode) : GradleTasksTreeNodeData {
    override fun toString(): String = folder.displayName
}

data class TaskNodeData(val descriptor: GradleTaskDescriptor) : GradleTasksTreeNodeData {
    override fun toString(): String = descriptor.taskName
}
