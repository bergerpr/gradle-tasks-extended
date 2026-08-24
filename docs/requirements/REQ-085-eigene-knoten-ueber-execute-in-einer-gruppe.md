---
id: REQ-085
titel: Eigene Knoten ueber execute in einer Gruppe
kategorie: darstellung
verifiziert: unit
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/model/VirtualTaskNodes.kt
  - src/main/kotlin/de/berger/gradletasksview/model/GradleTaskDescriptor.kt
  - src/main/kotlin/de/berger/gradletasksview/model/NodeVisibility.kt
  - src/main/kotlin/de/berger/gradletasksview/model/TaskTreeSorting.kt
  - src/main/kotlin/de/berger/gradletasksview/persistence/TaskViewConfig.kt
  - src/main/kotlin/de/berger/gradletasksview/persistence/TaskViewConfigKey.kt
  - src/main/kotlin/de/berger/gradletasksview/persistence/TaskViewConfigYaml.kt
  - src/main/kotlin/de/berger/gradletasksview/persistence/GradleTasksViewConfigStore.kt
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTreeNode.kt
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTreeCellRenderer.kt
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksSpeedSearch.kt
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTaskViewRefresher.kt
  - src/main/resources/help/configuration.html
  - src/main/resources/help/example-config.yaml
quelle: Wunsch des Benutzers
---

Ein Gruppeneintrag darf ein `tasks:` enthalten. Ein Eintrag darin, der `execute:` traegt, wird
zu einer **eigenen Zeile im Baum**, die einen vorhandenen Gradle-Task ausfuehrt. Es entsteht
dabei kein Gradle-Task, und weder der Name noch die Gruppe muessen vorher existieren:

```yaml
groups:
  ci/nightly:                       # gibt es sonst nirgends - entsteht hierdurch
    description: Was der Nachtlauf anfasst
    tasks:
      Alle Images bauen:            # kein Gradle-Task dieses Namens noetig
        execute: ":app:buildImage"
        icon: nightly.svg
```

Gruppen sind in dieser Ansicht keine eigenstaendigen Gebilde, sondern entstehen ausschliesslich
daraus, dass Knoten ihren Pfad tragen (`buildSortedGroups`/`buildNestedTree`). Ein solcher
Eintrag legt seinen Pfad deshalb von selbst an, mehrsegmentig samt Zwischenebenen und
einschliesslich der Ordnerzusammenfassung aus REQ-004. Umgekehrt verschwindet die Gruppe wieder,
sobald alles darin ausgeblendet ist.

## Aufloesung von `execute`

| Form | Bedeutung |
|---|---|
| `":app:buildImage"` | genau dieses Modul |
| `":buildImage"` | Wurzelprojekt |
| `"buildImage"` | jedes Modul, das einen so heissenden Task hat - bei eingeschalteter Modulgliederung also eine Zeile je Modul |

Massgeblich ist dabei der Modulpfad, unter dem **diese Ansicht** den Task fuehrt - also genau
das, was `Copy Task Path` im Kontextmenue liefert. Das ist der Pfad aus `settings.gradle.kts`:
`printLibGui` aus `lib/build.gradle.kts` ist `:lib:printLibGui`.

Steht der Eintrag in einem Modulblock (`modules: ":app": groups: ...`), schraenkt der
zusaetzlich auf dieses Modul ein. Zeigt der Verweis auf keinen vorhandenen Task, entsteht
**kein** Knoten und es wird eine Warnung protokolliert - wie bei einer fehlenden Icon-Datei.
Ein Knoten, der beim Doppelklick ins Leere liefe, waere schlimmer als ein fehlender.

Die Warnung nennt dabei die Pfade, unter denen ein Task dieses Namens tatsaechlich vorliegt
(`referenceCandidates`). Ohne sie steht im Log nur "nicht gefunden", und die naechste Frage -
"und wo ist er dann?" - bleibt offen; mit ihr ist ein falsch geschriebener Verweis in einer
Zeile geklaert. Solange noch gar keine Tasks geladen sind, wird nichts gemeldet: Beim ersten
Refresh ist die Projektdatenbank oft leer (REQ-031), und dann waere jeder Verweis unaufgeloest.

Ein Eintrag ohne `execute` definiert nichts, sondern konfiguriert nur einen anderswo
definierten Knoten. Genau diese Form schreibt `custom.yaml` beim Ausblenden, ohne die
Definition aus `default.yaml` zu wiederholen.

## Verhalten wie ein Task

`icon`, `description`, `hidden` und `modes` wirken wie bei jedem anderen Knoten; Doppelklick,
Run, Debug, Run with Coverage und "Modify Run Configuration" funktionieren unveraendert. Das
kostet keinen Sonderfall in der Ausfuehrung: Der erzeugte `GradleTaskDescriptor` traegt weiter
`taskName` und `externalProjectPath` des referenzierten Tasks - und genau daraus baut
`gradleTaskExecutionSettings` die Run-Konfiguration (ADR-018). Anders sind nur `displayName`
(die Beschriftung), `groupName` (die Einordnung) und `stableId`.

Drei Stellen folgen der Beschriftung statt dem Gradle-Namen: die Sortierung innerhalb einer
Gruppe, die Speed-Search und die Textfilterung. Sonst stuende ein Knoten an einer Stelle, die
zu seinem sichtbaren Namen nicht passt, und waere nur ueber den Task auffindbar, den er
ausfuehrt.

`Copy Task Name`/`Copy Task Path` liefern weiterhin den referenzierten Gradle-Task, nicht die
Beschriftung: Kopiert wird, was man in eine Kommandozeile einfuegen kann, und der frei gewaehlte
Name existiert dort nicht.

Beim Tooltip gilt: Ein echter Task zeigt weiterhin ausschliesslich seine Gradle-Beschreibung
(REQ-015, REQ-082). Ein selbst benannter Knoten zeigt seine eigene `description`, und ohne sie
die des ausgefuehrten Tasks.

## Ausblenden trifft nur die eigene Zeile

Der Knoten wird ueber seinen Eintrag adressiert - Gruppenpfad plus Name -, nicht ueber den
Namen des Tasks, den er ausfuehrt (ADR-023). Ein `hidden` auf diesen Task nimmt deshalb nur
sein Vorkommen an der Originalstelle weg:

```yaml
tasks:
  buildImage:
    hidden: true                    # am Originalplatz weg ...
groups:
  ci/nightly:
    tasks:
      Alle Images bauen:
        execute: ":app:buildImage"  # ... hier bleibt er stehen
```

Gruppen- und Modulregeln erfassen ihn dagegen wie jeden anderen Knoten: Er liegt sichtbar
darin, und ein ausgeblendeter Ordner nimmt seinen Teilbaum mit (REQ-075).

Damit das ueberhaupt moeglich ist, laeuft die Aufweitung **vor** dem Ausblenden: Sie arbeitet
auf der ungefilterten Taskliste, sonst waere der referenzierte Task schon weg, bevor der Knoten
entstehen kann.
