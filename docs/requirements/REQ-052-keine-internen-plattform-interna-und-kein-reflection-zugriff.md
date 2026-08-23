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

## Nachgewiesen, nicht nur behauptet

`./gradlew verifyPlugin` prueft das - die Kategorie `INTERNAL_API_USAGES` laesst den Build
scheitern und meldet derzeit nichts. Der Verifier hat diese Zusage einmal widerlegt:
`ExternalSystemTaskLocation`, ueber die die Ansicht ihre Auswahl an die Ausfuehrungsaktionen
gab, traegt `@ApiStatus.Internal`, was im Quellcode nirgends sichtbar war. Seit dem eigenen
`RunConfigurationProducer` (ADR-022) ist sie verschwunden.

Reflection kommt nirgends vor, interne View-Klassen ebenso wenig.
