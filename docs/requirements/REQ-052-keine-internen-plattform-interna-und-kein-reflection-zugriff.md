---
id: REQ-052
titel: Keine internen Plattform-Interna und kein Reflection-Zugriff
kategorie: plattform
verifiziert: konstruktiv
betrifft:
  - src/main/kotlin/de/example/gradletasksview/model/GradleTaskDataProvider.kt
  - src/main/kotlin/de/example/gradletasksview/execution/GradleTaskExecutor.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTree.kt
quelle: Plan.md, Abschnitt "Out of Scope"
---

Das Plugin greift nicht auf interne View-Klassen der Gradle-Ansicht zu (Paket
`com.intellij.openapi.externalSystem.view`) und benutzt kein Reflection, um an nicht
oeffentliche APIs zu gelangen. Grundlage ist ADR-001.
