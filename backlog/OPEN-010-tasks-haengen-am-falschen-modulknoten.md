---
id: OPEN-010
titel: Tasks haengen am falschen Modulknoten
prioritaet: hoch
betrifft_anforderung:
  - REQ-024
  - REQ-074
  - REQ-085
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/model/GradleTaskDataProvider.kt
quelle: Sandbox-Sitzung vom 2026-08-24, Log der Sonde zu REQ-085
---

Die Ansicht ordnet Tasks dem **falschen Gradle-Modul** zu: Ein Task aus einem Subprojekt
erscheint unter dessen **Elternmodul**, nicht unter dem Modul, in dem er definiert ist.

Nachgewiesen in der Sandbox mit dem Testprojekt `../gradle-tasks-extended-testproject`, indem
je ein `execute`-Verweis (REQ-085) auf einen Task gesetzt und die Warnung ausgewertet wurde,
die die tatsaechlich vorhandenen Pfade auflistet:

| Task, definiert in | Von der Ansicht gefuehrt als |
|---|---|
| `printLibGui` in `:lib` | `:printLibGui` |
| `printAppGui` in `:app` | `:printAppGui` |
| `legacyBuild` in `:legacy` | `:legacyBuild` |
| `buildImage` in `:app` **und** `:lib` | nur `:buildImage` |
| `printCoreReact` in `:app:core` | `:app:printCoreReact` **und** `:printCoreReact` |
| `printToolsCli` in `:tools:cli` | `:tools:printToolsCli` **und** `:printToolsCli` |

Das Muster ist durchgehend: Der Task landet unter dem Elternprojekt, und zusaetzlich fuehrt das
Wurzelprojekt offenbar den gesamten Build. 122 Tasks waren geladen, die Zuordnung ist also
nicht etwa leer, sondern systematisch verschoben.

## Warum das mehr ist als ein Schoenheitsfehler

- **Modulgliederung (REQ-074)** zeigt Tasks unter dem falschen Modul.
- **Ausfuehrung**: `externalProjectPath` stammt aus demselben `ModuleData`, Gradle wird also im
  Verzeichnis des Elternprojekts aufgerufen. Dass die Tasks trotzdem laufen, liegt daran, dass
  der blanke Name (ADR-018) in jedem passenden Subprojekt ausgefuehrt wird - genau das
  Verhalten, das REQ-010 eigentlich ausschliessen wollte.
- **Identitaet (REQ-024)**: Zwei gleichnamige Tasks in `:app` und `:lib` fallen auf denselben
  Pfad `:buildImage` zusammen.
- **Verweise (REQ-085)** muessen den verschobenen Pfad verwenden, was beim Schreiben der
  Konfiguration ueberrascht.

## Verdacht

`GradleTaskDataProvider.loadTasks` sammelt die Tasks je Modulknoten ueber
`ExternalSystemApiUtil.findAll(moduleNode, ProjectKeys.TASK)` (ADR-019). Die Annahme dahinter -
die direkten Task-Kinder eines Modulknotens sind die Tasks genau dieses Moduls - trifft in der
Zielplattform 2026.2 offenbar nicht zu. Zu pruefen ist, was `TaskData.getLinkedExternalProjectPath`
je Task liefert und ob daraus (statt aus dem tragenden Modulknoten) das richtige Modul
abzuleiten ist. ADR-015 und ADR-019 sind dann fortzuschreiben.
