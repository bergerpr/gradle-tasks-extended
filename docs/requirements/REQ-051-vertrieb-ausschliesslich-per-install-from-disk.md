---
id: REQ-051
titel: Vertrieb ausschliesslich per Install from Disk
kategorie: vertrieb
verifiziert: manuell
betrifft:
  - docs/INSTALLATION.md
  - build.gradle.kts
quelle: Plan.md, "Projekt-Rahmenbedingungen"
---

`./gradlew buildPlugin` erzeugt eine ZIP unter `build/distributions/`, die ueber
"Install Plugin from Disk" installiert wird.

Kein JetBrains-Marketplace-Vertrieb: kein `publishPlugin`, kein `signPlugin`, keine
Produktcode- oder Obfuskierungsschritte.
