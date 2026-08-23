---
id: REQ-076
titel: Aufbau und Aufloesung der YAML-Konfiguration
kategorie: persistenz
verifiziert: unit
betrifft:
  - src/main/kotlin/de/example/gradletasksview/persistence/TaskViewConfig.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/TaskViewConfigKey.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/TaskViewConfigYaml.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/GradleTasksViewConfigStore.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTreeNode.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTreeStateManager.kt
quelle: OPEN-005, Abschnitte "Ansichtseinstellungen in der YAML" und "Schluesselform mit Modulbezug"; OPEN-009
---

Die oberste Ebene von `default.yaml`/`custom.yaml` hat genau vier feste Schluessel. Darunter
ist kein Schluessel mehr an einer Konvention zu erkennen, sondern durch seinen Ort bestimmt -
insbesondere haben Gruppenpfade und Tasknamen getrennte Namensraeume (ADR-020).

```yaml
settings:                         # Ansichtseinstellungen
  showModules: true

groups:                           # Gruppen-/Ordnerpfade, gelten in allen Modulen
  docker/gui/jsp:
    icon: smile.svg

tasks:                            # Tasknamen, gelten in allen Modulen
  buildImage:
    icon: smile.svg

modules:
  ":app":                         # der Modulknoten selbst
    expanded: false
    icon: app.svg
    groups:                       # nur innerhalb von :app
      docker/gui/jsp:
        expanded: true
    tasks:
      runApp:
        hidden: true
```

Das Wurzelprojekt heisst `":"`. Modulschluessel bleiben absolut: `":app:core"` steht neben
`":app"` und nicht darin, obwohl die Anzeige verschachtelt ist - sonst waere ein
Unterschluessel eines Modulblocks nicht von einem Modulpfad zu unterscheiden.

Ein Knoteneintrag darf statt einer Abbildung ein blanker Boolean sein, wenn er nur den
Aufklappzustand setzt (`docker/gui: false`). Geschrieben wird diese Kurzform genau dann, wenn
der Eintrag sonst nichts traegt.

## Felder eines Knoteneintrags

| Feld | Bedeutung |
|---|---|
| `expanded` | Aufklappzustand. Bei Tasks ohne Wirkung, da Blattknoten. |
| `icon` | Dateiname relativ zu `icons/` (REQ-071). |
| `hidden` | Blendet den Knoten samt Teilbaum aus (REQ-075). |
| `modes` | Abweichende Werte je Ansichtsmodus; sie gehen den Feldern daneben vor. |

Alle Felder sind dreiwertig: `null` heisst "nicht konfiguriert". Nur so laesst sich "nicht
gesetzt" von einem ausdruecklich gesetzten Wert unterscheiden - was bei `hidden` darueber
entscheidet, ob ein spezifischerer Eintrag eine allgemeinere Vorgabe zuruecknehmen kann.

## Aufloesung

Pro Knoten **und Feld** gewinnt der erste Kandidat, der fuer dieses Feld einen Wert hat.
Sortiert wird nach **Modulbezug**, dann **Modusbezug**, dann **`custom.yaml` vor
`default.yaml`**:

1. Modulblock, `modes`-Eintrag des aktiven Modus - erst `custom.yaml`, dann `default.yaml`
2. Modulblock - erst `custom.yaml`, dann `default.yaml`
3. modulunabhaengig, `modes`-Eintrag - erst `custom.yaml`, dann `default.yaml`
4. modulunabhaengig - erst `custom.yaml`, dann `default.yaml`
5. eingebauter Standard

Der spezifischere Schluessel gewinnt also vor der persoenlichen Datei. Sonst koennte eine
einmal global gesetzte Vorliebe eine bewusst pro Modul gepflegte Team-Vorgabe nie zur Geltung
kommen lassen. Ein Eintrag, der nur ein Icon setzt, verdeckt dabei keinen Aufklappzustand
einer allgemeineren Ebene - entschieden wird je Feld, nicht je Eintrag.

Entlang der Pfade erbt nur `hidden`: Ein ausgeblendeter Ordner nimmt seine Unterordner mit,
ein ausgeblendetes Modul seine Untermodule (REQ-075). `expanded` und `icon` erben nicht - ein
geerbtes Icon wuerde jeden Unterordner mit dem Symbol seines Elternordners zeichnen.

Der Modulbezug eines Gruppen-/Ordnerknotens ergibt sich aus seiner Position im Baum, nicht aus
einer zweiten Ablage im Knoten selbst. Bei ausgeschalteter Modulgliederung gibt es keinen
Modulknoten ueber ihm, also greift automatisch der modulunabhaengige Schluessel. Im Modus
`Default` steht ueber ihm immer einer, weil die Modulebene dort fest ist (REQ-077); der
dazwischenliegende `Tasks`-Knoten aendert daran nichts, da der Modulbezug ueber beliebige
Zwischenknoten hinweg gesucht wird.

`showModules` liegt in derselben zweistufigen Konfiguration wie alles andere:
`default.yaml` als Team-Vorgabe, `custom.yaml` als persoenliche Abweichung, die der
Feststellbutton schreibt. "Reset to Default" (REQ-072) setzt sie damit mit zurueck.
