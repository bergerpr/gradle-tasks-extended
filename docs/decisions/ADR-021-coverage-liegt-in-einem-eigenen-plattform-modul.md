---
id: ADR-021
titel: Coverage liegt in einem eigenen Plattform-Modul
betrifft:
  - build.gradle.kts
  - src/main/resources/META-INF/plugin.xml
  - src/main/kotlin/de/example/gradletasksview/execution/GradleTaskExecutor.kt
  - src/main/kotlin/de/example/gradletasksview/actions/GradleTasksTreeContextActions.kt
quelle: REQ-079
---

`com.intellij.coverage.CoverageExecutor` - Quelle der Executor-ID `"Coverage"` fuer
`Run Task with Coverage` (REQ-079) - gehoert nicht zum Plattform-Kern, sondern zum Modul
`intellij.platform.coverage`. Dass dessen Jar direkt in `lib/` der IDE liegt und nicht in
einem Plugin-Verzeichnis, taeuscht: Auf dem Compile-Classpath steht es deswegen nicht.
`./gradlew compileKotlin` scheitert ohne weiteres Zutun an `Unresolved reference 'coverage'`.

Angefordert wird das Modul deshalb an zwei Stellen, die beide noetig sind:

- `bundledModule("intellij.platform.coverage")` im `intellijPlatform`-Block von
  `build.gradle.kts` - fuer die Kompilierung.
- `<dependencies><module name="intellij.platform.coverage"/></dependencies>` in `plugin.xml` -
  fuer die Laufzeit, weil Plugin-Klassenlader nur sehen, was ihr Deskriptor anfordert. Das
  `<dependencies>`-Element steht dabei neben den klassischen `<depends>`-Zeilen; beide Formen
  duerfen sich einen Deskriptor teilen, und `verifyPluginStructure` akzeptiert das.

`DefaultDebugExecutor` braucht nichts davon: Es liegt in `intellij.platform.debugger.jar`,
das ueber `com.intellij.modules.platform` ohnehin zur Verfuegung steht.

Die Alternative waere gewesen, die Konstante zu umgehen und das Literal `"Coverage"` an
`TaskExecutionSpec.withExecutorId` zu reichen - das haette beide Eintraege gespart. Dagegen
sprach, dass die ID dann nur noch als Zeichenkette im Code stuende und eine Umbenennung in
einer kuenftigen Plattformversion erst zur Laufzeit auffiele, waehrend der Verlust an
Ausdruckskraft nirgends dokumentiert waere. Die zwei Zeilen sind der ehrlichere Preis.
