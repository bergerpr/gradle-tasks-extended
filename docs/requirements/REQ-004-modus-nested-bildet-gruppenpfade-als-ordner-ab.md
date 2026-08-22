---
id: REQ-004
titel: Modus Nested bildet Gruppenpfade als Ordner ab
kategorie: modi
verifiziert: unit
betrifft:
  - src/main/kotlin/de/example/gradletasksview/model/TaskTreeBuilder.kt
  - src/main/kotlin/de/example/gradletasksview/model/NestedFolderNode.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTreeModelBuilder.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTreeCellRenderer.kt
quelle: Plan.md, Abschnitt "Zielverhalten / Nested"
---

Im Modus `Nested` wird jede Komponente eines slash-getrennten Gruppenpfads zu einem
virtuellen Ordnerknoten: aus `docker/gui/react` wird `docker` -> `gui` -> `react`.

Innerhalb eines Ordners stehen **Tasks vor Unterordnern**. Ordner und Tasks werden jeweils
alphabetisch sortiert (siehe REQ-023).

Ist die Modulgliederung eingeschaltet (REQ-074), haengt der Ordnerbaum unter dem Modulknoten;
jedes Modul bekommt also seinen eigenen. Die Untermodule folgen darunter - dieselbe
Reihenfolge wie Tasks vor Unterordnern.

## Ordner ohne eigene Tasks werden zusammengefasst

Ein Ordner wird mit seinem Kind zu **einer** Zeile verschmolzen, wenn er keine eigenen Tasks
und genau ein Kind hat - transitiv, aus `docker` + `server` + `gui` wird also die Zeile
`docker/server/gui`. Sobald auf einer Zwischenebene ein Task auftaucht, spaltet sich der Pfad
wieder auf.

Beide Bedingungen sind noetig. Ein Ordner mit eigenen Tasks hat etwas zu zeigen. Ein Ordner mit
zwei Kindern liesse sich nicht verlustfrei zusammenziehen: `server/gui` und `server/cli` als
Geschwister wuerden den gemeinsamen Ast verdoppeln. Es ist dieselbe Regel, die die IDE in der
Projektansicht als "Compact Middle Packages" verwendet - Benutzer kennen das Verhalten also
schon, und es ist die einzige Bedingung, unter der zwei Ebenen verlustfrei zu einer Zeile
werden.

Zusammengefasst wird erst **nach** dem Sortieren. Da ein Knoten nur mit seinem einzigen Kind
verschmilzt, kann sich die Reihenfolge unter Geschwistern dadurch nicht aendern, und der
Komparator muss nicht mit mehrsegmentigen Namen umgehen.

Nicht zusammengefasst wird ueber die Modulgrenze hinweg: Ein Modulknoten ist kein Ordner. Modul
und Gruppe sind verschiedene Begriffe und verwenden mit `:` und `/` bewusst verschiedene
Trenner.

`NestedFolderNode.fullPath` bleibt der volle Pfad, `displayName` das letzte Segment daraus;
angezeigt wird `compactedName`. Damit greifen Konfigurationsschluessel und Icons unveraendert
weiter (REQ-071). Es gibt keinen Schalter dafuer - das ist das Verhalten, nicht eine Option.
