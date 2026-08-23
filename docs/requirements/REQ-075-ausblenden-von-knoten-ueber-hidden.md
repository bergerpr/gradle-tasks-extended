---
id: REQ-075
titel: Ausblenden von Knoten ueber hidden
kategorie: daten
verifiziert: unit
betrifft:
  - src/main/kotlin/de/example/gradletasksview/model/NodeVisibility.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/TaskViewConfig.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/GradleTasksViewConfigStore.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTaskViewRefresher.kt
quelle: OPEN-005, Abschnitte "Vorrangregeln" und "Wirkung und Sichtbarkeit"; OPEN-009
---

`hidden: true` an einem Knoteneintrag blendet diesen Knoten samt allem darunter aus - ein
Modul samt Untermodulen, einen Ordner samt Unterordnern, einen Task. `hidden: false` nimmt
eine allgemeinere Ausblendung wieder zurueck.

Damit ist das Ausblenden **kein eigenes Regelwerk** neben den Knoteneintraegen, sondern ein
Feld wie `expanded` und `icon` (REQ-076). Ein Modul auszuschliessen und einzelne Gruppenpfade
darin wieder einzublenden, ist derselbe Mechanismus, nur zweimal angewandt:

```yaml
modules:
  ":legacy":
    hidden: true              # Modul samt Untermodulen ausgeblendet ...
    groups:
      docker/gui:
        hidden: false         # ... ausser diesem Gruppenpfad
```

Ein `modes`-Eintrag macht das modusabhaengig: Ein Modul kann in `Nested` sichtbar und in
`Sort` ausgeblendet sein.

## Vorrangregeln

Ohne Eintrag ist alles sichtbar. Konkurrieren mehrere Eintraege, gewinnt der erste
zutreffende dieser Reihenfolge (siehe ADR-020):

1. **Genauigkeit** - wie genau der Eintrag den Knoten benennt: erst der Task selbst, dann die
   Ordner von innen nach aussen, zuletzt das Modul. `docker/gui/jsp` schlaegt `docker/gui`,
   und das schlaegt den Modulausschluss.
2. **Modulbezug** - ein Eintrag aus einem Modulblock schlaegt den modulunabhaengigen.
3. **Modusbezug** - ein Eintrag aus dem `modes`-Block schlaegt den modusuebergreifenden;
   genau dafuer gibt es ihn.
4. **`custom.yaml` vor `default.yaml`** - die persoenliche Datei kann eine Team-Vorgabe
   zuruecknehmen.

Verglichen wird segmentweise: `:applet` ist kein Untermodul von `:app`, und `docker/guix` kein
Unterpfad von `docker/gui`.

Der Geltungsbereich eines Modulblocks wird **exakt** verglichen: Ein Eintrag im Block `":app"`
beschreibt die Ordner und Tasks unter genau diesem Modulknoten, nicht die von `":app:core"` -
das hat seinen eigenen Block. Nur der Modulknoten selbst erfasst seine Untermodule, denn er
benennt sie.

## Wirkung

Die Filterung ist reine Modelllogik und laeuft **vor** dem Baumaufbau. Dadurch entstehen keine
leeren Gruppen- und Ordnerknoten, die nach dem Ausblenden uebrig blieben; ein ausgeblendeter
Ordner nimmt seinen Teilbaum dadurch mit, dass dessen Tasks wegfallen.

Sie greift **immer**, auch bei ausgeschalteter Modulgliederung: Der Feststellbutton (REQ-074)
aendert die Darstellung, nicht den Inhalt.

Sie greift ausserdem **vor** der Textsuche (REQ-013), sodass die Suche keine Tasks aus
ausgeblendeten Knoten findet - ein Treffer, den der sichtbare Baum nicht enthaelt, waere ein
Widerspruch.
