---
id: REQ-079
titel: Run, Debug und Run with Coverage im Kontextmenue
kategorie: interaktion
verifiziert: manuell
betrifft:
  - src/main/kotlin/de/example/gradletasksview/execution/GradleTaskExecutor.kt
  - src/main/kotlin/de/example/gradletasksview/actions/GradleTasksTreeContextActions.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTree.kt
  - src/main/resources/META-INF/plugin.xml
  - src/main/resources/messages/GradleTasksViewBundle.properties
quelle: OPEN-010, Angleichung an die originale Gradle-Ansicht
---

Das Kontextmenue bietet auf einem Task dieselben drei Ausfuehrungsarten an wie die originale
Gradle-Ansicht der IDE: `Run Task`, `Debug Task` und `Run Task with Coverage`, in dieser
Reihenfolge und jeweils mit dem Icon, das die Plattform dafuer verwendet.

Die Vorgabe bleibt der normale Run-Befehl: Ein Doppelklick fuehrt den Task weiterhin im
Run-Executor aus (REQ-010), und `GradleTaskExecutor.execute` hat genau dafuer seinen
Vorgabewert. Debug und Coverage kommen daneben, nicht an seine Stelle.

Unterschieden werden die drei ausschliesslich durch die Executor-ID, die an
`TaskExecutionSpec.withExecutorId` geht - `DefaultRunExecutor.EXECUTOR_ID`,
`DefaultDebugExecutor.EXECUTOR_ID` und `CoverageExecutor.EXECUTOR_ID`. Settings und Aufrufweg
sind identisch, insbesondere bleibt es beim blanken Tasknamen plus `externalProjectPath`
(ADR-018) und bei der `TaskExecutionSpec`-Ueberladung (ADR-006). Die Plattform macht es in
ihrer eigenen `RunExternalSystemTaskAction` genauso, weshalb die drei Aktionen sich eine
Basisklasse teilen und nur Beschriftung, Icon und ID mitbringen.

Woher der Debugger bzw. die Coverage-Messung kommt, entscheidet damit die Plattform: Die
Aktionen legen keine Run-Konfiguration selbst an und setzen keine JVM-Argumente.
`CoverageExecutor` liegt nicht im Plattform-Kern, sondern in einem eigenen Modul, das
Buildskript und `plugin.xml` deshalb ausdruecklich anfordern (ADR-021).

Alle drei sind deaktiviert, solange kein Task selektiert ist, und wirken - wie `Run Task`
bisher schon - auf den ersten selektierten Task.
