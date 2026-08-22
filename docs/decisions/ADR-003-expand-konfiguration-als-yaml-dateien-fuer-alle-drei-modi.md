---
id: ADR-003
titel: Expand-Konfiguration als YAML-Dateien fuer alle drei Modi
betrifft:
  - src/main/kotlin/de/example/gradletasksview/persistence/TaskViewConfigYaml.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/GradleTasksViewConfigStore.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTaskViewRefresher.kt
ersetzt_vorgabe: Plan.md, Abschnitt "Persistenzregeln" (Loeschen beim Wechsel auf Default)
quelle: Erweiterung ueber Plan.md hinaus
---

Der Auf-/Zuklappzustand liegt in zwei versionierbaren YAML-Dateien im Projekt statt in Mengen
innerhalb des `PersistentStateComponent`, und er gilt in **allen drei** Ansichtsmodi, nicht
nur in `Nested`.

Der Gewinn ist, dass eine Vorgabe im Team geteilt und im Repository versioniert werden kann -
`default.yaml` fuer alle, `custom.yaml` fuer persoenliche Abweichungen.

**Daraus folgt eine bewusste Abweichung:** Beim Wechsel in den Modus `Default` wird der
Zustand **nicht** mehr geloescht. Die urspruengliche Vorgabe sah genau das vor
(`expandedFolderPaths.clear()`, `knownFolderPaths.clear()`), was aber sinnvoll war, solange
der Zustand ausschliesslich zu `Nested` gehoerte. Seit er modusuebergreifend gilt, wuerde ein
Reset beim Umschalten die gerade erst nutzbar gewordene Konfiguration bei jedem Moduswechsel
zerstoeren. Zuruecksetzen geschieht seither ausschliesslich explizit ueber den
"Reset to Default"-Button (REQ-072).

**Nachtrag zur Modusunabhaengigkeit:** Dass die Konfiguration fuer alle drei Modi
gleichermassen gilt, trifft weiterhin auf den Expand-Zustand und die Icons zu - aber nicht
mehr auf alles in diesen Dateien. Die Exclude-/Include-Listen (REQ-075) sind ausdruecklich
**pro Modus** konfigurierbar, ein Modul kann also in `Nested` sichtbar und in `Sort`
ausgeblendet sein. Die Abweichung ist gewollt und soll nicht stillschweigend entstehen.

Das Format ist bewusst flach gehalten, ohne `groups:`-Wrapper, damit es von Hand lesbar
bleibt. Gebuendelt wird eine eigene SnakeYAML-Version; IntelliJ-Plugin-Klassenlader sind pro
Plugin isoliert, ein Versionskonflikt mit der Plattform ist daher ausgeschlossen.
