---
id: REQ-070
titel: Dateibasierte Ansichtskonfiguration in YAML
kategorie: persistenz
verifiziert: unit
betrifft:
  - src/main/kotlin/de/example/gradletasksview/persistence/TaskViewConfigYaml.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/GradleTasksViewConfigStore.kt
  - gradle/gradle_tasks_extended/
quelle: Erweiterung ueber Plan.md hinaus
---

Unterhalb von `gradle/gradle_tasks_extended/` liegen zwei YAML-Dateien:
`default.yaml` als versionierbare Team-Vorgabe und `custom.yaml` fuer persoenliche
Abweichungen, die beim manuellen Auf-/Zuklappen automatisch entstehen. `custom.yaml` hat
Vorrang.

`default.yaml` wird nur angelegt, wenn sie fehlt, und **niemals** ueberschrieben. Fehlerhaft
editierte Dateien fuehren zu einem leeren Zustand statt zu einem Absturz - es handelt sich um
eine von Menschen editierbare Systemgrenze.

Die oberste Ebene hat genau vier feste Schluessel: `settings`, `groups`, `tasks` und
`modules` (REQ-076). `custom.yaml` haelt dabei nur **Abweichungen** fest: Ein Knoten wird nur
geschrieben, wenn sein Zustand von dem abweicht, was ohne diesen Eintrag gaelte, und
verschwindet wieder, sobald er ihm entspricht.

Begruendung und Konsequenzen: ADR-003 und ADR-020.
