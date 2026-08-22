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

Abgedeckt sind die Beispiel-Gruppenpfade (`docker/gui`, `docker/gui/jsp`, `docker/gui/react`,
`docker/server/gui`) ueber die `printGroup-*`-Demotasks im Buildskript, in allen drei Modi
visuell geprueft.

Offen sind: identische Tasknamen in verschiedenen Subprojekten, Mehrmodul- und
Composite-Builds, mehrere gleichzeitig geoeffnete Projekte mit unterschiedlichem Modus,
IDE-Neustart als Persistenz-Verifikation, Gradle-Reimport bei aktivem `Nested`-Modus, Filter
aktiv und dann Moduswechsel, sowie mehrfach schnelles Umschalten als
Debouncing-Verifikation.
