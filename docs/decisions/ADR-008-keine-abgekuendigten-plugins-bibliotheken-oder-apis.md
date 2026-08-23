---
id: ADR-008
titel: Keine abgekuendigten Plugins, Bibliotheken oder APIs
betrifft:
  - gradle/libs.versions.toml
  - build.gradle.kts
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTree.kt
quelle: Vorgabe des Projektinhabers
---

Abgekuendigtes wird nicht verwendet - es wird immer die aktuelle Variante gewaehlt. Das gilt
fuer Gradle-Plugin-IDs, Bibliotheken und Plattform-APIs gleichermassen. Da das Projekt nur die
neueste IDE unterstuetzt (REQ-050), gibt es nie einen Kompatibilitaetsgrund fuer das Gegenteil.

Wichtig bei der Pruefung: Der Kotlin-Compiler schweigt zu IntelliJs
`@ApiStatus.Obsolete`-Markierungen. Ein warnungsfreier Build ist deshalb **kein** Nachweis;
verifiziert wird gegen den Bytecode der Plattform-JARs. So wurde
`com.intellij.openapi.actionSystem.DataProvider` gefunden und durch
`UiDataProvider`/`DataSink` ersetzt (ADR-009), und so wurde
`com.github.ben-manes.versions` durch `io.github.ben-manes.versions` ersetzt.

Zwei bekannte, unvermeidbare Ausnahmen - offengelegt statt versteckt:
`ToolWindowFactory.isApplicable` und `isDoNotActivateOnStart` erscheinen im kompilierten
Bytecode nur, weil Kotlin fuer Default-Methoden eines Java-Interfaces automatisch
Ueberschreibungen erzeugt; und `nl.littlerobots.version-catalog-update` ruft intern das
abgekuendigte `Task.project` auf, was mit Gradle 10 zum Fehler wird. Das eigene Buildskript
erzeugt unter `--warning-mode all` null Deprecations.
