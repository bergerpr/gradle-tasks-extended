---
id: ADR-009
titel: UiDataProvider statt DataProvider
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTree.kt
quelle: Folge aus ADR-008
---

Der Baum implementiert `UiDataProvider.uiDataSnapshot(sink)` statt des aelteren
`DataProvider.getData(String)`, dessen Methode mit `@ApiStatus.Obsolete` markiert ist.

Der Unterschied ist nicht nur kosmetisch: `uiDataSnapshot` wird einmal pro Datenschnappschuss
auf dem EDT aufgerufen und befuellt eine `DataSink`, statt bei jedem einzelnen Schluessel
erneut nach einem String-Vergleich befragt zu werden.
