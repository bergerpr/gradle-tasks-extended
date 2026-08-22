---
id: ADR-007
titel: Keine UI-Automatisierung
betrifft:
  - src/test/kotlin/de/example/gradletasksview/
quelle: Plan.md, Abschnitt "UI-Automatisierung - bewusste Entscheidung"
---

Klick-Interaktionen auf Toolbar und Baumknoten werden **nicht** automatisiert getestet; es
gibt kein `remote-robot`-Setup.

Es handelt sich um ein privates Einzelprojekt ohne CI-Infrastruktur fuer UI-Tests. Diese
Faelle werden manuell geprueft. Die Entscheidung ist ausdruecklich dokumentiert, damit sie
nicht als unbeabsichtigte Luecke missverstanden wird.
