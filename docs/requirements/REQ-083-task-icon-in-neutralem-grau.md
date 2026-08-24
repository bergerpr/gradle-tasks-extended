---
id: REQ-083
titel: Task-Icon in neutralem Grau
kategorie: darstellung
verifiziert: konstruktiv
betrifft:
  - src/main/resources/icons/gradleTask.svg
  - src/main/resources/icons/gradleTask_dark.svg
quelle: Wunsch des Benutzers
---

Das Dreieck der Task-Blaetter ist grau, nicht gruen: `#6C707E` im hellen, `#CED0D6` im dunklen
Theme - dieselben beiden Farben, die Ordner-, Modul- und `Tasks`-Icon bereits verwenden. Das
Task-Icon war als einziges des Satzes eingefaerbt (`#3D8C40`/`#5FAD65`).

Zwei Gruende: Der Baum ist eine Liste zum Blaettern und kein Bedienfeld - ein gruener
Startknopf in jeder Zeile suggeriert eine Schaltflaeche, die es nicht gibt (ausgefuehrt wird
per Doppelklick oder Kontextmenue, REQ-010/REQ-079). Und die IDE haelt ihr Gruen fuer
tatsaechliche Ausfuehrungsaktionen frei; ein zweites Gruen daneben nimmt genau diesem Signal
seine Bedeutung.
