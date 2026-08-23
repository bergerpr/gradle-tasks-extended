---
id: REQ-074
titel: Modulgliederung als schaltbare Ansichtsebene
kategorie: modi
verifiziert: unit
betrifft:
  - src/main/kotlin/de/example/gradletasksview/actions/GradleTaskShowModulesToggleAction.kt
  - src/main/kotlin/de/example/gradletasksview/model/GradleTaskDataProvider.kt
  - src/main/kotlin/de/example/gradletasksview/model/GradleTaskDescriptor.kt
  - src/main/kotlin/de/example/gradletasksview/model/ModuleTreeNode.kt
  - src/main/kotlin/de/example/gradletasksview/toolwindow/GradleTasksExtendedPanel.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTreeModelBuilder.kt
  - src/main/kotlin/de/example/gradletasksview/util/GradleModulePathUtil.kt
quelle: OPEN-005, Abschnitte "Baumform" und "Geforderter Funktionsumfang"; OPEN-007
---

Ein Feststellbutton in der Toolbar, neben dem Modus-Dropdown, schaltet eine Modulebene ein
und aus. Er ist **orthogonal** zu `Sort` und `Nested`, aber kein vierter Modus. Standard ist
**aus**, damit sich fuer bestehende Projekte ohne Konfiguration nichts aendert.

In `Default` wirkt er **nicht**: Dort ist die Modulebene fester Bestandteil der Ansicht
(REQ-077), und der Button wird ausgegraut und angehakt dargestellt. Es gibt also fuenf
Kombinationen, nicht sechs - `Sort` und `Nested` je mit und ohne Modulebene, dazu `Default`.
Der **gespeicherte** Zustand des Buttons bleibt in `Default` unberuehrt, sodass er beim
Zurueckwechseln nach `Sort`/`Nested` unveraendert weitergilt.

## Baumform

Die Modulebene sitzt aussen, die Gruppen liegen darunter, und sie beginnt beim Wurzelprojekt
`":"` - dessen eigene Tasks erscheinen also unter einem eigenen Knoten statt zu verschwinden.
Tiefer verschachtelte Subprojekte bilden die Gradle-Projektstruktur nach: `:app:core` steht
unter `:app`, nicht daneben. Zwischenmodule ohne eigene Tasks werden dargestellt, damit die
Struktur nicht zerreisst.

```text
:                       (Wurzelprojekt)
|--- build
|    |--- assembleAll
|--- :app
|    |--- docker/gui
|    |    |--- build-all-gui
|    |--- :app:core
|         |--- verification
|              |--- test
|--- :lib
```

Innerhalb eines Moduls kommt erst dessen eigener Inhalt, dann die Untermodule - dieselbe
Reihenfolge wie "Tasks vor Unterordnern" (REQ-004). Der Modus bestimmt nur diesen Inhalt: in
`Sort` die sortierte Gruppenliste, in `Nested` der Ordnerbaum, in `Default` ein `Tasks`-Knoten
mit dem Ordnerbaum darunter (REQ-077). Geschwistermodule sind in allen Modi alphabetisch
sortiert: Die Modulebene ist Projektstruktur und keine von Gradle gelieferte Reihenfolge.

## Modulidentitaet

Jeder Task kennt seinen Gradle-Projektpfad (`:app:core`), den Anzeigenamen seines Moduls und
seinen qualifizierten Taskpfad (`:app:core:test`). Der Pfad stammt aus dem `ModuleData`-Knoten,
unter dem der Task haengt - welche Eigenschaft ihn traegt, steht in ADR-015, wie eingesammelt
wird in ADR-019; angezeigt wird er auch als
Beschriftung des Modulknotens, weil er zugleich dessen Konfigurationsschluessel ist (REQ-076).

Damit wird `stableId` (REQ-024) auch bei gleichnamigen Tasks in zwei Modulen eindeutig, und
die Ausfuehrung trifft genau das gemeinte Modul (REQ-010).
