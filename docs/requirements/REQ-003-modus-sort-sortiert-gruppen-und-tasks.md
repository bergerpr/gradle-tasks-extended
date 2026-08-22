---
id: REQ-003
titel: Modus Sort sortiert Gruppen und Tasks
kategorie: modi
verifiziert: unit
betrifft:
  - src/main/kotlin/de/example/gradletasksview/model/TaskTreeBuilder.kt
  - src/main/kotlin/de/example/gradletasksview/model/TaskTreeSorting.kt
quelle: Plan.md, Abschnitt "Zielverhalten / Sort"
---

Im Modus `Sort` werden die Gruppen nach ihrem vollstaendigen Gruppenpfad sortiert und die
Tasks innerhalb jeder Gruppe nach ihrem Namen.

Die Liste bleibt flach: `docker/gui` und `docker/gui/jsp` sind zwei getrennte Gruppenzeilen,
es entstehen keine Ordnerknoten. `Sort` ist damit der einzige Modus ohne Ordnerbaum - in
`Default` und `Nested` werden dieselben Pfade verschachtelt dargestellt (REQ-004, REQ-077).

Ist die Modulgliederung eingeschaltet (REQ-074), gilt das je Modul: Die sortierte Gruppenliste
haengt dann unter dem jeweiligen Modulknoten, sodass sich `docker/gui` in `:app` und in `:lib`
nicht mehr zu einer Zeile vermischen.
