---
id: REQ-042
titel: Mehrere offene Projekte haben unabhaengigen Zustand
kategorie: persistenz
verifiziert: konstruktiv
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/persistence/GradleTaskViewSettings.kt
  - src/main/kotlin/de/berger/gradletasksview/persistence/GradleTasksViewConfigStore.kt
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTaskViewRefresher.kt
offene_punkte:
  - OPEN-004
quelle: Plan.md, Abschnitt "Edge Cases"
---

Alle zustandstragenden Dienste sind `Service.Level.PROJECT`; jedes Projektfenster hat eine
eigene Instanz. Ein Application-Level-Singleton mit projektuebergreifendem Zustand ist
ausdruecklich unzulaessig.

Die Konfigurationsdateien liegen unterhalb der jeweiligen Projektwurzel und sind damit
ebenfalls projektgebunden.
