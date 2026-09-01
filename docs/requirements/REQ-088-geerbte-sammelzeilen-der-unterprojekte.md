---
id: REQ-088
titel: Geerbte Sammelzeilen der Unterprojekte
kategorie: darstellung
verifiziert: unit
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/model/GradleTaskDataProvider.kt
  - src/main/kotlin/de/berger/gradletasksview/model/GradleTaskDescriptor.kt
  - src/main/kotlin/de/berger/gradletasksview/model/VirtualTaskNodes.kt
  - src/main/kotlin/de/berger/gradletasksview/persistence/TaskViewConfig.kt
  - src/main/kotlin/de/berger/gradletasksview/persistence/TaskViewConfigYaml.kt
  - src/main/kotlin/de/berger/gradletasksview/persistence/GradleTasksViewConfigStore.kt
  - src/main/kotlin/de/berger/gradletasksview/actions/GradleTaskShowInheritedTasksToggleAction.kt
  - src/main/kotlin/de/berger/gradletasksview/toolwindow/GradleTasksExtendedPanel.kt
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTaskViewRefresher.kt
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTreeCellRenderer.kt
  - src/main/resources/help/configuration.html
  - src/main/resources/help/example-config.yaml
quelle: Fehlermeldung des Benutzers ("globale Tasks werden nicht mehr dargestellt")
---

Ein Modulknoten fuehrt neben seinen eigenen Tasks auch die seiner Unterprojekte - jeden Namen,
den er nicht selbst vergibt, genau einmal. Die Plattform legt diese Kopien an und markiert sie
als **geerbt** (`TaskData.isInherited`); die Ansicht zeigt sie an, so wie die originale
Gradle-Ansicht es tut.

Ohne sie hat ein Projekt, das seine Plugins zentral fuer alle Unterprojekte setzt
(`subprojects { apply(...) }`, Convention-Plugin), auf Wurzelebene **gar keine** Tasks: Die
Tasks existieren dort nur ueber die Unterprojekte. Genau das war zwischen ADR-024 und dieser
Anforderung der Fall.

## Umschalter

Ein Feststellbutton in der Toolbar blendet sie aus und wieder ein - das Gegenstueck zu
`Show Inherited Tasks` der originalen Ansicht, samt deren Vorgabe: **eingeschaltet**. Der
Zustand wird persistiert und liegt in derselben zweistufigen Konfiguration wie die
Modulgliederung, denn er beschreibt, wie der Baum dauerhaft aussehen soll:

```yaml
settings:
  showInheritedTasks: false
```

Er ist damit anders gelagert als `Show Hidden` (REQ-078), das zum Aufraeumen gehoert und
bewusst nicht ueberlebt.

## Darstellung und Ausfuehrung

Eine geerbte Zeile steht **kursiv** und traegt einen Tooltip, der sagt, was das bedeutet. Grau
war nicht moeglich: Das kennzeichnet bereits ausgeblendete Knoten (REQ-078), und beides an
derselben Zeile waere nicht mehr auseinanderzuhalten.

Ausgefuehrt wird sie mit blankem Namen im Verzeichnis ihres Moduls - also in jedem
Unterprojekt, das einen solchen Task hat. Das ist keine Ausnahme in der Ausfuehrung, sondern
faellt aus ADR-018 heraus: Die Plattform baut die Run-Konfiguration aus `taskName` und
`externalProjectPath`, und beide zeigen bei einer geerbten Zeile auf das Modul, an dem sie
haengt. Es ist zugleich die Semantik, die die Zeile verspricht und die die originale Ansicht
ihr gibt.

Sonst verhaelt sie sich wie jeder andere Task: eigene `stableId` (der Modulpfad steckt darin,
also kollidiert sie nicht mit dem echten Task im Unterprojekt), adressierbar ueber den
`tasks:`-Namensraum der Konfiguration, ausblendbar, such- und filterbar.

Eine bewusste Einschraenkung: `Copy Task Path` liefert auch hier den qualifizierten Pfad
(`:app:build`), und der ist als Kommandozeile **nicht** gueltig - Gradle findet den Task in
`:app` ja nicht. Einen gueltigen qualifizierten Pfad gibt es fuer einen Sammelaufruf nicht; er
entspricht `gradlew build` im Verzeichnis des Moduls. Der Pfad benennt hier also den Ort der
Zeile im Baum, nach dem auch die Suche filtert.

## Kein Ziel fuer `execute`

Ein Verweis aus `execute` (REQ-085) loest **nur** auf echte Tasks auf. Eine geerbte Zeile ist
kein weiterer Ort, an dem ein Task definiert waere, sondern derselbe Task ein zweites Mal am
Elternmodul; ein blanker Verweis erzeugte sonst zusaetzlich zu der Zeile je Modul noch eine je
Elternebene. Aus demselben Grund nennt die Warnung ueber einen unaufloesbaren Verweis
(`referenceCandidates`) nur die echten Vorkommen.
