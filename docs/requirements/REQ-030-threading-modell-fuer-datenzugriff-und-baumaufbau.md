---
id: REQ-030
titel: Threading-Modell fuer Datenzugriff und Baumaufbau
kategorie: plattform
verifiziert: unit
betrifft:
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTaskViewRefresher.kt
  - src/main/kotlin/de/example/gradletasksview/model/GradleTaskDataProvider.kt
quelle: Plan.md, Abschnitt "Threading"
---

Alle Lesezugriffe auf Projektdaten (`ProjectDataManager`, `DataNode`) laufen in
`readAction { }`; alle Mutationen des Tree-Modells laufen auf dem EDT
(`withContext(Dispatchers.EDT)`).

Verwendet werden die coroutine-basierten Top-Level-Funktionen, nicht das
`runReadAction()`-Legacy-Muster.

## Verifizierte API

`readAction { }` / `writeAction { }` sind suspend-Top-Level-Funktionen im Paket
`com.intellij.openapi.application`; `Dispatchers.EDT` stammt aus der Plattform-Erweiterung von
`kotlinx.coroutines`. Verifiziert in `platform/core-api/.../coroutines.kt`.
