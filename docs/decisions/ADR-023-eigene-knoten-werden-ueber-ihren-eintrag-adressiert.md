---
id: ADR-023
titel: Eigene Knoten werden ueber ihren Eintrag adressiert, nicht ueber den ausgefuehrten Task
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/model/VirtualTaskNodes.kt
  - src/main/kotlin/de/berger/gradletasksview/model/GradleTaskDescriptor.kt
  - src/main/kotlin/de/berger/gradletasksview/model/NodeVisibility.kt
  - src/main/kotlin/de/berger/gradletasksview/persistence/TaskViewConfigKey.kt
  - src/main/kotlin/de/berger/gradletasksview/persistence/GradleTasksViewConfigStore.kt
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTreeNode.kt
quelle: Wunsch des Benutzers (REQ-085)
---

Ein ueber `execute` konfigurierter Knoten (REQ-085) wird ueber **seinen Eintrag** angesprochen -
Gruppenpfad plus Anzeigename, als eigene Schluesselform `TaskViewConfigKey.VirtualTask` und
eigenes Regelziel `VisibilityTarget.VirtualTask`. Der Name des Tasks, den er ausfuehrt, spielt
dabei keine Rolle.

## Warum nicht ueber den Tasknamen

Die naheliegende Umsetzung waere gewesen, den Knoten wie ein zweites Vorkommen desselben Tasks
zu behandeln. Dann waere aber die ausdrueckliche Anforderung unerfuellbar geblieben: Ein
`hidden` auf den Task haette beide Zeilen zugleich getroffen, weil `VisibilityTarget.Task` nur
den Namen vergleicht und `filterHiddenTasks` auf der Descriptor-Liste arbeitet - vor dem
Baumaufbau, wo es die Position im Baum noch gar nicht gibt.

Mit einem eigenen Ziel faellt das gewuenschte Verhalten ohne Sonderregel heraus:

- `tasks: buildImage: hidden: true` trifft nur echte Vorkommen.
- Der konfigurierte Knoten wird ueber seinen eigenen Eintrag aus- und wieder eingeblendet.
- Gruppen- und Modulregeln erfassen ihn weiterhin - er liegt sichtbar darin, und ein
  ausgeblendeter Ordner nimmt seinen Teilbaum mit (REQ-075).

## Was daran haengt

**Reihenfolge.** Die Aufweitung laeuft vor dem Ausblenden, auf der ungefilterten Taskliste.
Andernfalls waere der referenzierte Task bereits entfernt, wenn der Knoten entstehen soll, und
"am Originalplatz weg, hier sichtbar" waere nicht darstellbar.

**Identitaet.** Der Knoten braucht eine eigene `stableId`, sonst zeigten zwei Zeilen dieselbe,
und die Selektionswiederherstellung nach einem Rebuild (REQ-024) traefe die falsche. Sie ist
deshalb die des referenzierten Tasks, erweitert um Gruppenpfad und Anzeigename.

**Ausfuehrung bleibt unberuehrt.** `taskName` und `externalProjectPath` zeigen weiter auf den
echten Task; nur `displayName`, `groupName` und `stableId` weichen ab. Dadurch braucht das
`execution`-Paket keinen Sonderfall - `gradleTaskExecutionSettings` baut die Run-Konfiguration
aus genau diesen beiden Feldern (ADR-018).

**Anzeigename und Gradle-Name sind zwei Dinge.** Sie in einem Feld zusammenzulegen war keine
Option: Der blanke Gradle-Name ist der Ausfuehrungsschluessel, der Anzeigename ist frei
waehlbar. Sortierung, Speed-Search und Textfilter folgen dem Anzeigenamen, alles rund um die
Ausfuehrung dem Gradle-Namen.
