---
id: REQ-052
titel: Keine internen Plattform-Interna und kein Reflection-Zugriff
kategorie: plattform
verifiziert: konstruktiv
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/model/GradleTaskDataProvider.kt
  - src/main/kotlin/de/berger/gradletasksview/execution/GradleTaskExecutor.kt
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTree.kt
quelle: Plan.md, Abschnitt "Out of Scope"
---

Das Plugin greift nicht auf interne View-Klassen der Gradle-Ansicht zu (Paket
`com.intellij.openapi.externalSystem.view`) und benutzt kein Reflection, um an nicht
oeffentliche APIs zu gelangen. Grundlage ist ADR-001.

## Eine offengelegte Ausnahme

`ExternalSystemTaskLocation` liegt zwar nicht im View-Paket, ist aber mit
`@ApiStatus.Internal` markiert - gemeldet vom Plugin Verifier, nicht vom Compiler. Die Klasse
ist der einzige Typ, den der Gradle-Producer der Plattform aus dem `ConfigurationContext`
annimmt; ohne sie gaebe es im Kontextmenue weder Debug noch Coverage noch "Modify Run
Configuration" (REQ-079). Begruendung und verworfene Alternativen stehen in ADR-022.

Reflection kommt weiterhin nirgends vor, und interne View-Klassen ebenso wenig.
