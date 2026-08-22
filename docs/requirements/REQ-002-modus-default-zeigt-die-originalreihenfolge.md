---
id: REQ-002
titel: Modus Default zeigt die Originalreihenfolge
kategorie: modi
verifiziert: visuell
betrifft:
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTreeModelBuilder.kt
quelle: Plan.md, Abschnitt "Zielverhalten / Default"
---

Im Modus `Default` erscheinen Gruppen und Tasks in genau der Reihenfolge, in der External
System sie liefert - ohne eigene Sortierung und ohne virtuelle Ordnerknoten. Die Gruppierung
entsteht ueber eine `LinkedHashMap`, die die Einfuegereihenfolge erhaelt.
