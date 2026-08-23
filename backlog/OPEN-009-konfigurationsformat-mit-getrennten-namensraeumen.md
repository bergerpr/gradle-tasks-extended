---
id: OPEN-009
titel: Konfigurationsformat mit getrennten Namensraeumen
prioritaet: hoch
betrifft_anforderung:
  - REQ-070
  - REQ-071
  - REQ-075
  - REQ-076
betrifft:
  - src/main/kotlin/de/example/gradletasksview/persistence/TaskViewConfig.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/TaskViewConfigKey.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/TaskViewConfigYaml.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/GradleTasksViewConfigStore.kt
  - src/main/kotlin/de/example/gradletasksview/model/ModuleFilter.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTaskViewRefresher.kt
quelle: Benutzerentscheidung in der Sitzung zu OPEN-008
---

Das gewachsene Format traegt die neuen Anforderungen nicht mehr sauber. Es wird durch eine
Struktur mit **getrennten Namensraeumen** ersetzt. Migration ist ausdruecklich nicht noetig:
Das Projekt ist in Entwicklung, bestehende Dateien duerfen neu geschrieben werden.

## Was am alten Format nicht mehr traegt

- **Gruppen und Tasks teilen sich einen Schluesselraum.** Bisher harmlos, weil jeder
  Knotentyp nur seinen eigenen Schluessel liest. Mit `hidden` (OPEN-008) nicht mehr: In Gradle
  ist `build` sowohl eine verbreitete Gruppe als auch ein verbreiteter Taskname, und wer den
  Task ausblendet, blendet still die ganze Gruppe mit weg.
- **Drei Sorten Schluessel auf oberster Ebene**, unterscheidbar nur an Konventionen (ein
  reservierter Name, ein fuehrender `:`, alles Uebrige). Jede neue Sorte braucht die naechste
  Konvention.
- **Zwei Mechanismen fuer dieselbe Sache.** `exclude`/`include` blendet Module und
  Gruppenpfade aus, `hidden` soll Knoten ausblenden. Zwei Regelwerke mit unterschiedlichen
  Vorrangregeln fuer ein und dieselbe Frage "ist das sichtbar?".

## Aufbau

Vier feste Schluessel auf oberster Ebene, darunter ist nichts mehr mehrdeutig:

```yaml
settings:
  showModules: true

groups:                       # gelten in allen Modulen
  build: false                # Kurzform: nur der Aufklappzustand
  docker/gui/jsp:
    expanded: true
    icon: smile.svg

tasks:                        # eigener Namensraum, keine Kollision mit gleichnamigen Gruppen
  buildImage:
    icon: smile.svg

modules:
  ":":                        # Wurzelprojekt
    icon: smile.svg
  ":app":
    expanded: true
    groups:
      docker/gui/jsp:
        expanded: false
    tasks:
      buildImage:
        icon: app.svg
  ":legacy":
    hidden: true              # Modul samt Untermodulen ausgeblendet ...
    groups:
      docker/gui:
        hidden: false         # ... ausser diesem Pfad
  ":app:core":
    modes:
      sort:
        hidden: true          # nur im Sort-Modus ausgeblendet
```

Geschrieben wird durchgaengig in Block-Schreibweise (`DumperOptions.FlowStyle.BLOCK`), damit
die versionierte `default.yaml` zeilenweise diffbar bleibt. Die kompakte Flow-Schreibweise
(`{ expanded: false }`) ist dasselbe YAML und wird beim Lesen selbstverstaendlich akzeptiert -
sie ist nur nicht das, was das Plugin erzeugt.

Modulschluessel bleiben **absolut** (`":app:core"` steht neben `":app"`, nicht darin) - die
Anzeige ist verschachtelt, der Schluessel nicht. Der Grund von damals gilt unveraendert: Ein
verschachtelter Schluessel liesse sich nicht von einem Untereintrag des Moduls unterscheiden.

## Knoteneintrag

Ein Eintrag unter `groups`, `tasks` oder direkt in einem Modulblock kennt:

| Feld | Bedeutung |
|---|---|
| `expanded` | Auf-/Zuklappzustand. Bei Tasks ohne Wirkung (Blattknoten). |
| `icon` | Dateiname relativ zu `icons/`, ersetzt das eingebaute Icon. |
| `hidden` | Blendet den Knoten samt Teilbaum aus (OPEN-008). |
| `modes` | Abbildung Modus -> Eintrag; ueberschreibt die Felder daneben fuer diesen Modus. |

Alle Felder sind dreiwertig (`null` = nicht konfiguriert). Die Kurzform - ein blanker Boolean
statt einer Abbildung - bleibt beim **Lesen** erlaubt und bedeutet `expanded`; geschrieben
wird sie nur, wenn ein Eintrag ausser `expanded` nichts enthaelt.

## Aufloesung

Pro Knoten und Feld gewinnt der spezifischste Treffer, von oben nach unten:

1. `custom.yaml`, Modulblock, `modes`-Eintrag des aktiven Modus
2. `custom.yaml`, Modulblock
3. `custom.yaml`, modulunabhaengig, `modes`-Eintrag
4. `custom.yaml`, modulunabhaengig
5. dieselben vier Stufen aus `default.yaml`
6. eingebauter Standard (aufgeklappt, eingebautes Icon, sichtbar)

Innerhalb einer Stufe erbt ein Pfad von seinen Vorfahren: `docker/gui` wirkt auf
`docker/gui/jsp`, `:legacy` auf `:legacy:child` - jeweils uebersteuerbar durch den laengeren
Pfad. Verglichen wird segmentweise, `:applet` ist also kein Untermodul von `:app` und
`docker/guix` kein Unterpfad von `docker/gui`.

## `exclude`/`include` entfaellt

Beides geht in `hidden` auf. Ein `":legacy"` mit `hidden: true` und darunter ein
`hidden: false` auf dem Gruppenpfad `docker/gui` ist genau das, was der bisherige
`include`-Eintrag ausgedrueckt hat - nur mit demselben Mechanismus wie jede andere
Sichtbarkeitsfrage und ohne die Sonderregel, dass `include` eine Abbildung statt einer Liste
sein muss.

Damit entfaellt auch die **Vereinigungsregel** aus ADR-016, nach der Listen aus `custom.yaml`
die aus `default.yaml` nur ergaenzen konnten. Das ist eine bewusste Verhaltensaenderung: Eine
persoenliche Einstellung kann eine Team-Vorgabe jetzt zuruecknehmen (`hidden: false`) statt
sie nur erweitern zu koennen. Genau das war bei OPEN-008 als Gegenmittel gewuenscht, und zwei
gegenlaeufige Vorrangregeln im selben Format waeren nicht erklaerbar.

REQ-075 und ADR-016 sind danach neu zu fassen, ADR-014 wird abgeloest.

## `custom.yaml` speichert nur Abweichungen

Ein Knoten wird nur festgeschrieben, wenn sein Zustand vom **aufgeloesten** Wert abweicht;
stimmt er wieder mit ihm ueberein, faellt der Eintrag heraus. Ein `Collapse All` erzeugt
dadurch keine Wand aus Eintraegen mehr, und die Datei bleibt von Hand lesbar.

Folge, die bewusst in Kauf genommen wird: Aendert sich spaeter die `default.yaml`, verschiebt
sich der persoenliche Zustand mit - er ist als Abweichung davon definiert, nicht als
Momentaufnahme.

## Arbeitspakete

Bewusst so geschnitten, dass **jedes Paket fuer sich uebersetzt, seine Tests bestehen und
committet werden kann**. Ein halbfertiger Formatumbau waere sonst ein Zustand, aus dem heraus
niemand - auch keine spaetere Sitzung - weiterarbeiten kann.

- **AP-1 Datenmodell und Datei-IO.** `TaskViewConfig`, `TaskViewConfigKey` und
  `TaskViewConfigYaml` auf `settings`/`groups`/`tasks`/`modules` umstellen, Knoteneintraege um
  `hidden` und `modes` erweitern, Lesen und Schreiben samt Unit-Tests. `exclude`/`include`
  bleiben in diesem Schritt als Felder unter `settings` bestehen und werden unveraendert
  ausgewertet - das Verhalten aendert sich also noch nicht.
- **AP-2 Sichtbarkeit vereinheitlichen.** `hidden` auswerten, `ModuleFilter` zu einer
  Sichtbarkeitsaufloesung ueber Modul-, Gruppen- und Taskpfade verallgemeinern,
  `exclude`/`include` entfernen. Die vorhandenen `ModuleFilterTest`-Faelle werden dabei auf
  `hidden` umgeschrieben, nicht geloescht: Sie sind der Nachweis, dass der `:legacy`-Fall
  weiter genauso aussieht.
- **AP-3 Nur Abweichungen schreiben.** `custom.yaml` nimmt einen Eintrag nur auf, wenn er vom
  aufgeloesten Wert abweicht, und entfernt ihn wieder, sobald er ihm entspricht.
- **AP-4 Fixture und Wissensbasis.** `default.yaml` des Testprojekts umstellen, REQ-070,
  REQ-071, REQ-075 und REQ-076 neu fassen, ADR-014 und ADR-016 abloesen, `CLAUDE.md`
  nachziehen.

Danach erst OPEN-008 (Kontextmenue-Eintrag und Toolbar-Umschalter).

## Reihenfolge

OPEN-009 kommt **vor** OPEN-008: `hidden` gleich im neuen Format zu bauen erspart, es zweimal
zu implementieren. Der Testfixture-`default.yaml` unter
`../gradle-tasks-extended-testproject` wird dabei mit umgestellt und deckt danach wieder das
vollstaendige Format ab.
