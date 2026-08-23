package de.berger.gradletasksview.ui

import de.berger.gradletasksview.persistence.GradleTasksViewConfigStore
import de.berger.gradletasksview.util.childNodes
import de.berger.gradletasksview.util.findFirstOrNull
import de.berger.gradletasksview.util.toTreePath
import javax.swing.JTree
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.tree.DefaultMutableTreeNode

/**
 * Wendet den dateibasierten Expand/Collapse-Zustand ([GradleTasksViewConfigStore]:
 * `default.yaml` + `custom.yaml`) auf Modul-, Gruppen- und Ordnerknoten an und haelt ihn bei
 * Benutzerinteraktion aktuell - in allen drei Modi (Default/Sort/Nested), nicht nur
 * im Nested-Modus. Als Schluessel dient der volle Gruppen-/Ordnerpfad (z. B.
 * `docker/gui/jsp`) bzw. der Gradle-Projektpfad (`:app`), derselbe von Menschen lesbare Pfad
 * wie in `default.yaml`/`custom.yaml`.
 *
 * Ist die Modulgliederung eingeschaltet - in `Default` immer, siehe REQ-077 -, wird der Zustand
 * eines Gruppen-/Ordnerknotens unter dem Modul abgelegt, zu dem er gehoert; erst damit kann
 * dieselbe Gruppe in zwei Modulen unterschiedlich aufgeklappt sein (siehe REQ-076).
 *
 * Der `Tasks`-Knoten des `Default`-Modus hat bewusst keinen Schluessel und damit keinen eigenen
 * Zustand: [configKeyOf] liefert `null`, er gilt also als aufgeklappt und wird nur dann
 * ueberhaupt erreicht, wenn sein Modul aufgeklappt ist.
 */
class GradleTasksTreeStateManager(
    private val tree: JTree,
    private val configStore: GradleTasksViewConfigStore
) {

    private var listening = false

    fun applyPersistedExpansionState() {
        val root = tree.model.root as? DefaultMutableTreeNode ?: return
        applyExpansionRecursively(root)
    }

    /**
     * Steigt bewusst NUR in die Kinder eines Knotens ab, wenn dieser selbst
     * aufgeklappt wird. Ein unbedingter Abstieg (frueher via `forEachNodeRecursively`)
     * rief `tree.expandPath()` auch fuer Kinder eines eigentlich zugeklappt
     * bleibenden Ordners auf - das kann `JTree` dazu bringen, den zugeklappten
     * Vorfahren mit aufzuklappen, um den Kindpfad ueberhaupt darstellen zu koennen.
     */
    private fun applyExpansionRecursively(node: DefaultMutableTreeNode) {
        val shouldExpand = configKeyOf(node)?.let { configStore.isExpanded(it) } ?: true

        if (!shouldExpand) return

        tree.expandPath(node.toTreePath())
        node.childNodes().forEach { applyExpansionRecursively(it) }
    }

    /** Muss nach jedem Rebuild neu aufgerufen werden, da der `TreeModel` ausgetauscht wird. */
    fun startListeningForUserExpansionChanges() {
        if (listening) return
        listening = true
        tree.addTreeExpansionListener(object : TreeExpansionListener {
            override fun treeExpanded(event: TreeExpansionEvent) = onExpansionChanged(event, expanded = true)
            override fun treeCollapsed(event: TreeExpansionEvent) = onExpansionChanged(event, expanded = false)
        })
    }

    private fun onExpansionChanged(event: TreeExpansionEvent, expanded: Boolean) {
        val node = event.path.lastPathComponent as? DefaultMutableTreeNode ?: return
        val key = configKeyOf(node) ?: return
        configStore.setExpanded(key, expanded)
    }

    fun captureSelectedStableId(): String? {
        val node = tree.selectionPath?.lastPathComponent as? DefaultMutableTreeNode ?: return null
        return (node.userObject as? TaskNodeData)?.descriptor?.stableId
    }

    fun restoreSelection(stableId: String?) {
        if (stableId == null) return
        val root = tree.model.root as? DefaultMutableTreeNode ?: return
        val match = root.findFirstOrNull { node ->
            (node.userObject as? TaskNodeData)?.descriptor?.stableId == stableId
        } ?: return
        val path = match.toTreePath()
        tree.selectionPath = path
        tree.scrollPathToVisible(path)
    }
}
