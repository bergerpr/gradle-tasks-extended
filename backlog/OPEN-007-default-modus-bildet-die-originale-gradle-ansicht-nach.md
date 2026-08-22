---
id: OPEN-007
titel: Default-Modus bildet die originale Gradle-Ansicht nach
prioritaet: hoch
betrifft_anforderung:
  - REQ-001
  - REQ-002
  - REQ-004
  - REQ-041
  - REQ-074
  - REQ-076
betrifft:
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTreeModelBuilder.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTreeNode.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTreeCellRenderer.kt
  - src/main/kotlin/de/example/gradletasksview/actions/GradleTaskShowModulesToggleAction.kt
  - src/main/kotlin/de/example/gradletasksview/model/TaskTreeBuilder.kt
quelle: Anforderung des Benutzers
---

Der `Default`-Modus soll aussehen wie die originale Gradle-Ansicht der IDE, mit einer
Erweiterung: Die Taskgruppen darunter erscheinen nicht als flache Zeilen, sondern als
Ordnerbaum wie im `Nested`-Modus.

**Setzt OPEN-006 voraus.** Ohne das Zusammenfassen von Ordnerpfaden waere diese Ansicht
tiefer verschachtelt als jede bisherige, weil zur Modulebene und zum `Tasks`-Knoten noch je
eine Zeile pro Pfadsegment kaeme.

## Zielbild

```text
:                            (Wurzelprojekt)
|--- Tasks
|    |--- build
|    |    |--- assembleAll
|    |--- docker/gui         (zusammengefasst, siehe OPEN-006)
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

Erst der eigene Inhalt eines Moduls, dann seine Untermodule - dieselbe Reihenfolge wie
"Tasks vor Unterordnern" (REQ-004) und wie bei der Modulgliederung (REQ-074).

## Entschieden

Die folgenden Punkte waren offen und sind gemeinsam festgelegt worden.

### Die Modulebene ist in Default immer an

Der Feststellbutton der Modulgliederung (REQ-074) wirkt nur noch in `Sort` und `Nested`. In
`Default` ist die Projektstruktur Teil der Ansicht, nicht eine Option daneben - sonst waere
"entspricht dem Original" nur die halbe Wahrheit. Der Button wird in `Default` ausgegraut,
statt wirkungslos anklickbar zu bleiben.

Damit gilt die in REQ-074 festgehaltene Orthogonalitaet nur noch fuer zwei der drei Modi: Aus
sechs Kombinationen werden fuenf. Das ist eine bewusste Abweichung von einer gerade erst
getroffenen Entscheidung und gehoert beim Umsetzen ausdruecklich in REQ-074, nicht in eine
Fussnote.

Der gespeicherte Zustand des Buttons bleibt davon unberuehrt: Wer in `Nested` die Module
eingeschaltet hat, findet sie dort nach einem Ausflug nach `Default` unveraendert vor.

### Ein Knoten "Tasks" je Modul

Zwischen Modulknoten und Ordnerbaum steht ein Knoten `Tasks`, wie im Original. Ein Modul ohne
eigene Tasks bekommt **keinen** - dort stuenden sonst eine leere Zeile und ein Klick ins Nichts.

Der Knoten ist ein reiner Strukturknoten und **nicht** eigenstaendig konfigurierbar: Er hat
keinen Schluessel in `default.yaml`/`custom.yaml`, bekommt kein eigenes Icon und folgt dem
Auf-/Zuklappzustand seines Moduls. Zwei Gruende: Ein Schluessel `Tasks` liesse sich nicht von
einer echten Gradle-Gruppe dieses Namens unterscheiden, und solange der Knoten das einzige Kind
seines Moduls ist, waere ihn zuzuklappen dasselbe wie das Modul zuzuklappen.

Sollten spaeter Geschwister danebentreten - `Dependencies`, `Run Configurations` wie im
Original -, ist diese Entscheidung neu zu treffen. Beide sind heute ausdruecklich nicht in
Sicht; dieses Plugin zeigt Tasks.

### Alphabetisch sortiert, REQ-002 wird abgeloest

Das Original sortiert Gruppen und Tasks alphabetisch, und "entspricht dem Original" schliesst
das ein. Damit verliert REQ-002 ("Default zeigt die Originalreihenfolge") seinen Inhalt: Die
von External System gelieferte Reihenfolge wird nirgends mehr angezeigt.

Sortiert wird mit den vorhandenen Komparatoren aus `TaskTreeSorting`, inklusive der Regel, dass
die Gruppe `Other` immer zuletzt steht (REQ-022, REQ-023). Module bleiben alphabetisch nach
ihrem Projektpfad geordnet, wie schon in REQ-074 festgelegt.

### Der Modusname bleibt "Default"

Obwohl der Name nicht mehr "Originalreihenfolge" meint, bleibt er. `GradleTaskViewMode.DEFAULT`
steht in `.idea/gradleTasksView.xml` jedes Projekts, und `modes: default:` kann in jeder
`default.yaml` stehen (REQ-076) - eine Umbenennung wuerde beides brechen, und zwar still. Der
Name traegt jetzt die Bedeutung "die uebliche Ansicht", und das ist er auch.

## Was das fuer die drei Modi bedeutet

Nach dieser Aenderung unterscheiden sich `Default` und `Nested` mit eingeschalteter
Modulgliederung nur noch durch den `Tasks`-Knoten und dadurch, dass die Modulebene in `Default`
nicht abschaltbar ist. Das ist beim Umsetzen bewusst zur Kenntnis zu nehmen: Entweder man
akzeptiert die Aehnlichkeit, weil `Default` die vertraute Ansicht fuer Umsteiger ist und
`Nested` die aufgeraeumte ohne Projektebene - oder die drei Modi gehoeren spaeter neu
zugeschnitten. Eine Entscheidung dazu ist **nicht** Teil dieses Eintrags, aber sie sollte
nicht unbemerkt faellig werden.

`Sort` bleibt unberuehrt: flache Gruppenzeilen mit vollem Pfad, Modulebene optional.

## Auswirkungen

- **Persistenz** (REQ-041, REQ-076): Ordnerknoten liegen in `Default` jetzt unter einem Modul
  und werden damit modulbezogen geschluesselt - anders als heute, wo `Default` ohne Modulebene
  modulunabhaengige Schluessel schreibt. Ein bestehender persoenlicher Expand-Zustand aus
  `Default` greift danach nicht mehr. Das ist dieselbe Folge, die der Feststellbutton schon
  heute hat, trifft hier aber ungefragt jeden - erwaehnenswert, kein Grund fuer eine Migration.
- **Exclude/Include** (REQ-075) wirken unveraendert und weiterhin vor dem Baumaufbau.
- **Selektionserhalt** (REQ-014) unveraendert, `stableId` haengt nicht an der Baumform.
- Der `Tasks`-Knoten braucht einen eigenen Knotentyp neben `ModuleNodeData` und `FolderNodeData`
  sowie ein Icon; `configKeyOf` muss fuer ihn `null` liefern.
- `enclosingModulePath` steigt bereits ueber beliebige Zwischenknoten hinweg nach oben und
  findet das Modul auch durch den `Tasks`-Knoten hindurch - hier ist nichts zu aendern.

## Testfaelle

| Fall | Erwartung |
|---|---|
| Wurzelprojekt mit eigenen Tasks | `:` mit `Tasks`-Knoten, darunter der Ordnerbaum |
| Modul ohne eigene Tasks | kein `Tasks`-Knoten, nur die Untermodule |
| verschachteltes `:app:core` | unter `:app`, mit eigenem `Tasks`-Knoten |
| gleichnamige Tasks in `:app` und `:lib` | je einmal unter dem eigenen Modul |
| Gruppen und Tasks | alphabetisch, `Other` zuletzt |
| Feststellbutton in `Default` | ausgegraut, Ansicht unveraendert |
| Feststellbutton nach Wechsel zu `Nested` | wieder wirksam, alter Zustand erhalten |
| Ausschluss eines Moduls | wirkt auch in `Default` |

**Fertig, wenn:** Die Faelle gruen sind und die Ansicht im Testprojekt neben der originalen
Gradle-Ansicht steht, ohne dass ein Unterschied ausser dem Ordnerbaum auffaellt. Der visuelle
Teil ist in einer Sandbox-IDE zu pruefen und gehoert in OPEN-004.

## Katalog danach

- Neue Anforderung fuer den Default-Modus, IDs fortlaufend ab REQ-077. Sie loest REQ-002 ab;
  REQ-002 wird entfernt, die ID nicht neu vergeben.
- REQ-074 ergaenzen: Der Feststellbutton wirkt nur in `Sort` und `Nested`, in `Default` ist die
  Modulebene fest. Die Formulierung "sechs Kombinationen" dort korrigieren.
- REQ-001 ergaenzen, falls der ausgegraute Button dort zu erwaehnen ist.
- Einen ADR fuer die Abloesung von REQ-002 anlegen, mit `ersetzt_vorgabe` - dass der
  `Default`-Modus einmal ausdruecklich die ungeordnete Originalreihenfolge zeigen sollte, ist
  eine bewusste Kehrtwende und keine Verfeinerung.
- CLAUDE.md: Die Beschreibung der drei Modi und der sechs Kombinationen anpassen.
