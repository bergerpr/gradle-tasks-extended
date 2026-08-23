---
id: ADR-018
titel: Taskname statt Taskpfad an die External-System-Settings
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/execution/GradleTaskExecutor.kt
  - src/main/kotlin/de/berger/gradletasksview/model/GradleTaskDataProvider.kt
ersetzt_vorgabe: ADR-015, Abschnitt "Folge fuer die Ausfuehrung"
quelle: OPEN-004, manuelle Testsitzung vom 2026-08-23
---

`ExternalSystemTaskExecutionSettings.taskNames` erwartet den **blanken** Tasknamen. Die
Plattform stellt ihm den Gradle-Projektpfad des Moduls voran, das unter
`externalProjectPath` liegt, und erst daraus entsteht der qualifizierte Pfad, den Gradle
schliesslich aufloest.

Uebergibt man dort bereits einen qualifizierten Pfad, wird zweimal praefigiert. In der
Sandbox belegt durch die Fehlermeldungen der Testsitzung:

```text
:app:core::app:core:printCoreReact   Cannot locate tasks ... empty segment
:app::app:printAppJsp                Cannot locate tasks ... empty segment
:legacy::legacy:legacyPrintGui       Cannot locate tasks ... empty segment
```

Das Praefix ist der Projektpfad des Moduls mit abschliessendem Doppelpunkt, beim
Wurzelprojekt also nur `":"`. Deshalb scheidet auch der naheliegende Gegenvorschlag aus,
statt des Modulverzeichnisses die Buildwurzel als `externalProjectPath` zu setzen und den
qualifizierten Pfad zu behalten: Daraus wuerde `"::app:buildImage"` - derselbe Fehler.

## Entscheidung

Uebergeben wird `descriptor.taskName` zusammen mit `descriptor.externalProjectPath`, dem
Verzeichnis des besitzenden Moduls. Die Plattform setzt daraus `:app:buildImage` zusammen.

Das erfuellt REQ-010 unveraendert - ausgefuehrt wird der qualifizierte Pfad, nur eben von der
Plattform gebildet. `descriptor.taskPath` bleibt bestehen und wird weiterhin fuer "Copy Path"
(REQ-012) und in der Anzeige verwendet; er ist damit kein Ausfuehrungsparameter mehr, sondern
reine Anzeige- und Kopierinformation.

## Warum das erst jetzt auffiel

Vor ADR-015 war `taskPath` mit `taskName` identisch, das Praefix der Plattform also die
einzige Qualifizierung - es funktionierte. Mit der Modulgliederung (REQ-074) bekam `taskPath`
seinen echten qualifizierten Wert, und ab da schlug **jede** Ausfuehrung fehl. Unit-Tests
konnten das nicht zeigen: Die Zusammensetzung passiert in der Plattform, nicht im eigenen
Code. Der Nachweis ist deshalb manuell (REQ-010) und Teil der Testmatrix in OPEN-004.
