package de.example.gradletasksview.ui

import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import de.example.gradletasksview.icons.GradleTasksIcons
import de.example.gradletasksview.persistence.GradleTasksViewConfigStore
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

/**
 * Rendert Gruppenzeilen (Default/Sort), virtuelle Ordner (Nested) und Task-Blaetter.
 *
 * Die Icons stammen aus den plugin-eigenen Ressourcen ([GradleTasksIcons]) und nicht mehr
 * aus `AllIcons`/`ExternalSystemIcons`. Damit haengt die Darstellung weder davon ab, unter
 * welchem Plattformmodul ein Icon gerade ausgeliefert wird, noch davon, ob eine
 * `AllIcons`-Konstante in der jeweiligen IDE-Version noch auf eine existierende Ressource
 * zeigt - beides hatte sich beim Nachstellen der originalen Gradle-Ansicht als
 * unzuverlaessig erwiesen (siehe docs/INTEGRATION_STRATEGY.md, "Tree-Row-Icons").
 *
 * Vorrang hat jedoch ein in `default.yaml`/`custom.yaml` konfiguriertes eigenes Icon
 * ([GradleTasksViewConfigStore.iconFor]); die eingebauten Icons sind nur der Rueckfall.
 */
class GradleTasksTreeCellRenderer(private val configStore: GradleTasksViewConfigStore) : ColoredTreeCellRenderer() {

    override fun customizeCellRenderer(
        tree: JTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ) {
        val userObject = (value as? DefaultMutableTreeNode)?.userObject

        when (userObject) {
            // Gruppenzeile (Default/Sort) und virtueller Ordner (Nested) teilen sich bewusst
            // dasselbe Ordner-Icon: Es ist in allen drei Modi derselbe Knotentyp aus Sicht des
            // Benutzers - ein aufklappbarer Container fuer Tasks - und soll deshalb ueberall
            // gleich aussehen. Nur die Beschriftung unterscheidet sich (voller Gruppenpfad in
            // Default/Sort, einzelnes Pfadsegment in Nested).
            is GroupHeaderNodeData -> {
                icon = configStore.iconFor(userObject.groupPath) ?: folderIcon(expanded)
                append(userObject.groupPath.ifEmpty { "(default)" }, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            }

            is FolderNodeData -> {
                icon = configStore.iconFor(userObject.folder.fullPath) ?: folderIcon(expanded)
                append(userObject.folder.displayName, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            }

            is TaskNodeData -> {
                // Schluessel ist der Taskname, so wie er in der Ansicht steht - derselbe Wert,
                // den ein Benutzer in die YAML-Datei schreiben wuerde.
                icon = configStore.iconFor(userObject.descriptor.taskName) ?: GradleTasksIcons.Task
                append(userObject.descriptor.taskName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                userObject.descriptor.description?.let {
                    append("  $it", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
                toolTipText = userObject.descriptor.description
            }

            else -> Unit
        }
    }

    /** Ordner-Icon folgt dem Auf-/Zuklappzustand, wie in der Projektansicht der IDE. */
    private fun folderIcon(expanded: Boolean) =
        if (expanded) GradleTasksIcons.FolderOpen else GradleTasksIcons.Folder
}
