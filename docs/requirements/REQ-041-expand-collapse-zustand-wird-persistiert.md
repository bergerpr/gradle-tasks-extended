---
id: REQ-041
titel: Expand/Collapse-Zustand wird persistiert
kategorie: persistenz
verifiziert: visuell
betrifft:
  - src/main/kotlin/de/example/gradletasksview/persistence/GradleTasksViewConfigStore.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/TaskViewConfigYaml.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTreeStateManager.kt
offene_punkte:
  - OPEN-005
quelle: Plan.md, Abschnitt "Persistenzregeln"
---

Der Auf-/Zuklappzustand ueberlebt einen IDE-Neustart, projekt- und pfadbezogen.

Umgesetzt ist das nicht wie urspruenglich skizziert ueber Mengen im
`PersistentStateComponent`, sondern ueber zwei YAML-Dateien im Projekt - siehe ADR-003.
