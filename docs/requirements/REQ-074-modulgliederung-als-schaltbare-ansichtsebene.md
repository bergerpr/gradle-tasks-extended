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
quelle: OPEN-005, Abschnitte "Baumform" und "Geforderter Funktionsumfang"
---

Ein Feststellbutton in der Toolbar, neben dem Modus-Dropdown, schaltet eine Modulebene ein
und aus. Er ist **orthogonal** zu den drei Modi: Es gibt sechs Kombinationen
(Default/Sort/Nested jeweils mit und ohne Modulebene), keinen vierten Modus. Standard ist
**aus**, damit sich fuer bestehende Projekte ohne Konfiguration nichts aendert.

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
`Default` bleibt die Originalreihenfolge, in `Sort` wird sortiert, in `Nested` haengt der
Ordnerbaum unter dem Modulknoten. Geschwistermodule sind immer alphabetisch sortiert, auch in
`Default`: Die Modulebene ist Projektstruktur und keine von Gradle gelieferte Reihenfolge, die
sich bewahren liesse.

## Modulidentitaet

Jeder Task kennt seinen Gradle-Projektpfad (`:app:core`), den Anzeigenamen seines Moduls und
seinen qualifizierten Taskpfad (`:app:core:test`). Der Pfad stammt aus dem
Eltern-`ModuleData`-Knoten des Task-Knotens, siehe ADR-015; angezeigt wird er auch als
Beschriftung des Modulknotens, weil er zugleich dessen Konfigurationsschluessel ist (REQ-076).

Damit wird `stableId` (REQ-024) auch bei gleichnamigen Tasks in zwei Modulen eindeutig, und
die Ausfuehrung trifft genau das gemeinte Modul (REQ-010).
