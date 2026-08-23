package de.example.gradletasksview.ui

import de.example.gradletasksview.model.GradleTaskDescriptor
import de.example.gradletasksview.model.NestedFolderNode
import de.example.gradletasksview.persistence.TaskViewConfigKey
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeNode

/**
 * Nutzdaten der `DefaultMutableTreeNode`-Knoten im eigenen `JTree`.
 * - [ModuleNodeData]: Gradle-Modul; in `Default` immer, in `Sort`/`Nested` nur bei
 *   eingeschalteter Modulgliederung.
 * - [TasksNodeData]: der feste Zwischenknoten `Tasks` unter einem Modul im `Default`-Modus.
 * - [GroupHeaderNodeData]: flache Gruppenzeile in Sort (kein Ordner-Icon,
 *   nicht ein-/ausklappbar im Sinne des persistierten Expand-States).
 * - [FolderNodeData]: virtueller Ordnerknoten in Default und Nested.
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
    override fun toString(): String = folder.compactedName
}

/**
 * Angezeigt wird der Gradle-Projektpfad, nicht der IDE-Modulname: Er ist eindeutig und zugleich
 * genau der Schluessel, den man in `default.yaml`/`custom.yaml` schreiben wuerde. Der Modulname
 * steht als Zusatz daneben (siehe [GradleTasksTreeCellRenderer]).
 */
data class ModuleNodeData(val gradlePath: String, val moduleName: String) : GradleTasksTreeNodeData {
    override fun toString(): String = gradlePath
}

data class TaskNodeData(val descriptor: GradleTaskDescriptor) : GradleTasksTreeNodeData {
    override fun toString(): String = descriptor.taskName
}

/**
 * Der Knoten `Tasks` zwischen Modulknoten und Ordnerbaum im `Default`-Modus, wie in der
 * originalen Gradle-Ansicht der IDE (REQ-077).
 *
 * Bewusst ein `object`: Der Knoten traegt keine Daten, sondern ist reine Struktur. Er ist
 * deshalb auch **nicht** konfigurierbar - [configKeyOf] liefert `null`, es gibt also keinen
 * Schluessel in `default.yaml`/`custom.yaml`, kein eigenes Icon und keinen eigenen
 * Expand-Zustand. Ein Schluessel `Tasks` liesse sich nicht von einer gleichnamigen echten
 * Gradle-Gruppe unterscheiden, und solange der Knoten das einzige Kind seines Moduls ist,
 * waere ihn zuzuklappen ohnehin dasselbe wie das Modul zuzuklappen.
 */
data object TasksNodeData : GradleTasksTreeNodeData {
    const val DISPLAY_NAME: String = "Tasks"
    override fun toString(): String = DISPLAY_NAME
}

/**
 * Gradle-Projektpfad des Moduls, unter dem [node] haengt, oder `null`, wenn ueber [node] kein
 * Modulknoten steht - in `Sort`/`Nested` mit ausgeschalteter Modulgliederung ist das der
 * Normalfall, in `Default` kommt es nicht vor.
 *
 * Steigt ueber beliebige Zwischenknoten hinweg nach oben und findet das Modul deshalb auch
 * durch den [TasksNodeData]-Knoten hindurch.
 *
 * Der Modulbezug wird bewusst aus der Baumstruktur hergeleitet, statt ihn in jeden Knoten zu
 * kopieren: Er ist bereits durch die Position im Baum festgelegt, und eine zweite Ablage
 * koennte davon abweichen.
 */
fun enclosingModulePath(node: TreeNode?): String? {
    var current: TreeNode? = node
    while (current != null) {
        val data = (current as? DefaultMutableTreeNode)?.userObject
        if (data is ModuleNodeData) return data.gradlePath
        current = current.parent
    }
    return null
}

/**
 * Konfigurationsschluessel eines Knotens, oder `null` fuer Knoten ohne eigenen Eintrag in
 * `default.yaml`/`custom.yaml`: Task-, Wurzel- und [TasksNodeData]-Knoten.
 */
fun configKeyOf(node: DefaultMutableTreeNode): TaskViewConfigKey? =
    when (val data = node.userObject) {
        is ModuleNodeData -> TaskViewConfigKey.module(data.gradlePath)
        is GroupHeaderNodeData -> TaskViewConfigKey.group(data.groupPath, enclosingModulePath(node))
        is FolderNodeData -> TaskViewConfigKey.group(data.folder.fullPath, enclosingModulePath(node))
        else -> null
    }
