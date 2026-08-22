---
id: REQ-012
titel: Copy fuer Task- und Ordnerknoten
kategorie: interaktion
verifiziert: manuell
betrifft:
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTree.kt
  - src/main/kotlin/de/example/gradletasksview/actions/GradleTasksTreeContextActions.kt
offene_punkte:
  - OPEN-002
quelle: Plan.md, Abschnitt "Copy"
---

Der Baum stellt ueber `UiDataProvider` einen `CopyProvider` bereit, sodass Ctrl+C fuer
Task-Knoten den Task-Pfad und fuer Ordnerknoten den vollen Ordnerpfad in die Zwischenablage
legt. Mehrfachselektion wird zeilenweise kopiert.
