---
id: REQ-077
titel: Modus Default bildet die originale Gradle-Ansicht nach
kategorie: modi
verifiziert: unit
betrifft:
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTreeModelBuilder.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTreeNode.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTreeCellRenderer.kt
  - src/main/kotlin/de/example/gradletasksview/actions/GradleTaskShowModulesToggleAction.kt
  - src/main/kotlin/de/example/gradletasksview/icons/GradleTasksIcons.kt
quelle: OPEN-007
---

Der Modus `Default` sieht aus wie die originale Gradle-Ansicht der IDE - Modulebene, darunter
je ein Knoten `Tasks`, darunter die Taskgruppen - mit **einer** Erweiterung: Die Gruppen
erscheinen nicht als flache Zeilen, sondern als Ordnerbaum wie im Modus `Nested` (REQ-004),
zusammengefasste Zwischenpfade eingeschlossen.

```text
:                            (Wurzelprojekt)
|--- Tasks
|    |--- build
|    |    |--- assembleAll
|    |--- docker/gui         (zusammengefasst, siehe REQ-004)
|    |    |--- printRootGui
|    |--- tooling
|         |--- rootOnly
|--- :app
|    |--- Tasks
|    |    |--- application
|    |    |    |--- runApp
|    |    |--- docker/gui
|    |         |--- printAppGui
|    |         |--- jsp
|    |              |--- printAppJsp
|    |--- :app:core
|         |--- Tasks
|              |--- verification
|                   |--- coreCheck
|--- :lib
     |--- Tasks
          |--- ...
```

Erst der eigene Inhalt eines Moduls, dann seine Untermodule - dieselbe Reihenfolge wie "Tasks
vor Unterordnern" (REQ-004) und wie bei der Modulgliederung (REQ-074).

## Die Modulebene ist hier immer an

Der Feststellbutton der Modulgliederung (REQ-074) wirkt nur in `Sort` und `Nested`. In
`Default` ist die Projektstruktur Teil der Ansicht und keine Option daneben - sonst waere
"entspricht dem Original" nur die halbe Wahrheit. Der Button wird in `Default` ausgegraut und
angehakt dargestellt, statt wirkungslos anklickbar zu bleiben.

Der **gespeicherte** Zustand des Buttons bleibt unberuehrt: Wer in `Nested` die Module
eingeschaltet hat, findet sie dort nach einem Ausflug nach `Default` unveraendert vor.

## Ein Knoten `Tasks` je Modul

Zwischen Modulknoten und Ordnerbaum steht ein Knoten `Tasks`, wie im Original. Ein Modul ohne
eigene Tasks bekommt **keinen** - dort stuenden sonst eine leere Zeile und ein Klick ins Nichts.

Er ist ein reiner Strukturknoten und **nicht** eigenstaendig konfigurierbar: kein Schluessel in
`default.yaml`/`custom.yaml` (`configKeyOf` liefert `null`), kein eigenes Icon, kein eigener
Expand-Zustand - er folgt dem seines Moduls. Zwei Gruende: Ein Schluessel `Tasks` liesse sich
nicht von einer gleichnamigen echten Gradle-Gruppe unterscheiden, und solange der Knoten das
einzige Kind seines Moduls ist, waere ihn zuzuklappen dasselbe wie das Modul zuzuklappen. Aus
demselben Grund ist er auch nicht ueber die Speed-Search auffindbar.

Sein Icon (`taskList.svg`) ist ein eigenes, nicht das Ordner-Icon aus REQ-005:
Er ist weder Modul noch Gruppe.

## Sortiert wird alphabetisch

Das Original sortiert Gruppen und Tasks alphabetisch, und "entspricht dem Original" schliesst
das ein - mit den vorhandenen Komparatoren aus `TaskTreeSorting`, inklusive der Regel, dass die
Gruppe `Other` immer zuletzt steht (REQ-022, REQ-023). Module bleiben alphabetisch nach ihrem
Projektpfad geordnet (REQ-074).

Damit wird die von External System gelieferte Reihenfolge nirgends mehr angezeigt; die
abgeloeste Anforderung REQ-002 ist entfallen, siehe ADR-017. Der Modusname bleibt trotzdem
`Default` - er traegt jetzt die Bedeutung "die uebliche Ansicht".

## Folgen fuer die Persistenz

Ordnerknoten liegen in `Default` unter einem Modul und werden damit modulbezogen
geschluesselt (REQ-041, REQ-076) - anders als frueher, wo `Default` ohne Modulebene
modulunabhaengige Schluessel schrieb. Neue Auf-/Zuklapp-Aktionen landen also ab jetzt in einem
Modulblock von `custom.yaml`.

Modulunabhaengige Eintraege werden davon **nicht** wirkungslos: Die Aufloesung faellt vom
modulbezogenen auf den modulunabhaengigen Schluessel zurueck (REQ-076), ein `docker/gui` unter
`groups:` wirkt in `Default` also weiter - bis es fuer ein einzelnes Modul ueberschrieben
wird.
Was sich aendert, ist die Reichweite: Derselbe Ordner kann in `:app` und in `:lib` jetzt
unterschiedlich aufgeklappt sein, wo `Default` zuvor nur einen gemeinsamen Zustand kannte.

Das Ausblenden (REQ-075) wirkt unveraendert vor dem Baumaufbau, und der Selektionserhalt
(REQ-014) haengt an `stableId` und damit nicht an der Baumform.
