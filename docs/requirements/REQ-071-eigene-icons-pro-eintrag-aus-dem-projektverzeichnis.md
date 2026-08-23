---
id: REQ-071
titel: Eigene Icons pro Eintrag aus dem Projektverzeichnis
kategorie: darstellung
verifiziert: visuell
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/persistence/GradleTasksViewConfigStore.kt
  - src/main/kotlin/de/berger/gradletasksview/persistence/TaskViewConfigYaml.kt
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTreeCellRenderer.kt
quelle: Erweiterung ueber Plan.md hinaus
---

Ein YAML-Eintrag kann statt eines blossen Booleans eine Abbildung mit `expanded`, `icon`,
`hidden` und `modes` sein. `icon` ist ein Dateiname relativ zu
`gradle/gradle_tasks_extended/icons/` und ersetzt das eingebaute Plugin-Icon - fuer Gruppen,
Ordner und Tasks.

Schluessel ist bei Containern der Gruppen-/Ordnerpfad, bei Tasks der Taskname, bei
Modulknoten der Gradle-Projektpfad. Jede dieser Arten hat ihren eigenen Namensraum
(`groups`, `tasks`, `modules`), sodass ein Eintrag nie einen gleichnamigen Knoten anderer Art
mittrifft (ADR-020).

Ein Eintrag unter `groups`/`tasks` gilt fuer alle Module; steht derselbe Schluessel
zusaetzlich im `groups`/`tasks` eines Modulblocks, gewinnt dort der modulbezogene (REQ-076).

Werden Ordner ohne eigene Tasks zu einer Zeile zusammengefasst (REQ-004), bleibt der
Schluessel der volle Pfad - `docker/server/gui` greift also auf der zusammengefassten Zeile.
Ein Eintrag fuer einen dabei weggefallenen Zwischenknoten wie `docker/server` findet dagegen
keinen Knoten mehr und bleibt wirkungslos. Das ist bewusst in Kauf genommen: Anders liesse
sich der Schluesselraum nicht eindeutig halten, und ein Zwischenknoten ohne eigene Tasks ist
genau der, dessen Icon oder Zuklappzustand niemanden interessiert.

Eine Geschwisterdatei `<name>_dark.<ext>` wird bei dunklen Themes bevorzugt. Eintraege, die
aus dem Icon-Verzeichnis herausfuehren, werden abgelehnt; fehlt eine Datei, greift das
eingebaute Icon und es wird gewarnt.
