---
id: REQ-084
titel: Zahnrad als Klappmenue mit Konfiguration und Reload
kategorie: interaktion
verifiziert: konstruktiv
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/actions/GradleTaskConfigActions.kt
  - src/main/kotlin/de/berger/gradletasksview/toolwindow/GradleTasksExtendedPanel.kt
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTaskViewRefresher.kt
  - src/main/kotlin/de/berger/gradletasksview/persistence/GradleTasksViewConfigStore.kt
  - src/main/resources/messages/GradleTasksViewBundle.properties
quelle: Wunsch des Benutzers
---

Das Zahnrad in der Toolbar (REQ-080) ist ein Klappmenue mit zwei Eintraegen:

- **Configuration** - oeffnet wie bisher die `default.yaml` des Projekts im Editor und legt sie
  vorher an, falls sie fehlt.
- **Reload** - liest `default.yaml`, `custom.yaml` und das Icon-Verzeichnis neu ein und baut
  die Ansicht damit neu auf.

`Reload` ist der Grund fuer das Menue. Neue Icons und geaenderte Konfiguration wurden bisher
erst wirksam, nachdem das Projekt entladen und neu geladen wurde. Beide Eintraege gehoeren zur
selben Sache und teilen sich deshalb einen Knopf, statt die Toolbar zu verbreitern.

## Tooltip des Reload-Eintrags

`Reload` traegt einen Tooltip, der in einem Satz sagt, was er tut. Er steht nicht in der
`description`: Die landet bei einem Menueeintrag in der Statuszeile
(`ActionMenuItem` ruft `ActionMenu.showDescriptionInStatusBar`), nicht am Mauszeiger. Das
Zahnrad oeffnet allerdings gar kein `JPopupMenu`, sondern ueber `ActionButton` ein
Listen-Popup (`PopupFactoryImpl.ActionGroupPopup`), und dessen Eintraege lesen ihren Tooltip
aus `ActionUtil.TOOLTIP_TEXT` der Presentation (`ActionPopupStep.getTooltipTextFor` ->
`ActionItem.getTooltip`). Genau dorthin schreibt die Aktion ihn - in `update`, weil das Popup
mit der frisch aktualisierten Presentation arbeitet, nicht mit der Vorlage. Der Schluessel ist
oeffentlich und traegt keine Abkuendigung (ADR-008).

## Warum erzwungen neu eingelesen wird

Der automatische Abgleich beim Refresh (`reloadIfChanged`) vergleicht die Zeitstempel der
beiden YAML-Dateien. Eine **neu ins `icons/`-Verzeichnis gelegte Datei** aendert keinen davon,
und das bereits zwischengespeicherte Icon bliebe bis zum naechsten IDE-Start das alte. `Reload`
ruft deshalb `forceReload()`: neu einlesen ohne Zeitstempelvergleich und den Icon-Cache
wegwerfen.

Laeuft in diesem Moment ein Schreibvorgang auf `custom.yaml`, wird nur der Icon-Cache geleert
und **nicht** neu eingelesen - der Zustand im Speicher ist dann neuer als der auf der Platte,
und ein Neueinlesen wuerde die gerade getaetigte Aenderung verwerfen.
