---
id: REQ-020
titel: Keine eigene Gradle-Synchronisation
kategorie: daten
verifiziert: unit
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/model/GradleTaskDataProvider.kt
quelle: Plan.md, Abschnitt "Datenbeschaffung (Weg B)"
---

Task-Daten stammen ausschliesslich aus bereits importierten External-System-Projektdaten
(`ProjectDataManager.getExternalProjectsData` und Traversierung der `DataNode`-Struktur mit
`ExternalSystemApiUtil.findAllRecursively` ueber `ProjectKeys.TASK`).

Das Plugin loest **niemals** selbst einen Gradle-Sync aus und implementiert keinen eigenen
Import.

## Verifizierte API

Gegen die `master`-Quellen von `JetBrains/intellij-community` geprueft (Stand August 2026):

- `ProjectDataManager.getInstance().getExternalProjectsData(project, GradleConstants.SYSTEM_ID)`
  liefert `Collection<ExternalProjectInfo>` fuer bereits importierte Gradle-Projekte, **ohne**
  einen Sync auszuloesen.
- `ExternalProjectInfo.getExternalProjectStructure()` liefert die Wurzel
  `DataNode<ProjectData>`.
- `ExternalSystemApiUtil.findAllRecursively(node, key)` durchsucht den gesamten Teilbaum -
  im Gegensatz zu `findAll`, das nur direkte Kinder betrachtet. Verwendet mit
  `ProjectKeys.TASK` (`Key<TaskData>`).
- `TaskData` stellt `getName()`, `getGroup()`, `getDescription()`,
  `getLinkedExternalProjectPath()` und `isInherited()` bereit.
