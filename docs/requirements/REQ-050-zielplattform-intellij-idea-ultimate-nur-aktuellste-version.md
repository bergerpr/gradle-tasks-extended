---
id: REQ-050
titel: Zielplattform IntelliJ IDEA Ultimate, nur aktuellste Version
kategorie: plattform
verifiziert: konstruktiv
betrifft:
  - build.gradle.kts
  - gradle/libs.versions.toml
  - gradle.properties
quelle: Plan.md, "Projekt-Rahmenbedingungen"
---

Gebaut wird gegen IntelliJ IDEA Ultimate (`IU`). Es gibt keine Anforderung an
Rueckwaertskompatibilitaet zu aelteren IntelliJ-Versionen oder anderen JetBrains-IDEs; ein
`untilBuild` wird bewusst nicht gesetzt.

Daraus folgt direkt ADR-008: Es gibt nie einen Grund, aus Kompatibilitaetsgruenden auf einer
veralteten API zu bleiben.

## Randbedingungen der Zielplattform

- Die "Unified IntelliJ IDEA"-Aenderung (ab 2025.3 keine getrennte Community-/Ultimate-
  Distribution mehr) aendert nichts an der Build-Konfiguration: `IntellijIdeaUltimate` bzw.
  `intellijIdeaUltimate(version)` existieren im IntelliJ Platform Gradle Plugin 2.20.0
  unveraendert als Zielplattform-Typ.
- Das IntelliJ Platform Gradle Plugin 2.20.0 benoetigt **Gradle >= 9.0** - nicht 8.13, wie
  aeltere Dokumentation vereinzelt behauptet. Der Projekt-Wrapper ist auf 9.7.1 fixiert.
- Die JVM-Toolchain folgt der Zielplattform: 2026.3 laeuft auf **Java 25**, das Projekt setzt
  daher `jvmToolchain(25)`. Weicht sie ab, meldet `./gradlew verifyPluginProjectConfiguration`
  das als "Java sourceCompatibility too low for target platform". Weil es keine
  Rueckwaertskompatibilitaet zu geben braucht, wird die Toolchain beim Anheben der IDE-Version
  einfach mitgezogen statt auf der aelteren LTS-Version gehalten.
- Die Kotlin-Version folgt der Linie, die die Ziel-IDE mitbringt: 2026.3 buendelt Kotlin
  **2.4.21** (nachlesbar in `KotlinVersionCurrentValue` der mitgelieferten Stdlib und im
  Manifest von `kotlin-reflect`), das Projekt uebersetzt mit 2.4.x. Das Plugin liefert wegen
  `kotlin.stdlib.default.dependency=false` keine eigene Stdlib mit, sondern nutzt die der IDE -
  eine aeltere Minor-Linie waere unnoetig, eine neuere riskant.

## Wechsel auf eine neue IDE-Version

Weil `pluginSinceBuild` immer auf die Zielplattform zeigt, wird mit jedem neuen IDE-Release
dreierlei zusammen angehoben: `intellijIdea` im Version Catalog, `pluginSinceBuild` in
`gradle.properties` und die Plugin-Version. Danach zeigen `compileKotlin` (neue
Deprecation-Warnungen) und `verifyPlugin`, was an Code nachzuziehen ist. Beim Wechsel auf
2026.3 waren das zwei neu abgekuendigte Aufrufe: der `DynamicBundle(String)`-Konstruktor
(Ersatz: `DynamicBundle(Class, String)`) und `FileEditorManager.openFile(file, focus)`, dessen
Ersatz `requestOpenFile` experimentell ist - geoeffnet wird deshalb ueber
`OpenFileDescriptor.navigate`.
