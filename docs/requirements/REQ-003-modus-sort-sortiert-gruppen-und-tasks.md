---
id: REQ-003
titel: Modus Sort sortiert Gruppen und Tasks
kategorie: modi
verifiziert: unit
betrifft:
  - src/main/kotlin/de/example/gradletasksview/model/TaskTreeBuilder.kt
  - src/main/kotlin/de/example/gradletasksview/model/TaskTreeSorting.kt
offene_punkte:
  - OPEN-005
quelle: Plan.md, Abschnitt "Zielverhalten / Sort"
---

Im Modus `Sort` werden die Gruppen nach ihrem vollstaendigen Gruppenpfad sortiert und die
Tasks innerhalb jeder Gruppe nach ihrem Namen.

Die Liste bleibt flach: `docker/gui` und `docker/gui/jsp` sind zwei getrennte Gruppenzeilen,
es entstehen keine Ordnerknoten. Nur die Reihenfolge aendert sich gegenueber `Default`.
