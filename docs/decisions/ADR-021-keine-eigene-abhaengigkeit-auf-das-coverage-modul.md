---
id: ADR-021
titel: Keine eigene Abhaengigkeit auf das Coverage-Modul
betrifft:
  - build.gradle.kts
  - src/main/resources/META-INF/plugin.xml
  - src/main/kotlin/de/berger/gradletasksview/actions/GradleTaskRunActionGroup.kt
quelle: REQ-079
---

Das Plugin fordert das Modul `intellij.platform.coverage` **nicht** an - weder als
`bundledModule` im Buildskript noch als `<dependencies><module .../>` in `plugin.xml`. Der
Eintrag "Run with Coverage" im Kontextmenue entsteht zur Laufzeit aus der Erweiterungsliste
der Executors (ADR-022), unser Code nennt `com.intellij.coverage.CoverageExecutor` nirgends.

## Warum das ueberhaupt einen Eintrag verdient

Kurzzeitig gab es diese Abhaengigkeit, weil die Ausfuehrungsarten als eigene Aktionen mit
fest verdrahteter Executor-ID gebaut waren. Dabei kam eine Eigenheit der Plattform ans Licht,
die beim naechsten Anlauf sonst erneut Zeit kostet:

`com.intellij.coverage.CoverageExecutor` gehoert nicht zum Plattform-Kern, sondern zum Modul
`intellij.platform.coverage`. Dass dessen Jar direkt in `lib/` der IDE liegt und nicht in
einem Plugin-Verzeichnis, taeuscht: Auf dem Compile-Classpath steht es deswegen nicht -
`./gradlew compileKotlin` scheitert an `Unresolved reference 'coverage'`. Wer die Konstante
wirklich braucht, muss beides eintragen: `bundledModule("intellij.platform.coverage")` fuer
die Kompilierung und `<dependencies><module name="intellij.platform.coverage"/></dependencies>`
fuer den Plugin-Klassenlader. Das `<dependencies>`-Element darf dabei neben den klassischen
`<depends>`-Zeilen stehen; `verifyPluginStructure` akzeptiert das.

`DefaultDebugExecutor` braucht nichts davon: Es liegt in `intellij.platform.debugger.jar`,
das ueber `com.intellij.modules.platform` ohnehin zur Verfuegung steht.
