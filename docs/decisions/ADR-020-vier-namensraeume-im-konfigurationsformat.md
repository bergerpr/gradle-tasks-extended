---
id: ADR-020
titel: Vier Namensraeume im Konfigurationsformat
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/persistence/TaskViewConfig.kt
  - src/main/kotlin/de/berger/gradletasksview/persistence/TaskViewConfigKey.kt
  - src/main/kotlin/de/berger/gradletasksview/persistence/TaskViewConfigYaml.kt
  - src/main/kotlin/de/berger/gradletasksview/persistence/GradleTasksViewConfigStore.kt
  - src/main/kotlin/de/berger/gradletasksview/model/NodeVisibility.kt
ersetzt_vorgabe: ADR-014 (flacher Schluesselraum), ADR-016 (Vorrang bei Exclude/Include)
quelle: OPEN-009
---

Die oberste Ebene von `default.yaml`/`custom.yaml` besteht aus genau vier festen Schluesseln:
`settings`, `groups`, `tasks` und `modules`. Darunter ist kein Schluessel mehr an einer
Konvention zu erkennen, sondern durch seinen Ort bestimmt.

Das loest ADR-014 ab, wo drei Sorten Schluessel nebeneinander auf oberster Ebene standen -
ein reservierter Name, ein fuehrender `:` und "alles Uebrige".

## Warum das noetig wurde

Der flache Schluesselraum hatte einen Defekt, der lange folgenlos blieb: **Gruppenpfade und
Tasknamen teilten sich einen Namensraum.** ADR-014 hielt das fuer unbedenklich, weil jeder
Knotentyp beim Rendern nur seinen eigenen Schluessel nachschlaegt - ein Gruppeneintrag konnte
also kein Task-Icon setzen.

Mit `hidden` (OPEN-008) traegt diese Begruendung nicht mehr. In Gradle ist `build` sowohl eine
verbreitete Gruppe als auch ein verbreiteter Taskname; wer den Task ausblendet, haette still
die ganze Gruppe mit ausgeblendet. Ein Defekt, der sich als "ein Task fehlt" aeussert, faellt
oft erst auf, wenn ihn jemand sucht.

Das Kompatibilitaetsargument aus ADR-014 - bestehende Dateien sollen unveraendert
weiterfunktionieren, weil das Plugin `default.yaml` nie umschreibt - traegt ebenfalls nicht
mehr: Das Projekt ist in Entwicklung, und es gibt eine Handvoll solcher Dateien, die von Hand
umgestellt werden koennen. Eine Migration ist deshalb ausdruecklich nicht vorgesehen.

Unveraendert gilt: Modulschluessel bleiben **absolut** (`":app:core"` steht neben `":app"`,
nicht darin), obwohl die Anzeige verschachtelt ist. Sonst waere ein Unterschluessel eines
Modulblocks nicht von einem Modulpfad zu unterscheiden.

## Sichtbarkeit wird ein Feld wie jedes andere

`exclude`/`include` entfaellt und geht in `hidden` auf (REQ-075). Ein Modulblock mit
`hidden: true` und darunter ein `hidden: false` auf einem Gruppenpfad ist genau das, was der
`include`-Eintrag ausdrueckte - mit demselben Mechanismus wie jede andere Sichtbarkeitsfrage
und ohne die Sonderregel, dass `include` eine Abbildung statt einer Liste sein muss.

Die Vorrangordnung ist dieselbe wie bei `expanded`/`icon`, nur vorne um ein Kriterium
erweitert: **Genauigkeit** (Task vor Ordner von innen nach aussen vor Modul), dann
**Modulbezug**, dann **Modusbezug**, dann **`custom.yaml` vor `default.yaml`**.

Damit entfaellt die Vereinigungsregel aus ADR-016, nach der Listen aus `custom.yaml` die aus
`default.yaml` nur ergaenzen und nie zuruecknehmen konnten. Das ist eine bewusste
Verhaltensaenderung. Die Vorsicht dahinter - eine Team-Vorgabe soll nicht unbemerkt
verschwinden - war an Listen gebunden, bei denen "ergaenzen" die einzige verlustfreie
Zusammenfuehrung ist. Bei einem Feld auf zwei Ebenen gibt es diese Wahl nicht: Entweder
gewinnt der spezifischere Eintrag, oder er ist wirkungslos. Und ein `hidden: false`, mit dem
sich eine Team-Vorgabe fuer ein einzelnes Modul zuruecknehmen laesst, war bei OPEN-008
ausdruecklich gewuenscht. Zwei gegenlaeufige Vorrangregeln im selben Format waeren nicht
erklaerbar gewesen.

Was von ADR-016 bleibt, ist die Reihenfolge Spezifitaet vor Modusbezug und die Begruendung
dafuer: Ein modusbezogener Eintrag muss einen modusuebergreifenden korrigieren koennen, sonst
gaebe es ihn umsonst.

## `custom.yaml` haelt nur Abweichungen fest

Ein Knoten wird nur geschrieben, wenn sein Zustand von dem abweicht, was ohne diesen Eintrag
gaelte; stimmt er wieder ueberein, verschwindet er. Vorher schrieb ein einziges "Collapse All"
rund vierzig Zeilen in die Datei, die fast alle nur den Standard wiederholten.

Der Preis ist bewusst in Kauf genommen: Aendert sich spaeter die `default.yaml`, verschiebt
sich der persoenliche Zustand mit. Er ist als Abweichung davon definiert, nicht als
Momentaufnahme - und genau das ist bei einer Team-Vorgabe auch das erwartbare Verhalten.
