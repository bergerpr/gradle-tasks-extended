---
id: REQ-012
titel: Copy fuer Task- und Ordnerknoten
kategorie: interaktion
verifiziert: manuell
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTree.kt
  - src/main/kotlin/de/berger/gradletasksview/actions/GradleTasksTreeContextActions.kt
quelle: Plan.md, Abschnitt "Copy"
---

Der Baum stellt ueber `UiDataProvider` einen `CopyProvider` bereit, sodass Ctrl+C fuer
Task-Knoten den Task-Pfad und fuer Ordnerknoten den vollen Ordnerpfad in die Zwischenablage
legt. Mehrfachselektion wird zeilenweise kopiert.

"Copy Task Path" liefert den qualifizierten Gradle-Pfad (`:app:buildImage`) und ist damit
etwas anderes als "Copy Task Name" (`buildImage`); der kopierte Pfad laesst sich unveraendert
auf der Kommandozeile verwenden.
