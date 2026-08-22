---
id: ADR-006
titel: Task-Ausfuehrung nur ueber die TaskExecutionSpec-Ueberladung
betrifft:
  - src/main/kotlin/de/example/gradletasksview/execution/GradleTaskExecutor.kt
ersetzt_vorgabe: Plan.md skizzierte die 4-Parameter-Ueberladung
quelle: Plan.md, Abschnitt "Task-Ausfuehrung", verifiziert gegen 2026.2
---

`GradleTaskExecutor` benutzt ausschliesslich `ExternalSystemUtil.runTask(TaskExecutionSpec)`.

Die vier Ueberladungen mit `ExternalSystemTaskExecutionSettings` als erstem Parameter sind in
der aktuellen Plattform mit `@ApiStatus.Obsolete` markiert; die urspruengliche Skizze im Plan
verwendete genau eine davon. Ebenfalls zu meiden ist
`TaskExecutionSpec.create(project, systemId, executorId, settings)` - diese Variante ist
`@Deprecated` **und** `@ApiStatus.ScheduledForRemoval`.

Eigene Prozessausfuehrung oder Kommandozeilenaufrufe sind ausgeschlossen.

## Verifizierte API

`TaskExecutionSpec` und `TaskExecutionSpecBuilder` liegen als Kotlin-Quellen in
`com.intellij.openapi.externalSystem.util.task`. Der Aufbau erfolgt ueber
`TaskExecutionSpec.create()` gefolgt von
`.withProject(...).withSystemId(...).withExecutorId(...).withSettings(...).build()`.

`ExternalSystemTaskExecutionSettings` traegt `externalProjectPath`, `taskNames` und
`externalSystemIdString` als Kotlin-Properties mit Java-Gettern/Settern.
