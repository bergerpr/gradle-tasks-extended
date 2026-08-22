---
id: REQ-073
titel: Abhaengigkeiten zentral im Gradle Version Catalog
kategorie: plattform
verifiziert: konstruktiv
betrifft:
  - gradle/libs.versions.toml
  - build.gradle.kts
  - gradle.properties
quelle: Erweiterung ueber Plan.md hinaus
---

Alle Versionen externer Abhaengigkeiten und Gradle-Plugins stehen in
`gradle/libs.versions.toml`; in `build.gradle.kts` ist keine Versionsnummer hartkodiert.

In `gradle.properties` bleibt nur, was keine Abhaengigkeit ist: `pluginGroup`, `pluginName`,
`pluginVersion`, `pluginSinceBuild` und `platformType` (ein Produktcode, keine Version).

Zwei reine Wartungswerkzeuge halten den Katalog aktuell und laufen nie als Teil von
`build`/`buildPlugin`: `dependencyUpdates` (Bericht) und `versionCatalogUpdate`
(Zurueckschreiben, benoetigt `--no-configuration-cache`).
