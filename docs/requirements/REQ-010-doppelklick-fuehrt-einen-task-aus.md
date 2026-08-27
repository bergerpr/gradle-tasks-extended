---
id: REQ-010
titel: Doppelklick fuehrt einen Task aus
kategorie: interaktion
verifiziert: manuell
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/execution/GradleTaskExecutor.kt
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTree.kt
quelle: Plan.md, Abschnitt "Task-Ausfuehrung (Doppelklick)"
---

Ein Doppelklick auf einen Task-Knoten fuehrt ihn ueber die offizielle External-System-API
aus - niemals ueber eigene Prozess- oder Kommandozeilenaufrufe.

Ausgefuehrt wird ueber eine temporaere Run-Konfiguration, die beim `RunManager` angemeldet und
ausgewaehlt wird (ADR-022, das ADR-006 abloest). Der Lauf erscheint dadurch im
Konfigurations-Widget der Kopfzeile - genauso wie beim Start aus dem Kontextmenue (REQ-079)
und wie in der originalen Gradle-Ansicht.

Enter auf einer einzeln selektierten Task-Zeile loest dieselbe Ausfuehrung aus (REQ-087).

Der Doppelklick bleibt beim normalen Run-Executor: Die Executor-ID ist ein Parameter von
`GradleTaskExecutor.execute` mit `DefaultRunExecutor.EXECUTOR_ID` als Vorgabe, und der
Doppelklick uebergibt keine.

Bei Gradle ankommen muss der **qualifizierte** Taskpfad (`:app:buildImage`), sonst laesst
Gradle den gleichnamigen Task in jedem passenden Projekt laufen statt nur im gemeinten. Woher
die Modulzugehoerigkeit stammt, steht in ADR-015.

Zusammengesetzt wird der Pfad von der Plattform: Uebergeben werden der blanke Taskname und
das Verzeichnis des besitzenden Moduls, und `ExternalSystemTaskExecutionSettings` stellt dem
Namen den Projektpfad dieses Moduls voran. Ein hier schon qualifizierter Pfad wuerde ein
zweites Mal praefigiert und die Ausfuehrung scheitern lassen (ADR-018).
