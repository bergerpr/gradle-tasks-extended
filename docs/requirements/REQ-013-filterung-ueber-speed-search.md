---
id: REQ-013
titel: Filterung ueber Speed-Search
kategorie: interaktion
verifiziert: visuell
betrifft:
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksSpeedSearch.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTaskViewRefresher.kt
quelle: Plan.md, Abschnitt "Filterung"
---

Die Ansicht filtert ueber `TreeSpeedSearch` auf dem Tasknamen, und zwar in allen drei Modi.

Die Filterung wirkt **datengetrieben vor** dem Aufbau von Sort/Nested: Der Refresher filtert
die Taskliste und baut den Baum daraus neu. Wuerde stattdessen nur der Speed-Search-Cursor
bewegt, blieben ausgefilterte Tasks durch die Verschachtelung sichtbar.

`TreeSpeedSearch` feuert sein Change-Event auch dann, wenn sich der Text nicht geaendert hat
(`PropertyChangeSupport` unterdrueckt nur bei nicht-null `oldValue`). Ein Rebuild wird daher
erst nach einem echten Textwechsel ausgeloest - sonst wuerde jeder manuelle
Expand/Collapse-Zustand vom naechsten Phantom-Refresh zurueckgesetzt.

## Einbindung

`TreeSpeedSearch.installOn(tree, false) { treePath -> ... }` wird direkt auf dem eigenen
`JTree` installiert. Die Ursachenanalyse hinter der Textwechsel-Pruefung steht in ADR-012.
