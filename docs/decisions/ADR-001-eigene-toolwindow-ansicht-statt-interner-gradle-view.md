---
id: ADR-001
titel: Eigene ToolWindow-Ansicht statt interner Gradle-View
betrifft:
  - src/main/resources/META-INF/plugin.xml
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTree.kt
  - src/main/kotlin/de/example/gradletasksview/model/GradleTaskDataProvider.kt
quelle: Plan.md, Abschnitt "Architekturentscheidung"
---

Das Plugin registriert ein eigenstaendiges ToolWindow mit eigenem `JTree`, statt die interne
Gradle-Ansicht zu erweitern.

Die Alternative waere direkter Zugriff auf JetBrains-Interna gewesen (`ExternalSystemNode`,
`ExternalSystemTasksTreeModel`, Paket `com.intellij.openapi.externalSystem.view`). Diese sind
nicht als stabile API deklariert und koennen ohne Deprecation-Zyklus verschwinden. Bei einem
privaten Werkzeug ohne Support-Team waere ein Nachziehen bei jedem IDE-Update
unverhaeltnismaessig.

Der Preis ist bewusst hoeher Aufwand in der Ersteinrichtung: Datenbeschaffung, Rendering,
Persistenz sowie Doppelklick, Kontextmenue und Copy muessen selbst gebaut werden - nicht per
Delegation an fremde Action-Instanzen, sondern ueber dieselben offiziellen Ausfuehrungs-APIs,
die auch die Originalansicht nutzt.

Der Gewinn ist volle Kontrolle ueber Icons, Sortierung, Verschachtelung und Persistenz -
genau das, was `Sort` und `Nested` brauchen.
