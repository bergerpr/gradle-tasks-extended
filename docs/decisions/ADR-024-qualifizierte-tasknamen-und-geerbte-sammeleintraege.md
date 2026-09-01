---
id: ADR-024
titel: Qualifizierte Tasknamen und geerbte Sammeleintraege der Plattform
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/model/GradleTaskDataProvider.kt
  - src/main/kotlin/de/berger/gradletasksview/model/GradleTaskDescriptor.kt
ersetzt_vorgabe: ADR-019, Abschnitt "Folge" (Diagnose war unvollstaendig)
quelle: OPEN-010, Sandbox-Sitzung vom 2026-08-25
---

Die Task-Daten der Gradle-Integration sehen anders aus, als der bisherige Code annahm. In der
Sandbox (IU-2026.2, Testprojekt `../gradle-tasks-extended-testproject`) ausgelesen:

```text
MODUL :lib   -> :lib:printLibGui [inherited=false]      # eigener Task, QUALIFIZIERTER Name
MODUL :      -> wrapper          [inherited=false]      # eigener Task, blanker Name
MODUL :      -> printLibGui      [inherited=true]       # Sammeleintrag ueber die Unterprojekte
MODUL :tools -> printToolsCli    [inherited=true]       # dasselbe eine Ebene tiefer
```

Daraus folgen zwei Regeln:

1. **`TaskData.getName()` ist bei Untermodulen der qualifizierte Pfad.** Der blanke Name wird
   daraus gewonnen (`bareTaskName`). Er ist die Beschriftung im Baum, der Schluessel im
   `tasks:`-Namensraum der Konfiguration, der Suchtext und der Wert, den die Ausfuehrung
   erwartet - ein qualifizierter Name wuerde von der Plattform ein zweites Mal praefigiert
   (ADR-018).
2. ~~**Geerbte Eintraege werden uebersprungen.**~~ **Ueberholt durch ADR-025.** Sie sind kein
   eigener Task des Moduls, sondern Gradles Sammelform ueber dessen Unterprojekte; der echte
   Task haengt am Modulknoten, in dem er definiert ist. Weggeworfen werden sie deswegen aber
   nicht: Die originale Gradle-Ansicht zeigt sie standardmaessig an, und in einem Projekt, das
   seine Plugins zentral fuer alle Unterprojekte setzt, sind sie die einzigen Zeilen auf
   Wurzelebene. Sie werden jetzt markiert, kursiv dargestellt und ueber einen Feststellbutton
   abschaltbar (REQ-088, ADR-025).

## Was vorher schiefging (OPEN-010)

Der Name wurde ungeprueft uebernommen und der Sammeleintrag wie ein echter Task behandelt.
Sichtbar wurde das als:

- Zeilen, die `:lib:printLibGui` hiessen statt `printLibGui`, und Taskpfade der Form
  `:app:core::app:core:printCoreReact` - Letzteres stand schon im Log zu ADR-019 und wurde
  dort als Problem der Modulaufloesung gedeutet. Die Modulknoten waren aber immer korrekt
  (`identity=:lib`); falsch war der Name.
- Jeder Task erschien ein zweites Mal unter dem Wurzelprojekt (und ein drittes unter jedem
  Zwischenmodul), weil die Sammeleintraege mitgezaehlt wurden. Diese Duplikate trugen den
  blanken Namen und waren damit die einzigen, die ein `tasks:`-Eintrag der Konfiguration
  ueberhaupt traf.
- `stableId` fiel fuer gleichnamige Tasks verschiedener Module zusammen (REQ-024), weil die
  Duplikate alle am Wurzelprojekt hingen.
- Ausgefuehrt wurde ein solches Duplikat im Verzeichnis des Wurzelprojekts mit blankem Namen -
  also in **jedem** passenden Unterprojekt statt in einem (REQ-010).

## Warum die Sammeleintraege nicht behalten wurden (ueberholt, siehe ADR-025)

Man koennte sie als "in allen Unterprojekten ausfuehren" anbieten. Genau das soll die Ansicht
aber nicht: REQ-010 fuehrt Tasks bewusst im eigenen Modul aus, und eine Zeile, die wie ein
gewoehnlicher Task aussieht, aber mehrere Builds anstoesst, ist eine Falle. Wer das braucht,
selektiert mehrere Zeilen oder legt sich einen eigenen Knoten mit `execute` an (REQ-085).
