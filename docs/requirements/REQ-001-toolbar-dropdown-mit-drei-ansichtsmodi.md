---
id: REQ-001
titel: Toolbar-Dropdown mit drei Ansichtsmodi
kategorie: modi
verifiziert: visuell
betrifft:
  - src/main/kotlin/de/example/gradletasksview/actions/GradleTaskViewModeActionGroup.kt
  - src/main/kotlin/de/example/gradletasksview/actions/GradleTaskViewModeAction.kt
  - src/main/kotlin/de/example/gradletasksview/toolwindow/GradleTasksExtendedPanel.kt
quelle: Plan.md, Abschnitt "Toolbar-Dropdown"
---

Die ToolWindow-Ansicht besitzt in ihrer eigenen Toolbar einen Dropdown mit den Modi
`Default`, `Sort` und `Nested`.

Der aktive Modus ist als Checkmark erkennbar, weil die drei Eintraege `ToggleAction`s in
einer Popup-`ActionGroup` sind. Ein Moduswechsel aktualisiert die Ansicht sofort, ohne
IDE-Neustart, ueber `GradleTaskViewRefresher.switchMode`.
