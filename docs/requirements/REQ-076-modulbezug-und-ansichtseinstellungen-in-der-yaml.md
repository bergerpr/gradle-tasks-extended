---
id: REQ-076
titel: Modulbezug und Ansichtseinstellungen in der YAML-Konfiguration
kategorie: persistenz
verifiziert: unit
betrifft:
  - src/main/kotlin/de/example/gradletasksview/persistence/TaskViewConfig.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/TaskViewConfigKey.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/TaskViewConfigYaml.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/GradleTasksViewConfigStore.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTreeNode.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTreeStateManager.kt
quelle: OPEN-005, Abschnitte "Ansichtseinstellungen in der YAML" und "Schluesselform mit Modulbezug"
---

Auf oberster Ebene von `default.yaml`/`custom.yaml` stehen drei Arten von Schluesseln
nebeneinander. Bestehende Dateien funktionieren dadurch unveraendert weiter, eine Migration
entfaellt - was noetig ist, weil das Plugin `default.yaml` nie umschreibt (REQ-070).

```yaml
gradle_tasks_extended:            # reservierter Schluessel: Ansichtseinstellungen
  showModules: true
  exclude:
    - ":legacy"
  include:
    ":legacy":
      - docker/gui
  modes:
    sort:
      exclude:
        - ":sandbox"

docker/gui/jsp:                   # ohne Modulbezug: gilt fuer alle Module, wie bisher
  icon: smile.svg

":app":                           # fuehrender Doppelpunkt = Modulblock
  expanded: false                 #   reserviert: der Modulknoten selbst
  icon: app.svg
  docker/gui/jsp:                 #   alles andere: Gruppenpfad nur innerhalb von :app
    expanded: true
```

Das Wurzelprojekt heisst `":"`. Modulschluessel bleiben absolut: `":app:core"` steht auf
oberster Ebene und nicht innerhalb des `":app"`-Blocks, obwohl die Anzeige verschachtelt ist.
Begruendung fuer alle drei Formentscheidungen: ADR-014.

## Aufloesung

Pro Knoten, von spezifisch nach allgemein: modulbezogener Eintrag aus `custom.yaml` >
modulbezogener Eintrag aus `default.yaml` > modulunabhaengiger Eintrag aus `custom.yaml` >
modulunabhaengiger Eintrag aus `default.yaml` > eingebauter Standard.

Der spezifischere Schluessel gewinnt also vor der persoenlichen Datei. Sonst koennte eine
einmal global gesetzte Vorliebe eine bewusst pro Modul gepflegte Team-Vorgabe nie zur Geltung
kommen lassen. Fuer die Exclude-/Include-**Listen** gilt das nicht - sie ergaenzen einander,
siehe REQ-075.

Der Modulbezug eines Gruppen-/Ordnerknotens ergibt sich aus seiner Position im Baum, nicht aus
einer zweiten Ablage im Knoten selbst. Bei ausgeschalteter Modulgliederung gibt es keinen
Modulknoten ueber ihm, also greift automatisch der modulunabhaengige Schluessel. Im Modus
`Default` steht ueber ihm immer einer, weil die Modulebene dort fest ist (REQ-077); der
dazwischenliegende `Tasks`-Knoten aendert daran nichts, da der Modulbezug ueber beliebige
Zwischenknoten hinweg gesucht wird.

`showModules` und die Listen liegen in derselben zweistufigen Konfiguration wie alles andere:
`default.yaml` als Team-Vorgabe, `custom.yaml` als persoenliche Abweichung, die der
Feststellbutton schreibt. "Reset to Default" (REQ-072) setzt sie damit mit zurueck.
