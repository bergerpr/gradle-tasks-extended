---
id: REQ-010
titel: Doppelklick fuehrt einen Task aus
kategorie: interaktion
verifiziert: manuell
betrifft:
  - src/main/kotlin/de/example/gradletasksview/execution/GradleTaskExecutor.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTree.kt
quelle: Plan.md, Abschnitt "Task-Ausfuehrung (Doppelklick)"
---

Ein Doppelklick auf einen Task-Knoten fuehrt ihn ueber die offizielle External-System-API
aus - niemals ueber eigene Prozess- oder Kommandozeilenaufrufe.

Verwendet wird ausschliesslich die `TaskExecutionSpec`-Ueberladung von
`ExternalSystemUtil.runTask`, siehe ADR-006.

Der Doppelklick bleibt beim normalen Run-Executor. Dass `GradleTaskExecutor.execute` seit
REQ-079 auch Debug und Coverage kann, aendert daran nichts: Die Executor-ID ist ein Parameter
mit `DefaultRunExecutor.EXECUTOR_ID` als Vorgabe, und der Doppelklick uebergibt keine.

Bei Gradle ankommen muss der **qualifizierte** Taskpfad (`:app:buildImage`), sonst laesst
Gradle den gleichnamigen Task in jedem passenden Projekt laufen statt nur im gemeinten. Woher
die Modulzugehoerigkeit stammt, steht in ADR-015.

Zusammengesetzt wird der Pfad von der Plattform: Uebergeben werden der blanke Taskname und
das Verzeichnis des besitzenden Moduls, und `ExternalSystemTaskExecutionSettings` stellt dem
Namen den Projektpfad dieses Moduls voran. Ein hier schon qualifizierter Pfad wuerde ein
zweites Mal praefigiert und die Ausfuehrung scheitern lassen (ADR-018).
