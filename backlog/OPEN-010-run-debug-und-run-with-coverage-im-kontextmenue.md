---
id: OPEN-010
titel: Kontextmenue bietet nur Run, nicht Debug und Run with Coverage
prioritaet: mittel
betrifft_anforderung:
  - REQ-010
  - REQ-011
betrifft:
  - src/main/kotlin/de/example/gradletasksview/execution/GradleTaskExecutor.kt
  - src/main/kotlin/de/example/gradletasksview/actions/GradleTasksTreeContextActions.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTree.kt
  - src/main/resources/META-INF/plugin.xml
  - src/main/resources/messages/GradleTasksViewBundle.properties
quelle: Wunsch des Benutzers, Angleichung an die originale Gradle-Ansicht
---

Die originale Gradle-Ansicht der IDE bietet beim Rechtsklick auf einen Task drei
Ausfuehrungsarten an: **Run**, **Debug** und **Run with Coverage**. Die eigene Ansicht kennt
bislang nur `Run Task` (REQ-011), weil `GradleTaskExecutor` den Executor fest auf
`DefaultRunExecutor.EXECUTOR_ID` setzt.

Der **Default bleibt der normale Run-Befehl**: Doppelklick (REQ-010) und die bestehende
Aktion `Run Task` fuehren den Task unveraendert im Run-Executor aus. Debug und Coverage
kommen als zwei zusaetzliche Kontextmenue-Eintraege daneben, nicht als Ersatz.

## Was dafuer noetig ist

- `GradleTaskExecutor.execute` bekommt die Executor-ID als Parameter (Vorgabewert
  `DefaultRunExecutor.EXECUTOR_ID`) und reicht sie an `TaskExecutionSpec.withExecutorId`
  weiter. Alles andere - `TaskExecutionSpec` statt der obsoleten `runTask`-Ueberladung
  (ADR-006), blanker Taskname plus `externalProjectPath` statt qualifiziertem Pfad (ADR-018) -
  bleibt unveraendert. Die Plattform macht es in
  `com.intellij.openapi.externalSystem.action.task.RunExternalSystemTaskAction` genauso: Sie
  variiert ausschliesslich die ueber `ExecutorRegistry.getExecutorById` aufgeloeste
  Executor-ID.
- Zwei weitere Aktionen in `GradleTasksTreeContextActions.kt`, im Aufbau identisch zu
  `GradleTaskExecuteAction` (Selektion direkt vom Baum lesen, in `update()` deaktivieren,
  wenn kein Task selektiert ist), dazu Eintraege in `plugin.xml` unterhalb von
  `GradleTasksExtended.ExecuteTask` und Texte im Message-Bundle.
- Executor-IDs, gegen die Plattform-Jars von 2026.2 geprueft:
  `com.intellij.execution.executors.DefaultDebugExecutor.EXECUTOR_ID` (`"Debug"`, liegt in
  `intellij.platform.debugger.jar`) und `com.intellij.coverage.CoverageExecutor.EXECUTOR_ID`
  (`"Coverage"`, liegt im Modul `intellij.platform.coverage`).

## Offene Fragen

- Das Coverage-Modul ist ein eigenes Plattform-Content-Modul. Ob dafuer ein
  `bundledModule("intellij.platform.coverage")` in `build.gradle.kts` und ein passender
  `<dependencies><module .../></dependencies>`-Eintrag in `plugin.xml` noetig werden, ist
  noch nicht geprueft. Falls der Konstantenzugriff zu teuer erkauft ist, kaeme das
  Literal `"Coverage"` in Frage - dann aber mit Begruendung als ADR, weil das Projekt sonst
  konsequent typisierte Plattform-Konstanten verwendet.
- Ob die Plattform `Run with Coverage` fuer einen beliebigen Gradle-Task sinnvoll ausfuehrt
  oder nur fuer Test-Tasks, ist offen. Sollte sie fuer andere Tasks nichts liefern, ist zu
  entscheiden, ob der Eintrag trotzdem immer erscheint (wie in der Originalansicht) oder
  in `update()` eingeschraenkt wird.
- Zu klaeren ist, ob die neuen Aktionen die Mehrfachselektion genauso behandeln wie
  `Run Task` heute.

## Nachweis

Manuell im Sandbox-Projekt `../gradle-tasks-extended-testproject`: Rechtsklick auf einen Task
zeigt drei Ausfuehrungseintraege; `Debug` haelt an einem Breakpoint in einem Build-Skript oder
Test an, `Run with Coverage` oeffnet das Coverage-Fenster, und Doppelklick startet weiterhin
den normalen Run. Der Fall gehoert damit in die Testmatrix aus OPEN-004.
