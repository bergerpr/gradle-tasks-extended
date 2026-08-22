package de.example.gradletasksview.ui

import com.intellij.ide.CopyProvider
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.ui.PopupHandler
import com.intellij.ui.treeStructure.Tree
import de.example.gradletasksview.execution.GradleTaskExecutor
import de.example.gradletasksview.model.GradleTaskDescriptor
import de.example.gradletasksview.model.NestedFolderNode
import de.example.gradletasksview.persistence.GradleTasksExpandConfigStore
import java.awt.datatransfer.StringSelection
import java.awt.event.HierarchyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * Eigener `JTree` fuer die Gradle-Task-Ansicht. Baut auf `com.intellij.ui.treeStructure.Tree`
 * auf (Standard-Basisklasse fuer IntelliJ-Trees) und bildet Doppelklick-Ausfuehrung,
 * Kontextmenue-Datenversorgung und Copy selbst nach, statt eine fremde View-Instanz
 * zu delegieren (Weg B, siehe Plan.md "Architekturentscheidung").
 */
class GradleTasksTree(private val project: Project) : Tree(DefaultTreeModel(DefaultMutableTreeNode())), DataProvider {

    private val executor = GradleTaskExecutor(project)
    val stateManager = GradleTasksTreeStateManager(this, project.service<GradleTasksExpandConfigStore>())

    /**
     * Wird bei jeder Aenderung des SpeedSearch-Suchtexts aufgerufen. `TreeSpeedSearch`
     * selbst blendet keine Knoten aus (es navigiert nur zum naechsten Treffer) - die
     * eigentliche Filterung erfolgt datengetrieben im [GradleTaskViewRefresher] vor dem
     * Aufbau von Sort/Nested (siehe Plan.md, Abschnitt "Filterung").
     */
    var onFilterTextChanged: (String) -> Unit = {}

    /**
     * Wird genau einmal aufgerufen, sobald der Baum erstmals tatsaechlich sichtbar wird
     * (`isShowing() == true`). Deckt den Fall ab, dass ein vom Vorlauf wiederhergestelltes,
     * bereits geoeffnetes ToolWindow seinen ersten Refresh auslost, bevor die Komponente
     * ueberhaupt in der sichtbaren Fensterhierarchie haengt - ein reines `repaint()` zu
     * diesem fruehen Zeitpunkt wuerde sonst folgenlos bleiben.
     */
    var onFirstShown: () -> Unit = {}
    private var firstShownFired = false

    init {
        isRootVisible = false
        showsRootHandles = true
        // Zusaetzliche Absicherung (nicht die Hauptursache eines frueher beobachteten
        // "Rechtsklick klappt alles auf"-Symptoms - das lag tatsaechlich an einem
        // periodischen Phantom-Refresh, siehe Kommentar bei speedSearch.addChangeListener
        // weiter unten): Standard-JTree-Verhalten ist expandsSelectedPaths=true, wodurch
        // das Setzen einer Selektion (z. B. durch `com.intellij.ui.treeStructure.Tree`s
        // eigenes Rechtsklick-Handling, das intern `getSelectionModel().setSelectionPath(...)`
        // aufruft) automatisch deren Vorfahren aufklappen wuerde. Das gezielte Aufklappen
        // ueber `scrollPathToVisible(...)` in restoreSelection() ist davon unabhaengig.
        expandsSelectedPaths = false
        cellRenderer = GradleTasksTreeCellRenderer()
        // Zwingend notwendig, damit der eigene Renderer ueberhaupt zum Zeichnen verwendet wird:
        // `com.intellij.ui.treeStructure.Tree` installiert bereits in seinem eigenen Konstruktor
        // `setCellRenderer(new NodeRenderer())`. Ein spaeteres `setCellRenderer(...)` aktualisiert
        // zwar `JTree.getCellRenderer()`, aber nicht das Feld `currentCellRenderer` in
        // `javax.swing.plaf.basic.BasicTreeUI` - und genau dieses Feld liest
        // `com.intellij.ui.tree.ui.DefaultTreeUI.getRenderer(...)` beim Zeichnen jeder Zeile.
        // Ohne diesen `updateUI()`-Aufruf zeichnet der Baum deshalb weiter mit `NodeRenderer`
        // (nur `toString()` je Knoten: keine Icons, keine Fettschrift fuer Gruppen/Ordner, keine
        // graue Task-Description) und [GradleTasksTreeCellRenderer.customizeCellRenderer] wird
        // nie aufgerufen. `updateUI()` installiert die TreeUI neu; deren `updateRenderer()`
        // uebernimmt dabei den aktuell gesetzten Renderer.
        //
        // Per Reflection auf `BasicTreeUI.currentCellRenderer` in einer laufenden Sandbox
        // verifiziert: direkt nach `setCellRenderer(...)` steht dort noch `NodeRenderer`,
        // nach `updateUI()` der eigene Renderer. Das war die eigentliche Ursache des lange
        // als "Icons werden nicht gezeichnet" dokumentierten Problems - siehe
        // docs/INTEGRATION_STRATEGY.md, Abschnitt "Tree-Row-Icons".
        updateUI()
        val speedSearch = GradleTasksSpeedSearch.installOn(this)
        var lastKnownFilterText = ""
        speedSearch.addChangeListener {
            // java.beans.PropertyChangeSupport#firePropertyChange feuert IMMER, wenn
            // oldValue == null ist - selbst wenn newValue ebenfalls null ist und sich
            // effektiv nichts geaendert hat (nur bei nicht-null oldValue wird auf
            // Gleichheit geprueft und unterdrueckt). TreeSpeedSearch loest dieses
            // No-Op-Event periodisch aus, daher hier explizit auf eine tatsaechliche
            // Textaenderung pruefen, bevor ein teurer Rebuild ausgeloest wird - sonst
            // wuerde jeder manuelle Expand/Collapse-Zustand durch den naechsten
            // Phantom-Refresh wieder rueckgaengig gemacht.
            val newFilterText = speedSearch.enteredPrefix.orEmpty()
            if (newFilterText != lastKnownFilterText) {
                lastKnownFilterText = newFilterText
                onFilterTextChanged(newFilterText)
            }
        }

        addHierarchyListener { event ->
            if (!firstShownFired &&
                event.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L &&
                isShowing
            ) {
                firstShownFired = true
                onFirstShown()
            }
        }
        stateManager.startListeningForUserExpansionChanges()

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount != 2) return
                val path = getPathForLocation(e.x, e.y) ?: return
                val data = (path.lastPathComponent as? DefaultMutableTreeNode)?.userObject
                if (data is TaskNodeData) {
                    executor.execute(data.descriptor)
                }
            }
        })

        registerContextMenu()
    }

    private fun registerContextMenu() {
        val group = ActionManager.getInstance().getAction("GradleTasksExtended.ContextMenu")
        if (group is ActionGroup) {
            PopupHandler.installPopupMenu(this, group, ActionPlaces.POPUP)
        }
    }

    fun selectedTaskDescriptors(): List<GradleTaskDescriptor> =
        selectionPaths.orEmpty().mapNotNull { path ->
            ((path.lastPathComponent as? DefaultMutableTreeNode)?.userObject as? TaskNodeData)?.descriptor
        }

    fun selectedFolder(): NestedFolderNode? {
        val path = selectionPath ?: return null
        return ((path.lastPathComponent as? DefaultMutableTreeNode)?.userObject as? FolderNodeData)?.folder
    }

    fun runSelectedTask() {
        selectedTaskDescriptors().firstOrNull()?.let { executor.execute(it) }
    }

    override fun getData(dataId: String): Any? = when {
        PlatformDataKeys.COPY_PROVIDER.`is`(dataId) -> copyProvider
        else -> null
    }

    private val copyProvider = object : CopyProvider {
        override fun performCopy(dataContext: DataContext) {
            val text = selectedTaskDescriptors().joinToString("\n") { it.taskPath }
                .ifEmpty { selectedFolder()?.fullPath.orEmpty() }
            if (text.isNotEmpty()) {
                CopyPasteManager.getInstance().setContents(StringSelection(text))
            }
        }

        override fun isCopyEnabled(dataContext: DataContext): Boolean =
            selectedTaskDescriptors().isNotEmpty() || selectedFolder() != null

        override fun isCopyVisible(dataContext: DataContext): Boolean = true

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
    }
}
