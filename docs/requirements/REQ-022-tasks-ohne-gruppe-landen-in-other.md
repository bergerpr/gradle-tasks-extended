---
id: REQ-022
titel: Tasks ohne Gruppe landen in Other
kategorie: daten
verifiziert: unit
betrifft:
  - src/main/kotlin/de/example/gradletasksview/util/GradleTaskPathUtil.kt
  - src/main/kotlin/de/example/gradletasksview/model/TaskTreeSorting.kt
  - src/main/kotlin/de/example/gradletasksview/model/GradleTaskDescriptor.kt
quelle: Plan.md, "Zu klaerende Produktdetails"
---

Tasks ohne oder mit leerer Gradle-Gruppe werden der Gruppe `Other` zugeordnet.

`Other` wird immer als letzter Eintrag einsortiert, unabhaengig von der alphabetischen
Reihenfolge der uebrigen Gruppen bzw. Ordner.
