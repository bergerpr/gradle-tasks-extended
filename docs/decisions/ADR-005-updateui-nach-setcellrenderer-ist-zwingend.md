---
id: ADR-005
titel: updateUI() nach setCellRenderer ist zwingend
betrifft:
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTree.kt
quelle: Diagnose an der laufenden Sandbox
---

`GradleTasksTree` ruft direkt nach `cellRenderer = GradleTasksTreeCellRenderer(...)` einmal
`updateUI()` auf. Diese Zeile ist kein Aufraeumen, sondern Voraussetzung dafuer, dass der
eigene Renderer ueberhaupt zum Zeichnen verwendet wird.

`com.intellij.ui.treeStructure.Tree` ruft bereits in seinem eigenen Konstruktor
`setCellRenderer(new NodeRenderer())` auf. Ein spaeteres `setCellRenderer(...)` aus der
Unterklasse aktualisiert zwar `JTree.getCellRenderer()`, aber **nicht** das Feld
`currentCellRenderer` in `javax.swing.plaf.basic.BasicTreeUI` - und genau dieses liest
`com.intellij.ui.tree.ui.DefaultTreeUI.getRenderer(...)` beim Zeichnen jeder Zeile.

Nachgewiesen per Reflection in einer laufenden Sandbox:

```text
vor  setCellRenderer(...):  ui.currentCellRenderer = NodeRenderer
nach setCellRenderer(...):  ui.currentCellRenderer = NodeRenderer          <- Ursache
nach updateUI():            ui.currentCellRenderer = GradleTasksTreeCellRenderer
```

Ohne den Aufruf zeichnet der Baum dauerhaft mit `NodeRenderer`, dessen Ausgabe fuer eigene
`DefaultMutableTreeNode`-Nutzdaten genau `toString()` ohne Icon ist. Sichtbare Symptome waren
fehlende Icons, fehlende Fettschrift bei Gruppen und Ordnern und fehlende Task-Beschreibungen.

Das erklaert rueckwirkend eine lange erfolglose Fehlersuche: Icon-Objekt, Icon-Groesse,
Zeilenhoehe, Theme, Repaint und Plattformversion waren alle unschuldig - der Renderer, in dem
das Icon gesetzt wurde, lief schlicht nie.
