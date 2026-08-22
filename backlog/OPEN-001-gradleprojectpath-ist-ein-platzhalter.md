---
id: OPEN-001
titel: gradleProjectPath ist ein Platzhalter
prioritaet: hoch
betrifft_anforderung:
  - REQ-024
  - REQ-014
betrifft:
  - src/main/kotlin/de/example/gradletasksview/model/GradleTaskDataProvider.kt
quelle: Plan.md, Abschnitt "Edge Cases"
---

`GradleTaskDataProvider.toDescriptor` setzt `gradleProjectPath = data.linkedExternalProjectPath`
und damit identisch zu `externalProjectPath`.

Folge: `stableId` ist bei gleichem Tasknamen in zwei Subprojekten **nicht** eindeutig - genau
der Fall, den REQ-024 als geloest beschreibt. Zwei Tasks koennen sich damit gegenseitig bei
Selektionserhalt und Persistenz verdraengen.

Loesungsansatz: den echten Gradle-Projektpfad aus dem Eltern-`ModuleData`-Knoten des
Task-`DataNode` herleiten. Verifikation braucht ein echtes Multi-Modul- oder
Composite-Build-Testprojekt.

## Warum es ein Platzhalter ist

`TaskData` selbst besitzt **keinen** Gradle-Projektpfad der Form `:module:task`. Verfuegbar
ist nur `getLinkedExternalProjectPath()`, also der Pfad des verlinkten Wurzelprojekts. Der
echte Modulpfad muss ueber den Elternknoten `DataNode<ModuleData>` hergeleitet werden - genau
das steht noch aus.
