---
id: ADR-025
titel: Geerbte Sammelzeilen bleiben sichtbar und bekommen einen Umschalter
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/model/GradleTaskDataProvider.kt
  - src/main/kotlin/de/berger/gradletasksview/model/GradleTaskDescriptor.kt
  - src/main/kotlin/de/berger/gradletasksview/model/VirtualTaskNodes.kt
  - src/main/kotlin/de/berger/gradletasksview/persistence/GradleTasksViewConfigStore.kt
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTreeCellRenderer.kt
ersetzt_vorgabe: ADR-024, Regel 2 ("Geerbte Eintraege werden uebersprungen")
quelle: Fehlermeldung des Benutzers, Sitzung vom 2026-09-01
---

ADR-024 hat die geerbten Eintraege der Plattform als reine Duplikate eingestuft und
weggeworfen. Das war zur Haelfte richtig und in der Wirkung falsch: Sie sind keine Duplikate
der Ansicht, sondern **eigene Zeilen der originalen Gradle-Ansicht**, und sie sind in einem
Projekt, das seine Plugins zentral fuer alle Unterprojekte setzt, die einzigen Tasks auf
Wurzelebene. Nach ADR-024 stand die Modulebene dort leer.

## Wie die Plattform sie erzeugt (2026.2, aus den IDE-Jars gelesen)

- `GradleTaskModelBuilder` setzt fuer jeden Task `inherited = false`.
- `GradleProjectResolver.registerInheritedTaskModelsInParentTaskModel` kopiert anschliessend
  die Tasks jedes Kindprojekts **nach oben** in das Modell des Elternprojekts - aber nur die,
  deren Namen das Elternprojekt nicht selbst vergibt - und markiert die Kopie mit
  `inherited = true`. Das laeuft von unten nach oben, deshalb traegt das Wurzelprojekt am Ende
  alles.
- `CommonGradleProjectResolverExtension.getTaskName` gibt einer geerbten Kopie den **blanken**
  Namen (ein echter Task eines Untermoduls bekommt den qualifizierten - siehe ADR-024, Regel 1,
  die unveraendert gilt).

Ein eigener Task des Moduls ist also nie geerbt: Der Vorrang liegt beim Elternprojekt.

## Warum die originale Ansicht der Massstab ist

`TaskNode.isVisible()` blendet einen geerbten Task nur aus, wenn
`ExternalProjectsView.showInheritedTasks()` falsch ist, und `ExternalProjectsViewState`
initialisiert `showInheritedTasks` mit `true`. Abschaltbar ist es ueber
`ShowInheritedTasksAction` im Zahnradmenue. Die originale Ansicht zeigt diese Zeilen also
standardmaessig - und der `Default`-Modus soll sie nachbilden (REQ-077).

Deshalb wird nachgebaut, was es dort gibt: sichtbar per Vorgabe, abschaltbar ueber einen
Feststellbutton, dessen Zustand in `settings.showInheritedTasks` liegt (REQ-088).

## Was von ADR-024 bleibt

Sein Einwand gegen diese Zeilen war: Sie sehen aus wie ein gewoehnlicher Task, stossen aber
mehrere Builds an. Der bleibt berechtigt und wird jetzt anders beantwortet als durch Weglassen
- durch Kenntlichmachen: kursive Schrift plus Tooltip, der sagt, dass der Task nicht in diesem
Modul definiert ist und in jedem Unterprojekt laeuft, das ihn hat.

Die uebrigen Beobachtungen aus ADR-024 waren richtig und bleiben in Kraft: der qualifizierte
Name aus `TaskData.getName()`, die Reduktion auf den blanken Namen, und dass ein echter Task in
seinem eigenen Modul auszufuehren ist (REQ-010). Die dort beschriebenen Fehlerbilder entstanden
saemtlich aus dem **Namen**, nicht daraus, dass die geerbten Zeilen ueberhaupt da waren:
Sie hingen am Wurzelprojekt, trugen den blanken Namen und fielen deshalb mit gleichnamigen
Tasks anderer Module auf dieselbe `stableId`. Das ist behoben, seit die `stableId` den Modulpfad
enthaelt und der echte Task seinen qualifizierten Namen behaelt - unabhaengig davon, ob geerbte
Zeilen angezeigt werden.

## Verweise zeigen weiter nur auf echte Tasks

`execute` (REQ-085) loest ausdruecklich nicht auf geerbte Zeilen auf. Sonst erzeugte ein
blanker Verweis zusaetzlich zu der Zeile je Modul noch eine je Elternebene - und der Knoten
liefe dann als Sammelaufruf, obwohl der Eintrag wie ein einzelner Task aussieht. Wer das will,
schreibt einen blanken Verweis und bekommt eine Zeile je Modul, oder benutzt die geerbte Zeile
selbst.
