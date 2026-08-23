---
id: REQ-011
titel: Kontextmenue der Baumansicht
kategorie: interaktion
verifiziert: manuell
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/actions/GradleTasksTreeContextActions.kt
  - src/main/resources/META-INF/plugin.xml
quelle: Plan.md, Abschnitt "Kontextmenue"
---

Rechtsklick oeffnet ein eigenes Kontextmenue mit: den Ausfuehrungsarten und "Modify Run
Configuration" (REQ-079), Task-Namen kopieren, Task-Pfad kopieren, Ordnerpfad kopieren,
Hide bzw. Show Again (REQ-078), Expand All, Collapse All.

Die Gruppe gehoert dem Plugin; die Ausfuehrungseintraege sind die einzigen darin, die von der
Plattform stammen und per `<group class=...>` bzw. `<reference>` eingehaengt werden.

Die Aktionen lesen die Selektion direkt vom Baum, statt an eine fremde Action-Instanz zu
delegieren (Folge von ADR-001). Aktionen, die fuer den selektierten Knotentyp nicht sinnvoll
sind, werden in `update()` deaktiviert.

`Expand All` klappt den Baum in **einem** Aufruf vollstaendig auf, einschliesslich der Ebenen,
die dabei erst sichtbar werden. Es laeuft deshalb ueber eine Schleife mit jedes Mal neu
gelesenem `rowCount`: Eine Zaehlschleife ueber `0 until rowCount` wertet die Obergrenze nur
einmal aus und klappt dann pro Klick nur eine weitere Ebene auf.
