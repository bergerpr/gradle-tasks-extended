package de.berger.gradletasksview.ui

import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.ide.CopyProvider
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.ui.PopupHandler
import com.intellij.ui.speedSearch.SpeedSearchSupply
import com.intellij.ui.treeStructure.Tree
import de.berger.gradletasksview.execution.GradleTaskExecutor
import de.berger.gradletasksview.execution.SELECTED_GRADLE_TASKS
import de.berger.gradletasksview.execution.gradleTaskExecutionSettings
import de.berger.gradletasksview.model.GradleTaskDescriptor
import de.berger.gradletasksview.model.NestedFolderNode
import de.berger.gradletasksview.persistence.GradleTasksViewConfigStore
import de.berger.gradletasksview.persistence.TaskViewConfigKey
import java.awt.datatransfer.StringSelection
import java.awt.event.HierarchyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

/**
 * Eigener `JTree` fuer die Gradle-Task-Ansicht. Baut auf `com.intellij.ui.treeStructure.Tree`
 * auf (Standard-Basisklasse fuer IntelliJ-Trees) und bildet Doppelklick- und
 * Enter-Ausfuehrung, Kontextmenue-Datenversorgung und Copy selbst nach, statt eine fremde
 * View-Instanz zu delegieren (siehe ADR-001).
 */
class GradleTasksTree(private val project: Project) : Tree(DefaultTreeModel(DefaultMutableTreeNode())), UiDataProvider {

    private val executor = GradleTaskExecutor(project)
    private val configStore = project.service<GradleTasksViewConfigStore>()
    val stateManager = GradleTasksTreeStateManager(this, configStore)

    /**
     * Wird bei jeder Aenderung des SpeedSearch-Suchtexts aufgerufen. `TreeSpeedSearch`
     * selbst blendet keine Knoten aus (es navigiert nur zum naechsten Treffer) - die
     * eigentliche Filterung erfolgt datengetrieben im [GradleTaskViewRefresher] vor dem
     * Aufbau von Sort/Nested (siehe REQ-013).
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
        cellRenderer = GradleTasksTreeCellRenderer(configStore)
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
        // ADR-005.
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
                // Nur die Zeile, auf die der Doppelklick tatsaechlich ging, und das ist die
                // selektierte: Ein Doppelklick auf eine Gruppe klappt sie schon beim zweiten
                // `mousePressed` auf (Toggle der TreeUI), und `scrollsOnExpand` schiebt den
                // Baum dabei so weit, dass ihre Kinder sichtbar werden. Bis `mouseClicked`
                // eintrifft, liegt unter dem Mauszeiger deshalb oft schon ein Task - der
                // ohne diese Pruefung ausgefuehrt wuerde (REQ-089).
                if (path != selectionPath) return
                val data = (path.lastPathComponent as? DefaultMutableTreeNode)?.userObject
                if (data is TaskNodeData) {
                    executor.execute(data.descriptor)
                }
            }
        })

        registerContextMenu()
        registerRunOnEnter()
    }

    /**
     * Enter fuehrt den selektierten Task aus - dieselbe Ausfuehrung wie der Doppelklick
     * (REQ-087). Auf einem Modul-, Gruppen-, Ordner- oder `Tasks`-Knoten klappt Enter ihn
     * stattdessen auf bzw. zu, wie es der Doppelklick dort tut (REQ-089).
     *
     * Bewusst eine [AnAction] mit [CommonShortcuts.ENTER] auf dieser Komponente statt eines
     * eigenen `KeyListener` oder eines InputMap-Eintrags: Nur so laeuft der Tastendruck durch
     * den `IdeKeyEventDispatcher` der Plattform und ordnet sich damit den vorrangigen
     * Verbrauchern unter - allen voran der Speed Search, deren Popup Enter zunaechst
     * schliessen soll (siehe [runOnEnterEnabled]).
     *
     * Numpad-Enter ist mit abgedeckt und braucht keinen zweiten Shortcut: AWT meldet dafuer
     * denselben `keyCode` `VK_ENTER` und unterscheidet die Taste nur ueber `getKeyLocation()`,
     * was beim Abgleich eines `KeyStroke` keine Rolle spielt.
     */
    private fun registerRunOnEnter() {
        object : AnAction() {
            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = runOnEnterEnabled()
            }

            override fun actionPerformed(e: AnActionEvent) {
                singleSelectedTaskDescriptor()?.let {
                    executor.execute(it)
                    return
                }
                val path = singleSelectedPath() ?: return
                // Laeuft ueber dieselben Expansion-Events wie ein Mausklick und wird damit
                // genauso in custom.yaml festgehalten.
                if (isExpanded(path)) collapsePath(path) else expandPath(path)
            }

            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
        }.registerCustomShortcutSet(CommonShortcuts.ENTER, this)
    }

    /**
     * Enter wirkt nur bei **genau einer** selektierten Zeile: Bei einer Mehrfachauswahl bleibt
     * der Tastendruck folgenlos (ausdrueckliche Anforderung, REQ-087) - anders als das
     * Kontextmenue, das mehrere Tasks desselben Gradle-Projekts in einem Aufruf startet.
     * Ein Task wird ausgefuehrt, jeder andere Knoten mit Kindern auf- oder zugeklappt.
     *
     * Waehrend die Speed Search offen ist, bleibt die Aktion abgeschaltet, damit Enter dort
     * erwartungsgemaess erst die Suche beendet; der naechste Enter wirkt dann.
     */
    private fun runOnEnterEnabled(): Boolean {
        if (SpeedSearchSupply.getSupply(this)?.isPopupActive == true) return false
        val path = singleSelectedPath() ?: return false
        return singleSelectedTaskDescriptor() != null || !model.isLeaf(path.lastPathComponent)
    }

    private fun singleSelectedPath(): TreePath? = selectionPaths?.singleOrNull()

    /**
     * Der Task der Selektion, wenn genau eine Zeile ausgewaehlt ist und diese ein Task-Knoten
     * ist - sonst `null`.
     */
    private fun singleSelectedTaskDescriptor(): GradleTaskDescriptor? {
        val path = singleSelectedPath() ?: return null
        return ((path.lastPathComponent as? DefaultMutableTreeNode)?.userObject as? TaskNodeData)?.descriptor
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

    /**
     * Konfigurationsschluessel aller selektierten Knoten. Knoten ohne Schluessel - der
     * `Tasks`-Strukturknoten des `Default`-Modus (REQ-077) - fallen dabei heraus.
     */
    fun selectedConfigKeys(): List<TaskViewConfigKey> =
        selectionPaths.orEmpty().mapNotNull { path ->
            (path.lastPathComponent as? DefaultMutableTreeNode)?.let(::configKeyOf)
        }

    fun selectedFolder(): NestedFolderNode? {
        val path = selectionPath ?: return null
        return ((path.lastPathComponent as? DefaultMutableTreeNode)?.userObject as? FolderNodeData)?.folder
    }

    /**
     * Stellt dem Action-System den [CopyProvider] bereit, damit "Copy" (Ctrl+C) im Baum
     * funktioniert.
     *
     * Bewusst [UiDataProvider] und nicht das aeltere `DataProvider`: dessen
     * `getData(String)` ist in der aktuellen Plattform mit `@ApiStatus.Obsolete` markiert.
     * Der Unterschied ist nicht nur kosmetisch - `uiDataSnapshot` wird einmal pro
     * Datenschnappschuss auf dem EDT aufgerufen und befuellt eine [DataSink], statt bei
     * jedem einzelnen Schluessel erneut nach einem String-Vergleich befragt zu werden.
     */
    override fun uiDataSnapshot(sink: DataSink) {
        sink[PlatformDataKeys.COPY_PROVIDER] = copyProvider
        sink[SELECTED_GRADLE_TASKS] = selectedTaskDescriptors()
        sink.lazy(Location.DATA_KEY) { selectedTaskLocation() }
    }

    /**
     * Die Selektion als gewoehnliche [PsiLocation] auf das Verzeichnis des besitzenden Moduls.
     *
     * Der Inhalt der Location interessiert niemanden - was der Baum ausgewaehlt hat, liest
     * [GradleTaskRunConfigurationProducer] aus [SELECTED_GRADLE_TASKS]. Gebraucht wird sie
     * trotzdem: `ConfigurationContext.createConfiguration` fragt ohne Location gar keine
     * Producer, und damit gaebe es im Kontextmenue weder Ausfuehrungsarten noch "Modify Run
     * Configuration". Frueher stand hier die passgenaue `ExternalSystemTaskLocation` - die ist
     * `@ApiStatus.Internal` und deshalb einem eigenen Producer gewichen (ADR-022).
     *
     * Bewusst ueber `DataSink.lazy`: Die Aufloesung schlaegt ein `PsiDirectory` nach und soll
     * nur laufen, wenn eine Aktion die Location wirklich anfragt.
     */
    private fun selectedTaskLocation(): Location<*>? {
        val settings = gradleTaskExecutionSettings(selectedTaskDescriptors()) ?: return null
        val directory = LocalFileSystem.getInstance().findFileByPath(settings.externalProjectPath)
            ?.let { PsiManager.getInstance(project).findDirectory(it) }
            ?: return null
        return PsiLocation(project, directory)
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
