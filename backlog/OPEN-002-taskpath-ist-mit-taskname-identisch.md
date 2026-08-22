---
id: OPEN-002
titel: taskPath ist mit taskName identisch
prioritaet: mittel
betrifft_anforderung:
  - REQ-012
  - REQ-010
betrifft:
  - src/main/kotlin/de/example/gradletasksview/model/GradleTaskDataProvider.kt
  - src/main/kotlin/de/example/gradletasksview/actions/GradleTasksTreeContextActions.kt
quelle: Beim Abgleich der Akzeptanzkriterien gefunden
---

`toDescriptor` setzt `taskPath = data.name`. Der Descriptor traegt damit keinen qualifizierten
Task-Pfad wie `:sub:build`.

Drei Auswirkungen:

- "Copy Task Path" kopiert exakt denselben String wie "Copy Task Name" - zwei
  Kontextmenue-Eintraege mit identischem Ergebnis.
- `GradleTaskExecutor` uebergibt den blanken Tasknamen zusammen mit dem Wurzel-Pfad des
  verlinkten Projekts. Fuer Subprojekt-Tasks in Mehrmodul-Builds ist die Ausfuehrung damit
  unverifiziert.
- Haengt eng mit OPEN-001 zusammen und sollte zusammen mit diesem geloest werden.
