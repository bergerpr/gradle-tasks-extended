---
id: REQ-024
titel: stableId als einzige Knotenidentitaet
kategorie: daten
verifiziert: unit
betrifft:
  - src/main/kotlin/de/example/gradletasksview/model/GradleTaskDescriptor.kt
  - src/main/kotlin/de/example/gradletasksview/model/GradleTaskDataProvider.kt
offene_punkte:
  - OPEN-001
quelle: Plan.md, Abschnitt "Datenmodell"
---

Die Identitaet eines Tasks ist immer
`externalProjectPath + "::" + gradleProjectPath + "::" + taskName`, niemals der sichtbare
Name allein. Selektionserhalt und Persistenzschluessel stuetzen sich ausschliesslich darauf.
