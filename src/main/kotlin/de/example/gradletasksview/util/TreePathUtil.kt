package de.example.gradletasksview.util

import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

/** Kleine Hilfsfunktionen rund um `javax.swing.tree`, geteilt zwischen den UI-Klassen. */

fun DefaultMutableTreeNode.toTreePath(): TreePath = TreePath(path)

fun DefaultMutableTreeNode.childNodes(): Sequence<DefaultMutableTreeNode> =
    children().asSequence().filterIsInstance<DefaultMutableTreeNode>()

fun DefaultMutableTreeNode.forEachNodeRecursively(action: (DefaultMutableTreeNode) -> Unit) {
    action(this)
    childNodes().forEach { it.forEachNodeRecursively(action) }
}

fun DefaultMutableTreeNode.findFirstOrNull(
    predicate: (DefaultMutableTreeNode) -> Boolean
): DefaultMutableTreeNode? {
    if (predicate(this)) return this
    childNodes().forEach { child -> child.findFirstOrNull(predicate)?.let { return it } }
    return null
}

@Suppress("UNCHECKED_CAST")
private fun java.util.Enumeration<*>.asSequence(): Sequence<Any> =
    (this as java.util.Enumeration<Any>).asIterator().asSequence()
