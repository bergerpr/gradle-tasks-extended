---
id: REQ-051
titel: Vertrieb ueber ein eigenes Plugin-Repository auf GitHub, ohne Marketplace
kategorie: vertrieb
verifiziert: manuell
betrifft:
  - docs/INSTALLATION.md
  - build.gradle.kts
  - .github/workflows/release.yml
quelle: Plan.md, "Projekt-Rahmenbedingungen"; OPEN-011 (Update-Quelle, 2026-11-22)
---

Ein Tag `v<pluginVersion>` startet den Workflow `release.yml`: Er baut, testet und
verifiziert das Plugin und haengt die ZIP und eine `updatePlugins.xml` an ein GitHub-Release.
Nutzer tragen einmal

    https://github.com/bergerpr/gradle-tasks-extended/releases/latest/download/updatePlugins.xml

als eigenes Plugin-Repository in der IDE ein und bekommen neue Fassungen danach wie jedes
andere Plugin-Update. Ohne eingetragene Quelle meldet eine neuere IDE fuer ein per Datei
installiertes Plugin "Cannot update plugins" (ADR-028).

`./gradlew buildPlugin` und "Install Plugin from Disk" bleiben fuer lokale Staende moeglich.

Kein JetBrains-Marketplace-Vertrieb: kein `publishPlugin`, kein `signPlugin`, keine
Produktcode- oder Obfuskierungsschritte.
