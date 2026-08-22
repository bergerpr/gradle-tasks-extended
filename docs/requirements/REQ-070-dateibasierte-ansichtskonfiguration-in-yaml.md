---
id: REQ-070
titel: Dateibasierte Ansichtskonfiguration in YAML
kategorie: persistenz
verifiziert: unit
betrifft:
  - src/main/kotlin/de/example/gradletasksview/persistence/TaskViewConfigYaml.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/GradleTasksViewConfigStore.kt
  - gradle/gradle_tasks_extended/
offene_punkte:
  - OPEN-005
quelle: Erweiterung ueber Plan.md hinaus
---

Unterhalb von `gradle/gradle_tasks_extended/` liegen zwei flache YAML-Dateien:
`default.yaml` als versionierbare Team-Vorgabe und `custom.yaml` fuer persoenliche
Abweichungen, die beim manuellen Auf-/Zuklappen automatisch entstehen. `custom.yaml` hat
Vorrang.

`default.yaml` wird nur angelegt, wenn sie fehlt, und **niemals** ueberschrieben. Fehlerhaft
editierte Dateien fuehren zu einem leeren Zustand statt zu einem Absturz - es handelt sich um
eine von Menschen editierbare Systemgrenze.

Begruendung und Konsequenzen: ADR-003.
