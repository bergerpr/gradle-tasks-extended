---
id: REQ-040
titel: Ausgewaehlter Modus wird projektbezogen persistiert
kategorie: persistenz
verifiziert: unit
betrifft:
  - src/main/kotlin/de/example/gradletasksview/persistence/GradleTaskViewSettings.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/GradleTaskViewSettingsState.kt
quelle: Plan.md, Abschnitt "Persistenz"
---

Der aktive Modus wird als `PersistentStateComponent` unter `gradleTasksView.xml` gespeichert,
gebunden an `Service.Level.PROJECT`.
