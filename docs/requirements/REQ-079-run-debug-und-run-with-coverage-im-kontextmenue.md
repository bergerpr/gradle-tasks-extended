---
id: REQ-079
titel: Run, Debug und Run with Coverage im Kontextmenue
kategorie: interaktion
verifiziert: manuell
betrifft:
  - src/main/kotlin/de/example/gradletasksview/actions/GradleTaskRunActionGroup.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTree.kt
  - src/main/kotlin/de/example/gradletasksview/execution/GradleTaskExecutor.kt
  - src/main/resources/META-INF/plugin.xml
quelle: OPEN-010, Angleichung an die originale Gradle-Ansicht
---

Das Kontextmenue bietet auf einem Task dieselben Ausfuehrungsarten an wie die originale
Gradle-Ansicht - Run, Debug, Run with Coverage - und darunter "Modify Run Configuration",
mit dem sich die Konfiguration vor dem Start anpassen laesst.

Fest verdrahtet ist davon nichts. `GradleTaskRunActionGroup` laeuft ueber die
Erweiterungsliste der registrierten Executors, filtert auf `Executor.isApplicable(project)`
und holt zu jedem die Kontext-Aktion der Plattform (`contextActionId`: `RunClass`,
`DebugClass`, `RunCoverage`, ...). Kommt eine Ausfuehrungsart hinzu, erscheint sie damit von
selbst, und Beschriftung und Icon stimmen mit dem Rest der IDE ueberein. Die originale
Ansicht baut ihr `ExternalSystemTaskMenu` genauso. "Modify Run Configuration" ist die
Plattform-Aktion `ExternalSystem.CreateRunConfiguration`, per `<reference>` eingebunden.

Damit diese Aktionen auf unserem Baum greifen, veroeffentlicht `uiDataSnapshot` die Selektion
als `Location.DATA_KEY` mit einer `ExternalSystemTaskLocation` - der Gradle-Producer der
Plattform liest genau diesen Schluessel (ADR-022). Sind mehrere Tasks desselben Gradle-Projekts
selektiert, landen sie zusammen in einer Konfiguration und laufen in einem Aufruf; ueber
Projektgrenzen hinweg wird keine Location geliefert, die Eintraege sind dann inaktiv. Auch das
entspricht der Originalansicht, deren `extractLocation` dieselbe Bedingung stellt.

Der Doppelklick bleibt der normale Run (REQ-010) und nimmt in `GradleTaskExecutor` denselben
Weg ueber eine temporaere Run-Konfiguration, damit beide Wege denselben Eintrag im
Konfigurations-Widget der Kopfzeile erzeugen.

Eigene Aktionen fuer Run, Debug und Coverage gibt es bewusst nicht mehr: Sie liefen ueber
`ExternalSystemUtil.runTask`, tauchten dadurch nie im Widget auf, und Coverage lief auf diesem
Weg ueberhaupt nicht (ADR-022). Das uebrige Kontextmenue - Kopieren, Hide, Expand/Collapse -
bleibt weiterhin plugin-eigen (REQ-011).
