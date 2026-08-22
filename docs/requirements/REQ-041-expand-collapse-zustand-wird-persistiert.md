---
id: REQ-041
titel: Expand/Collapse-Zustand wird persistiert
kategorie: persistenz
verifiziert: visuell
betrifft:
  - src/main/kotlin/de/example/gradletasksview/persistence/GradleTasksViewConfigStore.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/TaskViewConfigYaml.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTreeStateManager.kt
quelle: Plan.md, Abschnitt "Persistenzregeln"
---

Der Auf-/Zuklappzustand ueberlebt einen IDE-Neustart, projekt- und pfadbezogen.

Umgesetzt ist das nicht wie urspruenglich skizziert ueber Mengen im
`PersistentStateComponent`, sondern ueber zwei YAML-Dateien im Projekt - siehe ADR-003.

Bei eingeschalteter Modulgliederung haben auch Modulknoten einen Zustand, und der Zustand
eines Gruppen-/Ordnerknotens wird unter seinem Modul abgelegt. Erst damit kann dieselbe Gruppe
in zwei Modulen unterschiedlich aufgeklappt sein (REQ-076). In `Default` ist die Modulebene
fest (REQ-077), dort wird also immer modulbezogen geschluesselt.

Keinen eigenen Zustand hat der `Tasks`-Strukturknoten des `Default`-Modus: Er folgt dem seines
Moduls (REQ-077).
