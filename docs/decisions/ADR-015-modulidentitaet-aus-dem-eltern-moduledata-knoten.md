---
id: ADR-015
titel: Modulidentitaet aus dem Eltern-ModuleData-Knoten
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/model/GradleTaskDataProvider.kt
  - src/main/kotlin/de/berger/gradletasksview/model/GradleTaskDescriptor.kt
  - src/main/kotlin/de/berger/gradletasksview/execution/GradleTaskExecutor.kt
quelle: OPEN-005, Arbeitspaket AP-1
---

`ExternalSystemApiUtil.findAllRecursively(projectStructure, ProjectKeys.TASK)` flacht alle
Module in eine Liste ab. Die Modulzugehoerigkeit steht aber nur im Elternknoten, und der geht
dabei verloren. Sie wurde deshalb zunaechst je Task ueber
`taskNode.getParent(ModuleData::class.java)` nachgeschlagen.

**Dieser Nachschlag ist inzwischen ersetzt** - er lieferte nicht zuverlaessig ein Ergebnis,
siehe ADR-019. Gueltig bleibt der folgende Abschnitt: welche Eigenschaft des `ModuleData` den
Gradle-Projektpfad traegt.

## Welche Eigenschaft den Projektpfad traegt

`ModuleData.getId()` ist **nicht** der Gradle-Projektpfad. Der steht in frei gesetzten
Properties, die die Gradle-Integration als Kotlin-Erweiterungen im Paket
`org.jetbrains.plugins.gradle.util` anbietet:

- `ModuleData.gradlePathOrNull` - Pfad innerhalb des eigenen Builds.
- `ModuleData.gradleIdentityPathOrNull` - Pfad einschliesslich des Praefixes eines
  eingebundenen Builds, also ueber einen ganzen Composite Build hinweg eindeutig.

Verwendet wird `gradleIdentityPathOrNull` mit `gradlePathOrNull` als Rueckfall; faellt beides
aus, bleibt nur das Wurzelprojekt `":"` als vertretbare Annahme. Die nicht-nullable Varianten
`gradlePath`/`gradleIdentityPath` der Klasse `GradleModuleData` sind mit `@Deprecated` und
`@ApiStatus.ScheduledForRemoval` markiert ("Use gradlePathOrNull instead") und scheiden damit
aus (ADR-008). Die Wrapper-Klasse `GradleModuleData` selbst ist ausserdem
`@ApiStatus.Experimental`; die Erweiterungen auf `ModuleData` sind es nicht und werden deshalb
direkt benutzt.

Ebenfalls geprueft und verworfen: `ModuleData.directoryToRunTask`. Die Erweiterung ist
`internal` und aus Plugin-Code nicht aufloesbar. Sie faellt intern ohnehin auf
`linkedExternalProjectPath` zurueck, und genau das wird nun verwendet.

## Folge fuer die Ausfuehrung

Bei Gradle ankommen muss der qualifizierte Taskpfad (`:app:buildImage`), nicht der blanke
Taskname: Ein blanker Name laesst Gradle den gleichnamigen Task in *jedem* passenden Projekt
laufen - im Testprojekt sichtbar daran, dass `buildImage` dann zwei Ausgabezeilen erzeugt
(`:app` und `:lib`) statt einer.

Dass dieser Pfad dafuer in `taskNames` stehen muesse, war allerdings ein Fehlschluss: Die
Plattform bildet ihn selbst aus blankem Namen und Modulverzeichnis. Der urspruengliche
Wortlaut dieses Abschnitts ist damit ueberholt, siehe ADR-018.

Damit sind auch die frueheren Platzhalter aufgeloest: `gradleProjectPath` ist nicht mehr mit
`externalProjectPath` identisch, und `taskPath` nicht mehr mit `taskName`.
