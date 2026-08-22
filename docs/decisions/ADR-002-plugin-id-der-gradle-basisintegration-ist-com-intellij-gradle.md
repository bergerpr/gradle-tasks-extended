---
id: ADR-002
titel: Plugin-ID der Gradle-Basisintegration ist com.intellij.gradle
betrifft:
  - build.gradle.kts
  - src/main/resources/META-INF/plugin.xml
ersetzt_vorgabe: Plan.md gab org.jetbrains.plugins.gradle vor
quelle: Manuelle Verifikation gegen IntelliJ IDEA 2026.2
---

Sowohl `bundledPlugin(...)` im Buildskript als auch `<depends>` in der `plugin.xml`
referenzieren **`com.intellij.gradle`**.

Die urspruengliche Annahme `org.jetbrains.plugins.gradle` ist ueberholt: Diese ID gehoert in
aktuellen Plattformversionen nur noch zum optionalen Sub-Plugin "Gradle for Java". Die
Basisintegration mit `GradleConstants` und dem Sync-Datenmodell wurde unter der neuen ID
ausgelagert.

## Nachweis

Ein Kompilierlauf gegen IntelliJ IDEA Ultimate 2026.2 (Build 262.8665.258) schlaegt mit
`Unresolved reference 'GradleConstants'` fehl, wenn `bundledPlugin("org.jetbrains.plugins.gradle")`
verwendet wird: Diese ID legt nur die Jars von "Gradle for Java" auf den Compile-Classpath,
nicht die der Basis-Integration. `GradleConstants` liegt im Modul `intellij.gradle`, das zum
Plugin `com.intellij.gradle` gehoert.

Die Java-Paketnamen der Gradle-Klassen selbst (`org.jetbrains.plugins.gradle.util.GradleConstants`)
sind von der ID-Umbenennung unberuehrt.
