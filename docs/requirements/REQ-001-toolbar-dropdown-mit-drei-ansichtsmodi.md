---
id: REQ-001
titel: Toolbar-Dropdown mit drei Ansichtsmodi
kategorie: modi
verifiziert: visuell
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/actions/GradleTaskViewModeActionGroup.kt
  - src/main/kotlin/de/berger/gradletasksview/actions/GradleTaskViewModeAction.kt
  - src/main/kotlin/de/berger/gradletasksview/actions/GradleTaskShowModulesToggleAction.kt
  - src/main/kotlin/de/berger/gradletasksview/toolwindow/GradleTasksExtendedPanel.kt
quelle: Plan.md, Abschnitt "Toolbar-Dropdown"; OPEN-007
---

Die ToolWindow-Ansicht besitzt in ihrer eigenen Toolbar einen Dropdown mit den Modi
`Default`, `Sort` und `Nested`.

Der aktive Modus ist als Checkmark erkennbar, weil die drei Eintraege `ToggleAction`s in
einer Popup-`ActionGroup` sind. Ein Moduswechsel aktualisiert die Ansicht sofort, ohne
IDE-Neustart, ueber `GradleTaskViewRefresher.switchMode`.

Die Auswahl schliesst das Dropdown. Die Eintraege sind Radioknoepfe, kein Satz unabhaengiger
Haken - nach der Wahl gibt es im Popup nichts mehr zu tun. Da die Plattform Toggle-Eintraege
von sich aus als Mehrfachauswahl behandelt und das Popup offen laesst, setzen sie dafuer
`Presentation.keepPopupOnPerform` auf `KeepPopupOnPerform.Never`.

Der Moduswechsel wirkt auf den Feststellbutton daneben zurueck: In `Default` ist die
Modulebene fest, der Button wird dort ausgegraut und angehakt dargestellt (REQ-074,
REQ-077).
