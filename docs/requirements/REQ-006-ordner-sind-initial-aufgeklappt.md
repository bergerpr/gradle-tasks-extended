---
id: REQ-006
titel: Ordner sind initial aufgeklappt
kategorie: darstellung
verifiziert: visuell
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTreeStateManager.kt
  - src/main/kotlin/de/berger/gradletasksview/persistence/GradleTasksViewConfigStore.kt
quelle: Plan.md, Abschnitt "Zielverhalten / Nested"
---

Ein Ordner bzw. eine Gruppe, fuer die keine Konfiguration vorliegt, wird aufgeklappt
dargestellt. `GradleTasksViewConfigStore.isExpanded` liefert dafuer `true` als Standardwert.

Der Abstieg in Kindknoten erfolgt nur, wenn der Elternknoten selbst aufgeklappt wird - ein
unbedingter Abstieg wuerde `JTree` dazu bringen, zugeklappte Vorfahren mit aufzuklappen,
damit der Kindpfad ueberhaupt darstellbar ist.
