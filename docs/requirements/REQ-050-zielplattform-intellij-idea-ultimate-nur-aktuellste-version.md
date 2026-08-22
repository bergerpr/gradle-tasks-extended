---
id: REQ-050
titel: Zielplattform IntelliJ IDEA Ultimate, nur aktuellste Version
kategorie: plattform
verifiziert: konstruktiv
betrifft:
  - build.gradle.kts
  - gradle/libs.versions.toml
  - gradle.properties
quelle: Plan.md, "Projekt-Rahmenbedingungen"
---

Gebaut wird gegen IntelliJ IDEA Ultimate (`IU`). Es gibt keine Anforderung an
Rueckwaertskompatibilitaet zu aelteren IntelliJ-Versionen oder anderen JetBrains-IDEs; ein
`untilBuild` wird bewusst nicht gesetzt.

Daraus folgt direkt ADR-008: Es gibt nie einen Grund, aus Kompatibilitaetsgruenden auf einer
veralteten API zu bleiben.

## Randbedingungen der Zielplattform

- Die "Unified IntelliJ IDEA"-Aenderung (ab 2025.3 keine getrennte Community-/Ultimate-
  Distribution mehr) aendert nichts an der Build-Konfiguration: `IntellijIdeaUltimate` bzw.
  `intellijIdeaUltimate(version)` existieren im IntelliJ Platform Gradle Plugin 2.18.1
  unveraendert als Zielplattform-Typ.
- Das IntelliJ Platform Gradle Plugin 2.18.1 benoetigt **Gradle >= 9.0** - nicht 8.13, wie
  aeltere Dokumentation vereinzelt behauptet. Der Projekt-Wrapper ist auf 9.7.1 fixiert.
