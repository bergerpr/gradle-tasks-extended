---
id: REQ-024
titel: stableId als einzige Knotenidentitaet
kategorie: daten
verifiziert: unit
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/model/GradleTaskDescriptor.kt
  - src/main/kotlin/de/berger/gradletasksview/model/GradleTaskDataProvider.kt
quelle: Plan.md, Abschnitt "Datenmodell"
---

Die Identitaet eines Tasks ist immer
`externalProjectPath + "::" + gradleProjectPath + "::" + taskName`, niemals der sichtbare
Name allein. Selektionserhalt und Persistenzschluessel stuetzen sich ausschliesslich darauf.

`gradleProjectPath` ist der echte Gradle-Projektpfad des besitzenden Moduls (`:`, `:app`,
`:app:core`), ermittelt aus dem Eltern-`ModuleData`-Knoten (ADR-015). Damit ist `buildImage`
in `:app` und in `:lib` zuverlaessig unterscheidbar; `externalProjectPath` haelt zusaetzlich
Composite Builds mit gleich benannten Modulen auseinander.

Ein ueber `execute` konfigurierter Knoten (REQ-085) haengt diese Identitaet um Gruppenpfad und
Anzeigenamen seines Eintrags: `<stableId des Tasks>@@<gruppenpfad>::<name>`. Er zeigt denselben
Task ein zweites Mal, und zwei Zeilen mit derselben Identitaet wuerden die
Selektionswiederherstellung nach einem Rebuild auf die falsche setzen.
