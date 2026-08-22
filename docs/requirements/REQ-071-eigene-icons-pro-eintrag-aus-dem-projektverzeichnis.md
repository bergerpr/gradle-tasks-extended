---
id: REQ-071
titel: Eigene Icons pro Eintrag aus dem Projektverzeichnis
kategorie: darstellung
verifiziert: visuell
betrifft:
  - src/main/kotlin/de/example/gradletasksview/persistence/GradleTasksViewConfigStore.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/TaskViewConfigYaml.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTreeCellRenderer.kt
quelle: Erweiterung ueber Plan.md hinaus
---

Ein YAML-Eintrag kann statt eines blossen Booleans eine Abbildung mit `expanded` und `icon`
sein. `icon` ist ein Dateiname relativ zu `gradle/gradle_tasks_extended/icons/` und ersetzt
das eingebaute Plugin-Icon - fuer Gruppen, Ordner und Tasks.

Schluessel ist bei Containern der Gruppen-/Ordnerpfad, bei Tasks der Taskname, bei
Modulknoten der Gradle-Projektpfad. Alle teilen sich einen Namensraum, ohne dass
Mehrdeutigkeit entsteht: Jeder Knotentyp schlaegt beim Rendern nur seinen eigenen Schluessel
nach, und Modulpfade sind am fuehrenden `:` erkennbar (ADR-014).

Ein Eintrag ohne Modulbezug gilt weiterhin fuer alle Module; steht derselbe Schluessel
zusaetzlich in einem Modulblock, gewinnt dort der modulbezogene (REQ-076).

Eine Geschwisterdatei `<name>_dark.<ext>` wird bei dunklen Themes bevorzugt. Eintraege, die
aus dem Icon-Verzeichnis herausfuehren, werden abgelehnt; fehlt eine Datei, greift das
eingebaute Icon und es wird gewarnt.
