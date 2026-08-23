package de.berger.gradletasksview.ui

import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import de.berger.gradletasksview.icons.GradleTasksIcons
import de.berger.gradletasksview.persistence.GradleTasksViewConfigStore
import de.berger.gradletasksview.persistence.TaskViewConfigKey
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

/**
 * Rendert Modulknoten (in `Default` immer, sonst bei eingeschalteter Modulgliederung), den
 * `Tasks`-Strukturknoten (`Default`), Gruppenzeilen (`Sort`), virtuelle Ordner
 * (`Default`/`Nested`) und Task-Blaetter.
 *
 * Die Icons stammen aus den plugin-eigenen Ressourcen ([GradleTasksIcons]) und nicht mehr
 * aus `AllIcons`/`ExternalSystemIcons`. Damit haengt die Darstellung weder davon ab, unter
 * welchem Plattformmodul ein Icon gerade ausgeliefert wird, noch davon, ob eine
 * `AllIcons`-Konstante in der jeweiligen IDE-Version noch auf eine existierende Ressource
 * zeigt - beides hatte sich beim Nachstellen der originalen Gradle-Ansicht als
 * unzuverlaessig erwiesen (siehe ADR-004 und ADR-005).
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
        val node = value as? DefaultMutableTreeNode
        val modulePath = enclosingModulePath(node)
        // Ausgeblendete Knoten sind normalerweise gar nicht im Baum; sichtbar werden sie nur
        // ueber den Umschalter "Show Hidden" - dann ausgegraut, damit der Unterschied zum
        // regulaeren Inhalt sofort erkennbar ist (REQ-078).
        val hidden = node?.let(::configKeyOf)?.let(configStore::isHidden) == true
        val bold = if (hidden) SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES else SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
        val regular = if (hidden) SimpleTextAttributes.GRAYED_ATTRIBUTES else SimpleTextAttributes.REGULAR_ATTRIBUTES

        when (val userObject = node?.userObject) {
            // Modulknoten: der Gradle-Projektpfad ist der Anzeigetext, der IDE-Modulname steht
            // nur als Zusatz daneben. Der Pfad ist eindeutig und zugleich der Schluessel, den
            // man in default.yaml/custom.yaml schreiben wuerde.
            is ModuleNodeData -> {
                icon = configStore.iconFor(TaskViewConfigKey.module(userObject.gradlePath))
                    ?: GradleTasksIcons.Module
                append(userObject.gradlePath, bold)
                userObject.moduleName
                    .takeIf { it.isNotEmpty() && it != userObject.gradlePath }
                    ?.let { append("  $it", SimpleTextAttributes.GRAYED_ATTRIBUTES) }
            }

            // Fester Strukturknoten "Tasks" unter einem Modul im Default-Modus. Er hat bewusst
            // keinen Konfigurationsschluessel und fragt deshalb - anders als alle uebrigen
            // Knoten - gar nicht erst nach einem eigenen Icon (siehe REQ-077).
            TasksNodeData -> {
                icon = GradleTasksIcons.TaskList
                append(TasksNodeData.DISPLAY_NAME, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            }

            // Gruppenzeile (Sort) und virtueller Ordner (Default/Nested) teilen sich bewusst
            // dasselbe Ordner-Icon: Es ist in allen drei Modi derselbe Knotentyp aus Sicht des
            // Benutzers - ein aufklappbarer Container fuer Tasks - und soll deshalb ueberall
            // gleich aussehen. Nur die Beschriftung unterscheidet sich (voller Gruppenpfad in
            // Sort, zusammengefasster Pfad in Default/Nested).
            is GroupHeaderNodeData -> {
                icon = configStore.iconFor(TaskViewConfigKey.group(userObject.groupPath, modulePath))
                    ?: folderIcon(expanded)
                append(userObject.groupPath.ifEmpty { "(default)" }, bold)
            }

            is FolderNodeData -> {
                icon = configStore.iconFor(TaskViewConfigKey.group(userObject.folder.fullPath, modulePath))
                    ?: folderIcon(expanded)
                // compactedName statt displayName: Ordner ohne eigene Tasks sind mit ihrem
                // einzigen Kind zu einer Zeile zusammengefasst und heissen dann "server/gui"
                // (REQ-004). Der Schluessel fuer Icon und Expand-Zustand bleibt der volle Pfad.
                append(userObject.folder.compactedName, bold)
            }

            is TaskNodeData -> {
                // Schluessel ist der Taskname, so wie er in der Ansicht steht - derselbe Wert,
                // den ein Benutzer in die YAML-Datei schreiben wuerde.
                icon = configStore.iconFor(TaskViewConfigKey.task(userObject.descriptor.taskName, modulePath))
                    ?: GradleTasksIcons.Task
                append(userObject.descriptor.taskName, regular)
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
