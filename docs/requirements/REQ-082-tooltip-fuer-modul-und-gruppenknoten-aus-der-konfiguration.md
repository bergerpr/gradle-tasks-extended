---
id: REQ-082
titel: Tooltip fuer Modul- und Gruppenknoten aus der Konfiguration
kategorie: darstellung
verifiziert: unit
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTreeCellRenderer.kt
  - src/main/kotlin/de/berger/gradletasksview/persistence/TaskViewConfig.kt
  - src/main/kotlin/de/berger/gradletasksview/persistence/TaskViewConfigYaml.kt
  - src/main/kotlin/de/berger/gradletasksview/persistence/GradleTasksViewConfigStore.kt
  - src/main/resources/help/configuration.html
  - src/main/resources/help/example-config.yaml
quelle: Wunsch des Benutzers
---

Modulknoten, Gruppenzeilen und virtuelle Ordner zeigen einen Tooltip **nur dann**, wenn fuer
sie in `default.yaml`/`custom.yaml` ein `description`-Eintrag hinterlegt ist. Ohne Eintrag
haben sie gar keinen Tooltip.

```yaml
groups:
  docker/gui:
    description: Everything around the GUI images
modules:
  ":app":
    description: The deployable application
```

Tasks sind davon nicht betroffen: Sie zeigen unveraendert die Beschreibung, die Gradle selbst
mitliefert - grau hinter dem Namen und als Tooltip (REQ-015) - und fragen die Konfiguration
dafuer gar nicht erst.

## Der Text steht an genau einer Stelle

`description` ist ein gewoehnliches Feld eines Knoteneintrags und wird wie `icon` aufgeloest
(Modulbezug vor modulunabhaengig, `custom.yaml` vor `default.yaml`, siehe REQ-076) - mit einer
Ausnahme: Innerhalb von `modes` wird es **nicht** ausgewertet und beim Lesen verworfen. Ein
Erlaeuterungstext beschreibt, was ein Knoten fachlich ist; das aendert sich nicht dadurch, dass
derselbe Knoten in `Sort` statt in `Nested` steht. Ihn pro Modus schreiben zu koennen haette
nur bedeutet, denselben Satz dreimal zu pflegen.

Ein leerer oder nur aus Leerzeichen bestehender Text (`description: "  "`) zaehlt als nicht
gesetzt - ein leerer Tooltip waere schlechter als gar keiner.

## Der Renderer muss den Tooltip in jedem Zweig setzen

Vorher stand `toolTipText` nur im Task-Zweig. Da `ColoredTreeCellRenderer` **eine einzige,
fuer alle Zeilen wiederverwendete Komponente** ist, blieb der zuletzt gesetzte Text stehen und
erschien anschliessend an Modul- und Gruppenknoten als deren Tooltip - genau das Verhalten, das
diese Anforderung abstellt. Jeder Zweig setzt den Wert deshalb ausdruecklich, auch auf `null`.
