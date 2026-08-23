---
id: REQ-032
titel: Refresh-Ausloeser
kategorie: plattform
verifiziert: visuell
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTaskViewRefresher.kt
  - src/main/kotlin/de/berger/gradletasksview/toolwindow/GradleTasksExtendedStartupActivity.kt
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTree.kt
quelle: Plan.md, Abschnitt "Refresh-Strategie / Trigger"
---

Die Ansicht baut sich neu auf bei: Gradle-Reimport (`ProjectDataImportListener`), Aenderung
des Filtertexts, Moduswechsel, erstmaligem Sichtbarwerden des Baums und beim Projektstart.

Der allererste Refresh wird zusaetzlich mit steigendem Abstand wiederholt (500 ms / 1 s / 2 s
/ 4 s), solange keine Tasks gefunden werden. Ein beim Projektstart wiederhergestelltes,
bereits geoeffnetes ToolWindow kann sichtbar werden, bevor die zwischengespeicherten
Projektdaten verfuegbar sind, und es existiert kein verlaessliches "Daten bereit"-Event fuer
diesen Fall.
