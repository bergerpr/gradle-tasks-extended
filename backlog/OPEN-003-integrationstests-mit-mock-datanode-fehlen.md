---
id: OPEN-003
titel: Integrationstests mit Mock-DataNode fehlen
prioritaet: mittel
betrifft_anforderung:
  - REQ-060
betrifft:
  - src/test/
  - build.gradle.kts
quelle: Plan.md, Abschnitt "Integration Tests"
---

Es existieren ausschliesslich Modelllogik-Unit-Tests (REQ-060).
`testFramework(TestFrameworkType.Platform)` ist im Buildskript bewusst auskommentiert.

Geplant war eine Abdeckung gegen einen Mock-`DataNode`-Baum statt eines echten Gradle-Syncs,
um Laufzeit und Flakiness gering zu halten. Ungedeckt sind damit: Vorhandensein von ToolWindow
und Dropdown, erwartete Baumstruktur je Modus, gesetzte Icons, Expand-Zustand ueber
simulierten Neustart, Verhalten im Modus `Default`, Aufruf von `runTask` mit den erwarteten
Einstellungen, Aktivierung der Kontextmenue-Aktionen, Filterung.

Seit der Modulgliederung kommt ein Bereich hinzu, der sich ohne Mock-`DataNode`-Baum
ueberhaupt nicht abdecken laesst: Die Modulzugehoerigkeit stammt aus dem Eltern-`ModuleData`-
Knoten (ADR-015), und genau dieses Zusammenspiel - Modulpfad, qualifizierter Taskpfad und die
daraus gebildete `stableId` - ist heute nur manuell in einer Sandbox-IDE pruefbar. Die
Baumform, die Filterregeln und das Konfigurationsformat sind dagegen reine Modelllogik und
bereits durch Unit-Tests gedeckt (REQ-074, REQ-075, REQ-076). Als Vorlage fuer die Fixture
dient die Struktur von `../gradle-tasks-extended-testproject`.

Wenn das kommt, gehoert es in ein eigenes Source-Set mit `testFramework(Platform)`, damit die
schnellen Unit-Tests unabhaengig davon bleiben.
