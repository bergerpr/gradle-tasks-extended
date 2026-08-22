---
id: OPEN-005
titel: Modulgliederung als schaltbare Ansicht mit Exclude/Include
prioritaet: hoch
betrifft_anforderung:
  - REQ-001
  - REQ-003
  - REQ-004
  - REQ-013
  - REQ-041
  - REQ-070
  - REQ-071
  - REQ-072
betrifft:
  - src/main/kotlin/de/example/gradletasksview/model/GradleTaskDataProvider.kt
  - src/main/kotlin/de/example/gradletasksview/model/GradleTaskDescriptor.kt
  - src/main/kotlin/de/example/gradletasksview/model/TaskTreeBuilder.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/GradleTasksViewConfigStore.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/TaskViewConfigYaml.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTreeStateManager.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTreeModelBuilder.kt
  - src/main/kotlin/de/example/gradletasksview/execution/GradleTaskExecutor.kt
  - src/main/kotlin/de/example/gradletasksview/actions/GradleTaskViewModeActionGroup.kt
  - src/main/kotlin/de/example/gradletasksview/toolwindow/GradleTasksExtendedPanel.kt
quelle: Analyse auf Nachfrage nach einer Multi-Projekt-Anpassung
---

Ansicht und Konfiguration sind durchgaengig auf einen **globalen** Namensraum aus Gruppen und
Tasknamen ausgelegt. In einem Gradle-Build mit mehreren Subprojekten - und erst recht in einem
Composite Build - fehlt damit ueberall die Dimension "zu welchem Modul gehoert das".

> Gelesen als Multi-Projekt-/Multi-Modul-Build inklusive Composite Builds. Falls stattdessen
> Kotlin Multiplatform im engeren Sinne gemeint war (Targets und Source-Sets innerhalb *eines*
> Moduls), ist die Analyse unten nur teilweise einschlaegig - der Punkt zur Baumform bliebe,
> die Punkte zu Modulidentitaet und Ausfuehrung entfielen.

## Wo die Modul-Dimension heute verloren geht

1. **Datenbeschaffung.** `loadTasks()` ruft
   `ExternalSystemApiUtil.findAllRecursively(projectStructure, ProjectKeys.TASK)` auf der
   gesamten Projektstruktur auf und flacht damit alle Module in **eine** Liste ab. Der
   Elternknoten, der die Modulzugehoerigkeit traegt, wird nicht ausgewertet.

2. **Datenmodell.** `GradleTaskDescriptor` hat kein Modulfeld. `gradleProjectPath` ist ein
   Platzhalter (OPEN-001), `taskPath` ist mit `taskName` identisch (OPEN-002). Damit ist
   `stableId` bei gleichnamigen Tasks in zwei Modulen nicht eindeutig - der Selektionserhalt
   kann den falschen Knoten treffen.

3. **Baumaufbau.** `buildSortedGroups` und `buildNestedTree` gruppieren ausschliesslich ueber
   `groupName`. Zwei Module mit je einem Task `test` in der Gruppe `verification` erzeugen
   zwei optisch identische Geschwisterzeilen, die sich nicht auseinanderhalten lassen.

4. **Konfigurationsschluessel.** Der Schluesselraum in `default.yaml`/`custom.yaml` ist
   global: Gruppen-/Ordnerpfad fuer Container, Taskname fuer Tasks. Zwei Module mit einer
   Gruppe `build` teilen sich zwangslaeufig **einen** Expand-Eintrag; zwei Tasks `test` teilen
   sich **ein** Icon. Das ist heute bewusst so dokumentiert (REQ-071), waere in einem
   Multi-Modul-Build aber keine Vereinfachung mehr, sondern schlicht falsch.

5. **Ausfuehrung.** `GradleTaskExecutor` uebergibt den blanken `taskName` zusammen mit dem
   `externalProjectPath` der Projektwurzel. Gradle fuehrt einen so benannten Task dann in
   jedem passenden Projekt aus, nicht im gemeinten (OPEN-002).

6. **Filterung.** Der Speed-Search matcht nur auf `taskName`. Es gibt keine Moeglichkeit, auf
   ein Modul einzugrenzen - in einem grossen Build die naheliegendste Filterung ueberhaupt.

## Was eine Anpassung umfassen muesste

**A - Modulidentitaet beschaffen.** Zu jedem Task-`DataNode` laesst sich der Modulknoten ueber
`taskNode.getParent(ModuleData::class.java)` ermitteln. `ModuleData` bietet `getId()`,
`getModuleName()`, `getLinkedExternalProjectPath()` und `getIdeGrouping()`. Bei Composite
Builds liefert `getExternalProjectsData(...)` mehrere `ExternalProjectInfo`-Eintraege mit je
eigener Wurzel; deren Unterscheidung traegt bereits `externalProjectPath`.

**B - Datenmodell erweitern.** `GradleTaskDescriptor` um den echten `gradleProjectPath`
(`:sub:modul`) und einen Anzeigenamen des Moduls ergaenzen, `taskPath` auf den qualifizierten
Pfad `:sub:task` setzen. Damit werden OPEN-001 und OPEN-002 miterledigt und `stableId` wird
ohne weitere Aenderung eindeutig.

**C - Baumform.** Modulebene aussen, beginnend beim Wurzelprojekt, mit nachgebildeter
Projektstruktur - festgelegt, siehe "Entschieden / Baumform".

**D - Schluesselformat der Konfiguration.** Gruppenpfade eines Moduls verschachtelt unter
dem Modulblock, Schluessel ohne Modulbezug gelten weiter fuer alle Module - festgelegt, siehe
"Entschieden / Schluesselform mit Modulbezug". Damit behalten bestehende `default.yaml`-Dateien
ihre Wirkung, statt beim Umstieg wertlos zu werden; eine Migration scheidet aus, weil das
Plugin `default.yaml` nie umschreibt (REQ-070).

**E - Ausfuehrung.** `taskPath` statt `taskName` verwenden und den `externalProjectPath` des
Moduls statt den der Wurzel.

**F - Filterung.** Optional ein Modulfilter im Speed-Search, oder zumindest Matching gegen den
qualifizierten Taskpfad statt nur gegen den Namen.

**G - Tests.** Die Modelllogik-Tests brauchen Fixtures mit mehreren Modulen und bewusst
kollidierenden Task-/Gruppennamen; als Vorlage dafuer dient die Struktur von
`../gradle-tasks-extended-testproject`. Sinnvoll abdecken laesst sich das Zusammenspiel erst mit
den Integrationstests aus OPEN-003, weil dort ein Mock-`DataNode`-Baum mit echter
Modulstruktur aufgebaut werden kann.

## Geforderter Funktionsumfang

Ueber die reine Modulkenntnis hinaus ist Folgendes gefordert:

**H - Feststellbutton in der Toolbar.** Ein `ToggleAction` neben dem Modus-Dropdown schaltet
die Modulgliederung ein und aus. Er ist **orthogonal** zu den drei Modi: Es entstehen sechs
Kombinationen (Default/Sort/Nested jeweils mit und ohne Modulebene), nicht ein vierter Modus.

**I - Modulgliederung in allen drei Modi.** Ist der Button aktiv, wird jede Ansicht zusaetzlich
nach Modulen unterteilt: In `Default` bleibt innerhalb eines Moduls die Originalreihenfolge, in
`Sort` wird innerhalb sortiert, in `Nested` haengt der Ordnerbaum unter dem Modulknoten.

**J - Module aus einzelnen Ansichten ausschliessen.** Die Ausschlussliste ist **pro Modus**
konfigurierbar, nicht nur global - ein Modul kann also in `Nested` sichtbar und in `Sort`
ausgeblendet sein.

**K - Gruppenpfade trotz Ausschluss einblenden.** Zu einem ausgeschlossenen Modul lassen sich
einzelne Gruppenpfade gezielt wieder einblenden. Das Modul erscheint dann wieder, enthaelt aber
nur die eingeschlossenen Pfade.

**L - Konfigurierbar in `default.yaml`.** Der Zustand des Feststellbuttons sowie die
Exclude-/Include-Listen gehoeren in die bestehende zweistufige Konfiguration: `default.yaml`
als Team-Vorgabe, `custom.yaml` als persoenliche Abweichung, die der Button schreibt.

## Entschieden

Die folgenden Punkte waren offen und sind gemeinsam festgelegt worden. Sie sind damit
Vorgabe fuer die Umsetzung, nicht mehr Diskussionsgrundlage.

### Baumform

Die Modulebene sitzt **aussen**, die Gruppen liegen darunter. Sie beginnt beim
**Wurzelprojekt** - so wie die originale Gradle-Ansicht -, dessen eigene Tasks also unter
einem eigenen Knoten erscheinen und nicht verschwinden. Darunter folgen die Subprojekte,
darunter deren Gruppen und Tasks.

Tiefer verschachtelte Subprojekte bilden die Gradle-Projektstruktur nach: `:app:core`
erscheint unter `:app`, nicht als dessen Geschwister. Zwischenknoten ohne eigene Tasks
muessen dabei dargestellt werden, damit die Struktur nicht zerreisst.

```text
:                       (Wurzelprojekt)
|--- assemble
|--- :app
|    |--- docker/gui
|    |    |--- build-all-gui
|    |--- :app:core
|         |--- verification
|              |--- test
|--- :lib
```

### Ansichtseinstellungen in der YAML

Ein **reservierter Schluessel auf oberster Ebene** traegt die Ansichtseinstellungen; die
Knoteneintraege bleiben daneben flach wie bisher. Bestehende Dateien funktionieren dadurch
unveraendert weiter, eine Migration entfaellt - was noetig ist, weil das Plugin `default.yaml`
bewusst nie umschreibt.

Der Schluessel traegt den Namen des Plugins statt eines generischen Worts wie `view`, damit
eine Kollision mit einer echten Gradle-Gruppe praktisch ausgeschlossen ist. Vorgeschlagener
Name: **`gradle_tasks_extended`** - identisch zum Namen des Konfigurationsverzeichnisses.
Diese Schreibweise ist festgelegt.

### Schluesselform mit Modulbezug

Gruppenpfade eines bestimmten Moduls werden **unter dem Modul verschachtelt**. Ein Schluessel
auf oberster Ebene ohne Modulbezug gilt weiterhin fuer **alle** Module.

Zur Unterscheidung dient ein Merkmal, das sich aus Gradle selbst ergibt: **Projektpfade
beginnen immer mit `:`**. Ein Schluessel `:app` ist damit zweifelsfrei ein Modulblock,
`docker/gui/jsp` ein modulunabhaengiger Knoteneintrag. Das Wurzelprojekt heisst konsequent
`":"`.

Innerhalb eines Modulblocks konfigurieren die reservierten Schluessel `expanded` und `icon`
den **Modulknoten selbst**; jeder andere Schluessel ist ein Gruppenpfad innerhalb dieses
Moduls. Modulknoten sind damit ebenso konfigurierbar wie Gruppen.

### Vorrangregeln

- Ohne Eintrag ist alles eingeschlossen.
- `exclude` blendet aus, `include` blendet gezielt wieder ein.
- Bei Ueberschneidung gewinnt der laengere, spezifischere Pfad.
- Bei **gleicher** Spezifitaet gewinnt `include`. Zu viel anzuzeigen ist harmloser, als
  jemandem stillschweigend Tasks vorzuenthalten - ein fehlender Task faellt oft erst auf,
  wenn man ihn sucht.
- Listen aus `custom.yaml` **ergaenzen** die aus `default.yaml`, sie ersetzen sie nicht.
  `include` ist das Gegenmittel, um trotz eines Team-Exclude etwas zu sehen. So koennen
  Team-Vorgaben nicht unbemerkt verschwinden.
- Modusabhaengige Listen ergaenzen die globalen ebenfalls; bei gleicher Spezifitaet gewinnt
  der Moduseintrag.

### Wirkung und Sichtbarkeit

- Modul-Excludes greifen **immer**, auch bei ausgeschalteter Modulgliederung. Der
  Feststellbutton aendert dann ausschliesslich die Darstellung, nicht den Inhalt - zwei
  unabhaengige Begriffe, die sich getrennt erklaeren lassen.
- Der Speed-Search findet **keine** Tasks aus ausgeschlossenen Modulen: Der Ausschluss wirkt
  vor der Filterung. Sonst widerspraeche ein Treffer dem sichtbaren Baum.
- `showModules` ist standardmaessig **aus**. Fuer bestehende Projekte ohne Konfiguration
  aendert sich dadurch nichts.

### Formatskizze

```yaml
gradle_tasks_extended:
  showModules: true               # Vorgabe des Feststellbuttons
  exclude:
    - ":legacy"                   # ganzes Modul ausblenden
  include:
    ":legacy":                    # ... bis auf diese Gruppenpfade
      - docker/gui
  modes:
    sort:
      exclude:
        - ":sandbox"              # nur im Sort-Modus zusaetzlich ausblenden

docker/gui/jsp:                   # gilt fuer alle Module, wie bisher
  icon: smile.svg

":app":                           # fuehrender Doppelpunkt = Modulblock
  expanded: false                 # Konfiguration des Modulknotens selbst
  icon: app.svg
  docker/gui/jsp:                 # Gruppenpfad nur innerhalb von :app
    expanded: true
```

### Zwei Korrekturen an der urspruenglichen Skizze

Beim Ausformulieren fielen zwei Mehrdeutigkeiten auf, die vorher nicht sichtbar waren:

- **`include` als flache Liste war mehrdeutig.** Ein Eintrag `":legacy:docker/gui"` laesst
  sich nicht eindeutig zerlegen: `:legacy` mit Gruppe `docker/gui` oder Modul `:legacy:docker`
  mit Gruppe `gui`? Beides sind gueltige Gradle-Pfade. `include` ist deshalb eine **Abbildung
  Modul -> Liste von Gruppenpfaden**. Das passt auch inhaltlich, denn `include` ist immer eine
  Ausnahme zu einem ausgeschlossenen Modul.
- **Modulschluessel bleiben absolut.** Auch wenn die *Anzeige* verschachtelt ist, steht
  `":app:core"` als eigener Schluessel auf oberster Ebene und nicht innerhalb des
  `":app"`-Blocks. Sonst waere innerhalb eines Modulblocks nicht mehr unterscheidbar, ob ein
  Unterschluessel ein Untermodul oder ein Gruppenpfad ist. Anzeigehierarchie und
  Konfigurationsschluessel sind damit bewusst entkoppelt.

### Folgen fuer bestehende Entscheidungen

ADR-003 haelt fest, dass die Konfiguration bewusst fuer alle drei Modi gleichermassen gilt.
Das trifft ab jetzt nur noch auf den Expand-Zustand und die Icons zu - Exclude und Include
sind modusabhaengig. ADR-003 ist entsprechend zu ergaenzen, sobald umgesetzt wird; die
Abweichung soll nicht stillschweigend entstehen.

## Weitere Auswirkungen

- Leere Container: Nach dem Ausschliessen koennen Gruppen und Ordner ohne Tasks
  zurueckbleiben; sie muessen weggelassen werden, sonst entstehen leere Aeste.
- Der "Reset to Default"-Button (REQ-072) loescht `custom.yaml` und setzt damit automatisch
  auch den Feststellbutton auf die Team-Vorgabe zurueck - das ist konsistent, sollte aber
  bewusst so benannt sein.
- Die Vorrangregeln sind reine Modelllogik und damit ohne Platform-Testframework testbar.
  Genau dort lohnen Unit-Tests am meisten, weil sich Fehler sonst als "ein Task fehlt" aeussern
  und schwer zu bemerken sind.

## Arbeitsplan

Fuenf Arbeitspakete, aufsteigend abhaengig. Jedes ist fuer sich abschliessbar und
hinterlaesst einen lauffaehigen Zustand. Verifiziert wird durchgaengig gegen
`../gradle-tasks-extended-testproject` - dessen Struktur ist genau auf diese Faelle
zugeschnitten, siehe dessen `README.md`.

### AP-1: Modulidentitaet im Datenmodell

Erledigt zugleich OPEN-001 und OPEN-002 und aendert die Ansicht noch nicht.

- `GradleTaskDataProvider`: je Task ueber `taskNode.getParent(ModuleData::class.java)` den
  Modulknoten ermitteln; `ModuleData.getId()` als Gradle-Projektpfad, `getModuleName()` als
  Anzeigename, `getLinkedExternalProjectPath()` als Modulverzeichnis.
- `GradleTaskDescriptor`: `gradleProjectPath` echt befuellen, `taskPath` auf `:sub:task`
  setzen, Anzeigename des Moduls ergaenzen.
- `GradleTaskExecutor`: `taskPath` statt `taskName` und den `externalProjectPath` des Moduls.

**Fertig, wenn:** `stableId` bei `buildImage` in `:app` und `:lib` unterschiedlich ist, und ein
Doppelklick auf `buildImage` in `:app` genau **eine** Ausgabezeile erzeugt (`Projekt=:app`),
nicht zwei. Die Gegenprobe auf der Kommandozeile steht in der README des Testprojekts.

**Katalog danach:** `backlog/OPEN-001` und `backlog/OPEN-002` entfernen; in REQ-012, REQ-014
und REQ-024 die jeweiligen `offene_punkte`-Eintraege loeschen und die Texte auf den neuen
Zustand bringen.

### AP-2: Leseseite des Konfigurationsformats

Reine Parser-Arbeit, noch ohne Wirkung auf die Ansicht.

- `TaskViewConfigYaml`: reservierten Schluessel `gradle_tasks_extended` mit `showModules`,
  `exclude`, `include` und `modes` lesen; Modulbloecke an Schluesseln mit fuehrendem `:`
  erkennen; innerhalb eines Modulblocks `expanded`/`icon` als Konfiguration des Modulknotens
  behandeln, jeden anderen Schluessel als Gruppenpfad dieses Moduls.
- Neue Modelltypen fuer Ansichtseinstellungen und Filterlisten; `TaskViewConfig` entsprechend
  erweitern.
- `GradleTasksViewConfigStore`: Aufloesung `custom.yaml` > `default.yaml` auch fuer die neuen
  Felder, Listen dabei **ergaenzend** (siehe "Vorrangregeln").

**Fertig, wenn:** `TaskViewConfigYamlTest` deckt beide Formen ab und eine bestehende flache
Datei liefert unveraendert dasselbe Ergebnis wie vorher. Der auskommentierte Zielblock in
`../gradle-tasks-extended-testproject/gradle/gradle_tasks_extended/default.yaml` laesst sich
aktivieren und parst.

### AP-3: Modulebene im Baum und Feststellbutton

- `TaskTreeBuilder`: Modulhierarchie aufbauen - Wurzelprojekt als oberster Knoten mit eigenen
  Tasks, Subprojekte darunter, verschachtelt entsprechend Gradle-Projektstruktur.
  Zwischenknoten ohne eigene Tasks muessen erscheinen.
- `GradleTasksTreeModelBuilder` und `GradleTasksTreeCellRenderer`: Modulknoten darstellen.
- Neue Action `GradleTaskShowModulesToggleAction` in `actions/`, in
  `GradleTasksExtendedPanel` neben dem Modus-Dropdown eingehaengt; Zustand ueber den
  ConfigStore in `custom.yaml`, Vorgabe aus `default.yaml`, Standard **aus**.
- `GradleTasksTreeStateManager`: Modulknoten bekommen einen Konfigurationsschluessel.

**Fertig, wenn:** Alle drei Modi jeweils mit und ohne Modulebene korrekt aussehen, `:app:core`
unter `:app` steht und die Wurzelprojekt-Tasks sichtbar sind. Visuell gegen das Testprojekt
pruefen (Screenshot).

### AP-4: Exclude und Include

- Filterung als reine Modelllogik **vor** dem Baumaufbau, damit leere Gruppen und Ordner gar
  nicht erst entstehen.
- Vorrangregeln exakt nach dem Abschnitt "Vorrangregeln" umsetzen.

**Unit-Tests, die es dafuer braucht** (reine Modelllogik, kein Platform-Testframework):

| Fall | Erwartung |
|---|---|
| kein Eintrag | alles sichtbar |
| `exclude: [":legacy"]` | `:legacy` samt Untermodulen weg |
| dazu `include: {":legacy": ["docker/gui"]}` | `:legacy` wieder da, aber nur mit `docker/gui` |
| exclude `:app`, include `:app:core` | laengerer Pfad gewinnt, `:app:core` bleibt |
| exclude und include auf demselben Pfad | `include` gewinnt |
| Liste nur in `custom.yaml` | ergaenzt die aus `default.yaml`, ersetzt sie nicht |
| `modes.sort.exclude` | wirkt nur im Sort-Modus |
| Exclude bei `showModules: false` | wirkt trotzdem |

**Fertig, wenn:** Alle Faelle gruen und im Testprojekt zeigt sich `:legacy` nur noch mit
`docker/gui`.

**Katalog danach:** ADR-003 ergaenzen - Expand-Zustand und Icons bleiben modusuebergreifend,
Exclude und Include sind es nicht.

### AP-5: Suche nachziehen und abschliessen

- `GradleTaskViewRefresher`: Ausschluss wirkt vor der Textfilterung.
- Speed-Search optional gegen den qualifizierten Taskpfad matchen lassen.

**Fertig, wenn:** Ein ausgeschlossenes Modul liefert auch bei aktiver Suche keine Treffer.

## Abschluss im Katalog

Wenn alle fuenf Pakete durch sind, ist dieser Eintrag aufzuloesen - er beschreibt dann
umgesetzten Zustand und gehoert damit nach `docs/`:

- Neue Anforderungen in `docs/requirements/`: Modulgliederung mit Feststellbutton
  (H, I), Exclude/Include mit Vorrangregeln (J, K), Konfiguration der Ansichtseinstellungen
  (L). IDs fortlaufend ab REQ-074 vergeben, `verifiziert`-Feld wahrheitsgemaess setzen.
- `betrifft`-Listen der neuen Eintraege auf die tatsaechlich beteiligten Dateien setzen.
- In REQ-003, REQ-004, REQ-013, REQ-041, REQ-070, REQ-071 die `offene_punkte: OPEN-005`
  entfernen und die Texte auf den neuen Zustand bringen.
- ADR-003 um die Modusabhaengigkeit ergaenzen; einen neuen ADR fuer die Formatentscheidungen
  anlegen (reservierter Schluessel, `:`-Praefix als Unterscheidungsmerkmal, absolute
  Modulschluessel trotz verschachtelter Anzeige) - die Begruendungen dafuer stehen oben unter
  "Entschieden" und "Zwei Korrekturen an der urspruenglichen Skizze".
- `backlog/OPEN-005` loeschen.
- Den auskommentierten Zielblock in der `default.yaml` des Testprojekts aktivieren.

Composite Builds bleiben auch danach ungeprueft; das Testprojekt deckt sie nicht ab
(OPEN-004).
