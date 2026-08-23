---
id: ADR-022
titel: Ausfuehrung ueber eine temporaere Run-Konfiguration
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/execution/GradleTaskExecutor.kt
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTree.kt
  - src/main/kotlin/de/berger/gradletasksview/actions/GradleTaskRunActionGroup.kt
  - src/main/resources/META-INF/plugin.xml
ersetzt_vorgabe: ADR-006 (Ausfuehrung ueber ExternalSystemUtil.runTask)
quelle: REQ-079, verifiziert gegen die Plattform-Jars von 2026.2
---

Ausgefuehrt wird ueber eine **temporaere Run-Konfiguration**: Der Baum veroeffentlicht die
Selektion als `Location.DATA_KEY`, das Kontextmenue benutzt die Kontext-Aktionen der
Plattform, und der Doppelklick baut sich in `GradleTaskExecutor` denselben Weg nach -
`ExternalSystemUtil.createExternalSystemRunnerAndConfigurationSettings`,
`RunManager.setTemporaryConfiguration`, `ExecutionUtil.runConfiguration`,
`RunManager.setSelectedConfiguration`. `ExternalSystemUtil.runTask` wird nicht mehr benutzt.

## Warum nicht mehr `runTask`

Zwei Beobachtungen aus dem Bytecode von `intellij.platform.externalSystem.impl.jar`:

- **Kein Eintrag in der Kopfzeile.** `runTask(spec)` ruft `createExecutionEnvironment(...)`, das
  intern zwar eine `RunnerAndConfigurationSettings` erzeugt, sie aber nur in die
  `ExecutionEnvironment` legt. `RunManager.setTemporaryConfiguration` oder
  `setSelectedConfiguration` kommen darin nicht vor. Das Konfigurations-Widget im
  IDE-Kopf zeigt die **ausgewaehlte** Konfiguration des `RunManager` - eine nie angemeldete
  Konfiguration taucht dort folglich nicht auf, und es gibt keinen an sie gebundenen
  Rerun-Knopf.
- **Coverage lief ueberhaupt nicht.** `ExternalSystemUtil` haelt eine feste Zuordnung von
  Executor-ID zu Runner-ID, und die kennt nur `"Run"` und `"Debug"`. `registerRunnerId` wird
  in keinem mitgelieferten Plugin aufgerufen. Ohne Runner-ID liefert
  `createExecutionEnvironment` `null`, `runTask` schreibt eine Warnung ins Log und tut nichts.
  Coverage fuer Gradle haengt am `DefaultJavaCoverageRunner`, dessen `canRun` auf
  `executorId == "Coverage"` und `JavaCoverageEngine.isApplicableTo(...)` prueft - den waehlt
  die Plattform nur aus, wenn sie den Runner aus einer Run-Konfiguration aufloest.

Die originale Ansicht nimmt in `RunExternalSystemTaskAction.perform` denselben Weg wie wir
jetzt: `ConfigurationContext` -> vorhandene Konfiguration finden oder eine neue temporaer
anlegen -> `ExecutionUtil.runConfiguration` -> als ausgewaehlt setzen. Auf
`ExternalSystemUtil.runTask` faellt sie nur zurueck, wenn sich aus dem Kontext gar keine
Konfiguration ableiten laesst - genau das war bis dahin unser Normalfall.

## Der Datenschluessel ist die ganze Anbindung

`AbstractExternalSystemRunConfigurationProducer.getTaskSettingsFromContext` macht nichts
weiter als `context.getLocation()` auf `ExternalSystemTaskLocation` zu casten und daraus die
`ExternalTaskExecutionInfo` zu lesen. Ein `Location.DATA_KEY` im `uiDataSnapshot` des Baums
genuegt deshalb, damit die gesamte Plattform-Maschinerie auf unserer Ansicht arbeitet: die
Kontext-Aktionen der Executors, "Modify Run Configuration" und die Wiedererkennung einer
bereits bestehenden Konfiguration.

Veroeffentlicht wird ueber `DataSink.lazy`, weil `ExternalSystemTaskLocation.create` das
`PsiDirectory` zum Projektpfad nachschlaegt und das nur laufen soll, wenn eine Aktion die
Location wirklich anfragt. `ExternalProjectsViewImpl` haelt es genauso.

### Kein `ExternalSystemTaskLocation` - eigener Producer

Zuerst veroeffentlichte der Baum die Auswahl als `ExternalSystemTaskLocation`, die einzige
Form, die der Gradle-Producer der Plattform aus dem `ConfigurationContext` liest. Der Plugin
Verifier hat gemeldet, was eine Sichtpruefung der Klassendatei nicht hergab: Diese Klasse
traegt `@ApiStatus.Internal` (ADR-008, REQ-052).

Aufgeloest ueber einen **eigenen `RunConfigurationProducer`**. Er liest die Auswahl aus einem
plugin-eigenen Datenschluessel (`SELECTED_GRADLE_TASKS`) und fuellt damit eine
`ExternalSystemRunConfiguration` - alle drei Bausteine (`LazyRunConfigurationProducer`, die
Konfigurationsklasse und `AbstractExternalSystemTaskConfigurationType`) sind oeffentlich.

Eine `Location` veroeffentlicht der Baum weiterhin, jetzt aber als gewoehnliche
`PsiLocation` auf das Modulverzeichnis. Der Inhalt interessiert niemanden mehr; gebraucht wird
sie, weil `ConfigurationContext.createConfiguration` ohne Location gar keine Producer fragt -
im Bytecode nachlesbar an der Stelle, an der `getLocation()` auf `null` geprueft wird, bevor
`PreferredProducerFind` ueberhaupt aufgerufen wird.

Verworfen wurde der Rueckweg zu eigenen Aktionen ueber `ExternalSystemUtil.runTask`: Er kostet
den Eintrag im Konfigurations-Widget und laesst Coverage gar nicht laufen - genau die Gruende,
aus denen dieser ADR entstanden ist.

Seither meldet `./gradlew verifyPlugin` keine interne API mehr, und die Kategorie
`INTERNAL_API_USAGES` laesst den Build wieder scheitern.
