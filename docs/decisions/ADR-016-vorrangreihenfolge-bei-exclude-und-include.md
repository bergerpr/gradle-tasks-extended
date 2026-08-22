---
id: ADR-016
titel: Vorrangreihenfolge bei Exclude und Include
betrifft:
  - src/main/kotlin/de/example/gradletasksview/model/ModuleFilter.kt
ersetzt_vorgabe: OPEN-005, Abschnitt "Vorrangregeln" (zwei Regeln fuer denselben Gleichstand)
quelle: OPEN-005, Arbeitspaket AP-4
---

Die Vorgabe nannte zwei Regeln fuer "gleiche Spezifitaet", ohne zu sagen, welche zuerst
greift:

- "Bei gleicher Spezifitaet gewinnt `include`."
- "Modusabhaengige Listen ergaenzen die globalen ebenfalls; bei gleicher Spezifitaet gewinnt
  der Moduseintrag."

Sie kollidieren genau dann, wenn ein modusuebergreifendes `include` und ein modusabhaengiges
`exclude` auf denselben Modulpfad zeigen.

## Festgelegte Reihenfolge

Absteigend gewichtet:

1. **Spezifitaet** - der laengere Modulpfad gewinnt.
2. **Modusbezug** - bei gleicher Spezifitaet gewinnt der Eintrag aus dem `modes`-Block.
3. **`include` vor `exclude`.**

Ausschlaggebend war, dass die Modusregel sonst wirkungslos waere: Ein modusabhaengiges
`exclude` koennte ein modusuebergreifendes `include` desselben Pfades nie korrigieren - also
genau den Fall nicht, fuer den es die Regel gibt. Die Vorsicht hinter "include gewinnt" bleibt
dort erhalten, wo sie gemeint war: innerhalb desselben Geltungsbereichs.

Ergaenzend, und unabhaengig von diesem Vorrang: Sind mehrere `include`-Eintraege gleich
spezifisch, werden ihre Gruppenpfade **vereinigt**. Das ist das "ergaenzen" aus der Vorgabe -
es betrifft den Inhalt der Listen, nicht die Frage, ob ein- oder ausgeblendet wird.

## Warum ein `include` allein nichts einschraenkt

Ein `include`, zu dem es kein passendes `exclude` gibt, bleibt wirkungslos. Sonst wuerde
`include: {":legacy": [docker/gui]}` allein bereits alles andere in `:legacy` ausblenden - ein
Ausschluss, den niemand geschrieben hat. `include` ist immer nur die Ausnahme zu einem
Ausschluss.
