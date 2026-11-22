---
id: ADR-028
titel: Updates kommen aus einem eigenen Plugin-Repository ueber GitHub Releases
betrifft:
  - build.gradle.kts
  - .github/workflows/release.yml
  - docs/INSTALLATION.md
quelle: Meldung "Cannot update plugins" der IDE, Entscheidung des Benutzers, 2026-11-22
---

Neuere IDE-Fassungen fragen bei einem Plugin ohne bekannte Herkunft - jedes per "Install
Plugin from Disk" installierte -, woher Updates kommen sollen ("Cannot update plugins -
Unable to identify where ... were installed from"). Die Antwort ist ein **eigenes
Plugin-Repository**: eine URL mit einer XML, die die IDE auf neue Fassungen abfragt.

**Format.** Gelesen wird die Datei vom `RepositoryContentHandler` der Plattform: `<plugin>` mit
den Attributen `id`, `url`, `version`, darunter `idea-version since-build`, `name`, `vendor`,
`description`, `change-notes` (Tag-Namen gegen den Bytecode von 2026.3 geprueft). Die Aufgabe
`generateUpdatePluginsXml` erzeugt sie aus denselben Quellen wie `patchPluginXml`:
`gradle.properties` und `src/main/plugin-meta/`. Kein `until-build` (REQ-050).

**Ablage.** Die XML haengt als Datei an jedem GitHub-Release; die stabile Adresse
`.../releases/latest/download/updatePlugins.xml` leitet GitHub auf das neueste Release um. Die
Download-URL in der XML zeigt auf die ZIP genau dieses Release. So gibt es keinen zweiten Ort
(Pages, Zweig), der beim Release mitgepflegt werden muesste.

**Release-Workflow** (`.github/workflows/release.yml`): Tag `v*`, Abgleich Tag gegen
`pluginVersion`, dann `clean test buildPlugin verifyPlugin generateUpdatePluginsXml` und
`gh release create` mit dem automatischen `GITHUB_TOKEN`. Ohne `gradle/actions/setup-gradle`,
dessen Cache-Teil seit v6 proprietaer lizenziert ist; `setup-java` cached die Abhaengigkeiten.

**Ungeprueft bis zum ersten echten Release:** ob der Plugin-Manager der Umleitung von
`releases/latest/download` folgt. Faellt das durch, wandert die XML in den Zweig `main` und wird
ueber `raw.githubusercontent.com` ausgeliefert; Workflow und Aufgabe bleiben, nur der
Ablageort aendert sich.

Verworfen: Marketplace (REQ-051), ein privates Repo (Release-Dateien verlangen eine Anmeldung,
die eigene Repository-Abfrage der IDE hat keine), ein eigener Server (weiterer
oeffentlicher Dienst mit Pflege und Sicherung, fuer ein einzelnes Plugin ohne Gewinn).
