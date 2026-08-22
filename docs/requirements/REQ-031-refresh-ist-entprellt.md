---
id: REQ-031
titel: Refresh ist entprellt
kategorie: plattform
verifiziert: visuell
betrifft:
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTaskViewRefresher.kt
quelle: Plan.md, Abschnitt "Performance bei grossen Projekten"
---

Ein neu angeforderter Refresh bricht einen noch wartenden ab (300 ms Coalescing), statt bei
jedem Einzelevent einen vollstaendigen Rebuild auszuloesen.

Bei grossen Multi-Modul-Projekten ist das keine Optimierung, sondern Voraussetzung: Ein
Sync erzeugt mehrere Notification-Events kurz hintereinander.
