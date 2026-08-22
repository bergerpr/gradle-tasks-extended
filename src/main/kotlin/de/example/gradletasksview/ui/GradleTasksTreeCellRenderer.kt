package de.example.gradletasksview.ui

import com.intellij.icons.AllIcons
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import icons.ExternalSystemIcons
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

/**
 * Rendert Gruppenzeilen (Default/Sort), virtuelle Ordner (Nested) und Task-Blaetter
 * mit denselben Icons wie die originale Gradle-Tool-Window-Ansicht:
 * - Gruppen/Ordner: `AllIcons.Nodes.ConfigFolder` (siehe `TasksNode`/das anonyme
 *   Gruppenknoten-Icon in `com.intellij.openapi.externalSystem.view.TasksNode`).
 * - Tasks: `ExternalSystemIcons.Task` (siehe `DefaultExternalSystemUiAware.getTaskIcon()`,
 *   das Gradle ueber `GradleManager.getTaskIcon()` verwendet).
 *
 * Bekannte Einschraenkung: In dieser Session wurde ausfuehrlich verifiziert, dass die
 * Icon-Zuweisung selbst korrekt ist (gueltiges Icon-Objekt, korrekte Groesse), die
 * Icons in der laufenden Sandbox aber dennoch nicht sichtbar gezeichnet werden - auch
 * nicht mit einem alternativen `DefaultTreeCellRenderer` oder einem manuell gemalten
 * Test-Icon. Siehe docs/INTEGRATION_STRATEGY.md, Abschnitt "Bekannte Risiken", fuer
 * Details und den aktuellen Stand.
 */
class GradleTasksTreeCellRenderer : ColoredTreeCellRenderer() {

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
            is GroupHeaderNodeData -> {
                icon = AllIcons.Nodes.ConfigFolder
                append(userObject.groupPath.ifEmpty { "(default)" }, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            }

            is FolderNodeData -> {
                icon = AllIcons.Nodes.ConfigFolder
                append(userObject.folder.displayName, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            }

            is TaskNodeData -> {
                icon = ExternalSystemIcons.Task
                append(userObject.descriptor.taskName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                userObject.descriptor.description?.let {
                    append("  $it", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
                toolTipText = userObject.descriptor.description
            }

            else -> Unit
        }
    }
}
