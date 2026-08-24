---
id: REQ-015
titel: Task-Beschreibung als Tooltip und Zusatztext
kategorie: darstellung
verifiziert: visuell
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTreeCellRenderer.kt
quelle: Plan.md, Abschnitt "Ziel" (Erhalt der Tooltips)
---

Hat ein Task eine Gradle-Beschreibung, erscheint sie grau hinter dem Tasknamen und
zusaetzlich als Tooltip des Knotens.

Ein ueber `execute` konfigurierter Knoten (REQ-085) traegt einen selbst gewaehlten Namen und
zeigt deshalb seine eigene `description`, ohne sie die des ausgefuehrten Tasks.

Das gilt nur fuer Tasks. Modul-, Gruppen- und Ordnerknoten haben keine Beschreibung aus
Gradle; ihr Tooltip kommt ausschliesslich aus einem `description`-Eintrag der Konfiguration
und fehlt ohne ihn ganz (REQ-082).
