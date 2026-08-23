---
id: ADR-017
titel: Default zeigt nicht mehr die Originalreihenfolge
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTreeModelBuilder.kt
  - src/main/kotlin/de/berger/gradletasksview/actions/GradleTaskShowModulesToggleAction.kt
ersetzt_vorgabe: Plan.md, Abschnitt "Zielverhalten / Default" (ehemals REQ-002)
quelle: OPEN-007, Abschnitte "Entschieden" und "Katalog danach"
---

Die urspruengliche Spezifikation gab dem Modus `Default` eine eigene, ausdrueckliche Aufgabe:
Gruppen und Tasks genau in der Reihenfolge zu zeigen, in der External System sie liefert - ohne
Sortierung, ohne Ordnerknoten. Genau das ist jetzt nicht mehr der Fall (REQ-077). Das ist eine
bewusste Kehrtwende, keine Verfeinerung, und deshalb steht sie hier.

## Warum

`Default` sollte die vertraute Ansicht fuer Umsteiger sein. Die originale Gradle-Ansicht der
IDE sortiert aber alphabetisch, und sie zeigt eine Modulebene mit einem `Tasks`-Knoten. Eine
unsortierte, flache Liste ist damit gerade **nicht** das Vertraute - sie war eine dritte,
eigene Darstellung neben `Sort` und `Nested`, und die von Gradle gelieferte Reihenfolge hat
fuer den Benutzer keine erkennbare Bedeutung, an der sich die Ansicht festhalten liesse.

Damit wird die Originalreihenfolge nirgends mehr angezeigt. Sie geht nicht verloren -
`GradleTaskDataProvider` liefert sie unveraendert -, sie wird nur nicht mehr dargestellt.

## Folgen, die mitentschieden sind

- **REQ-002 ist entfernt**, die ID wird nicht neu vergeben (Regel aus `docs/SCHEMA.md`).
- **Der Modusname bleibt `Default`.** `GradleTaskViewMode.DEFAULT` steht in
  `.idea/gradleTasksView.xml` jedes Projekts, und `modes: default:` kann in jeder
  `default.yaml` stehen (REQ-076). Eine Umbenennung wuerde beides brechen, und zwar still. Der
  Name traegt jetzt die Bedeutung "die uebliche Ansicht", und das ist er auch.
- **Die Orthogonalitaet aus REQ-074 gilt nur noch fuer zwei der drei Modi.** Aus sechs
  Kombinationen werden fuenf, weil die Modulebene in `Default` fest ist. Auch das weicht von
  einer gerade erst getroffenen Entscheidung ab und ist deshalb in REQ-074 selbst
  nachgetragen, nicht nur hier.

## Was dabei offen bleibt

`Default` und `Nested` mit eingeschalteter Modulgliederung unterscheiden sich danach nur noch
durch den `Tasks`-Knoten und dadurch, dass die Modulebene in `Default` nicht abschaltbar ist.
Diese Aehnlichkeit wird bewusst in Kauf genommen: `Default` ist die vertraute Ansicht fuer
Umsteiger, `Nested` die aufgeraeumte ohne Projektebene. Ob die drei Modi deshalb spaeter neu
zugeschnitten gehoeren, ist hier ausdruecklich **nicht** entschieden - es soll nur nicht
unbemerkt faellig werden.
