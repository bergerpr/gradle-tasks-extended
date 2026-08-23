---
id: ADR-011
titel: targetComponent der Toolbar muss der Baum sein
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/toolwindow/GradleTasksExtendedPanel.kt
  - src/main/kotlin/de/berger/gradletasksview/actions/GradleTasksTreeContextActions.kt
quelle: Ursachenanalyse aus einer manuellen Testsitzung
---

`toolbar.targetComponent` ist auf den `JTree` gesetzt, nicht auf das umschliessende Panel.

Zuvor war das Panel eingetragen, und Expand All / Collapse All liefen wirkungslos ins Leere:
Die Aktionen ermitteln den Baum ueber `PlatformDataKeys.CONTEXT_COMPONENT`, das sich aus
`targetComponent` ableitet - mit dem Panel als Ziel lieferte das immer `null`.

Das Kontextmenue war nicht betroffen, weil `PopupHandler.installPopupMenu(this, group, place)`
mit `this` = dem Baum aufgerufen wird und das Ziel dort bereits korrekt setzt. Genau diese
Asymmetrie machte den Fehler schwer zu sehen: Dieselbe Aktion funktionierte im Kontextmenue
und versagte in der Toolbar.
