---
id: REQ-004
titel: Modus Nested bildet Gruppenpfade als Ordner ab
kategorie: modi
verifiziert: unit
betrifft:
  - src/main/kotlin/de/example/gradletasksview/model/TaskTreeBuilder.kt
  - src/main/kotlin/de/example/gradletasksview/model/NestedFolderNode.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTreeModelBuilder.kt
quelle: Plan.md, Abschnitt "Zielverhalten / Nested"
---

Im Modus `Nested` wird jede Komponente eines slash-getrennten Gruppenpfads zu einem
virtuellen Ordnerknoten: aus `docker/gui/react` wird `docker` -> `gui` -> `react`.

Innerhalb eines Ordners stehen **Tasks vor Unterordnern**. Ordner und Tasks werden jeweils
alphabetisch sortiert (siehe REQ-023).

Ist die Modulgliederung eingeschaltet (REQ-074), haengt der Ordnerbaum unter dem Modulknoten;
jedes Modul bekommt also seinen eigenen. Die Untermodule folgen darunter - dieselbe
Reihenfolge wie Tasks vor Unterordnern.
