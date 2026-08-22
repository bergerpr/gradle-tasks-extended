---
id: REQ-060
titel: Unit-Tests der reinen Modelllogik
kategorie: test
verifiziert: unit
betrifft:
  - src/test/kotlin/de/example/gradletasksview/
quelle: Plan.md, Abschnitt "Unit Tests"
---

Die Modelllogik ist ohne IntelliJ-Platform-Testframework testbar: Pfadnormalisierung,
Sortierung, Nested-Baumaufbau, `stableId`, Einstellungen und das YAML-Format.

`testFramework(TestFrameworkType.Platform)` wird bewusst **nicht** aktiviert - es zieht einen
JUnit5-Launcher-Listener der IDE-Sandbox nach sich, der ohne vorbereitete Sandbox fehlschlaegt
und die schnellen Unit-Tests unbrauchbar machen wuerde.
