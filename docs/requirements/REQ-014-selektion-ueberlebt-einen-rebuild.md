---
id: REQ-014
titel: Selektion ueberlebt einen Rebuild
kategorie: interaktion
verifiziert: visuell
betrifft:
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTreeStateManager.kt
offene_punkte:
  - OPEN-003
quelle: Plan.md, Akzeptanzkriterien / Funktionserhalt
---

Vor jedem Rebuild wird die `stableId` des selektierten Tasks gesichert und danach im neuen
Baum wieder gesucht und selektiert. Sichtbare Namen allein reichen dafuer nicht aus - seit die
`stableId` den echten Gradle-Projektpfad enthaelt (REQ-024), trifft die Wiederherstellung auch
bei gleichnamigen Tasks in zwei Modulen den richtigen Knoten.
