---
id: OPEN-004
titel: Manuelle Testmatrix nur teilweise abgearbeitet
prioritaet: niedrig
betrifft_anforderung:
  - REQ-042
  - REQ-031
betrifft:
  - build.gradle.kts
quelle: Plan.md, Abschnitt "Manuelle Tests"
---

Seit dem Anlegen von `../gradle-tasks-extended-testproject` steht ein Multi-Projekt-Fixture
bereit: Wurzelprojekt mit eigenen Tasks, verschachteltes `:app:core`, gleichnamige Tasks in
`:app` und `:lib`, ueberlappende Gruppenpfade und ein Task ohne Gruppe. Damit ist die Matrix
erstmals ueberhaupt durchfuehrbar.

Abgedeckt sind bislang die Beispiel-Gruppenpfade (`docker/gui`, `docker/gui/jsp`,
`docker/gui/react`, `docker/server/gui`) in allen drei Modi, visuell geprueft - allerdings
noch gegen das alte Ein-Modul-Fixture im Plugin-Repository, nicht gegen das neue Projekt.

Offen sind: identische Tasknamen in verschiedenen Subprojekten und Mehrmodul-Builds - beides
jetzt mit dem neuen Fixture pruefbar -, Composite-Builds, mehrere gleichzeitig geoeffnete Projekte mit unterschiedlichem Modus,
IDE-Neustart als Persistenz-Verifikation, Gradle-Reimport bei aktivem `Nested`-Modus, Filter
aktiv und dann Moduswechsel, sowie mehrfach schnelles Umschalten als
Debouncing-Verifikation.
