---
id: REQ-010
titel: Doppelklick fuehrt einen Task aus
kategorie: interaktion
verifiziert: manuell
betrifft:
  - src/main/kotlin/de/example/gradletasksview/execution/GradleTaskExecutor.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTree.kt
quelle: Plan.md, Abschnitt "Task-Ausfuehrung (Doppelklick)"
---

Ein Doppelklick auf einen Task-Knoten fuehrt ihn ueber die offizielle External-System-API
aus - niemals ueber eigene Prozess- oder Kommandozeilenaufrufe.

Verwendet wird ausschliesslich die `TaskExecutionSpec`-Ueberladung von
`ExternalSystemUtil.runTask`, siehe ADR-006.
