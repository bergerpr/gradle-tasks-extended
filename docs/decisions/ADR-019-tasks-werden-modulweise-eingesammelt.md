---
id: ADR-019
titel: Tasks werden modulweise eingesammelt
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/model/GradleTaskDataProvider.kt
ersetzt_vorgabe: ADR-015, Abschnitt "Nachschlag ueber den Elternknoten"
quelle: OPEN-004, manuelle Testsitzung vom 2026-08-23
---

Eingesammelt wird von oben nach unten: erst
`ExternalSystemApiUtil.findAllRecursively(projectStructure, ProjectKeys.MODULE)`, dann je
Modulknoten dessen direkte Task-Kinder ueber `ExternalSystemApiUtil.findAll(moduleNode,
ProjectKeys.TASK)`. Das Modul ist damit bekannt, waehrend der Descriptor entsteht.

Der bisherige Weg - alle Tasks rekursiv einsammeln und das Modul je Task ueber
`taskNode.getParent(ModuleData::class.java)` nachschlagen - lieferte in der Sandbox nicht
verlaesslich ein Ergebnis. Im Log derselben Sitzung erscheint derselbe Task innerhalb von
13 Sekunden zweimal mit unterschiedlicher Herkunft:

```text
:app:core::app:core:printCoreReact   Modul aufgeloest
:printCoreReact                      Modul nicht aufgeloest, Rueckfall auf ":"
```

Der zweite Fall ist der Rueckfall `?: ROOT_MODULE_PATH` aus ADR-015: Ohne Modul wird der Task
dem Wurzelprojekt zugeschlagen, laeuft dort ins Leere und wird ausserdem an der falschen
Stelle des Baums angezeigt. Ein stiller Rueckfall auf das Wurzelprojekt ist damit keine
"vertretbare Annahme" mehr, sondern eine Fehlerquelle - beim modulweisen Einsammeln kann er
gar nicht mehr eintreten.

## Warum der Nachschlag unzuverlaessig ist

`DataNode.getParent(Class)` liest das Feld `data` der Vorfahrenknoten direkt und prueft es mit
`Class.isInstance` (im Bytecode der Plattform nachgesehen). Es haengt damit daran, dass die
Kette `parent` intakt und jedes `data` gesetzt ist. `DataNode.clear(true)` haengt einen Knoten
aus genau dieser Kette aus, und ob ein Task-Knoten ueberhaupt unter einem `ModuleData`-Knoten
haengt, ist eine Annahme ueber fremde Datenstruktur - kein zugesichertes Verhalten. Die
Gegenrichtung, vom Modul zu seinen Tasks, braucht diese Annahme nicht.

## Folge

Task-Knoten, die nicht unter einem Modulknoten haengen, tauchen nicht mehr auf. Das ist
beabsichtigt: Zu ihnen ist kein Modul und damit kein ausfuehrbarer Taskpfad bekannt.
