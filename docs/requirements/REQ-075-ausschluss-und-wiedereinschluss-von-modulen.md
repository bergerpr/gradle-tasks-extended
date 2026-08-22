---
id: REQ-075
titel: Ausschluss und Wiedereinschluss von Modulen
kategorie: daten
verifiziert: unit
betrifft:
  - src/main/kotlin/de/example/gradletasksview/model/ModuleFilter.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/TaskViewConfig.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/GradleTasksViewConfigStore.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTaskViewRefresher.kt
quelle: OPEN-005, Abschnitte "Vorrangregeln" und "Wirkung und Sichtbarkeit"
---

`exclude` blendet Module samt Untermodulen aus, `include` blendet einzelne Gruppenpfade eines
ausgeschlossenen Moduls gezielt wieder ein. Das Modul erscheint dann wieder, enthaelt aber nur
die eingeschlossenen Pfade. Beide Listen sind **pro Modus** konfigurierbar: Ein Modul kann in
`Nested` sichtbar und in `Sort` ausgeblendet sein.

`include` ist eine Abbildung Modulpfad -> Gruppenpfade, keine flache Liste. Ein Eintrag
`":legacy:docker/gui"` liesse sich nicht eindeutig zerlegen, da sowohl `:legacy` mit Gruppe
`docker/gui` als auch `:legacy:docker` mit Gruppe `gui` gueltige Gradle-Pfade sind.

## Vorrangregeln

- Ohne Eintrag ist alles eingeschlossen. Ein `include` ohne passendes `exclude` aendert nichts;
  es ist immer nur die Ausnahme zu einem Ausschluss.
- Bei Ueberschneidung gewinnt der laengere, spezifischere Modulpfad. Verglichen wird
  segmentweise, `:applet` ist also kein Untermodul von `:app`.
- Bei gleicher Spezifitaet gewinnt der Eintrag aus dem `modes`-Block, danach `include` vor
  `exclude`. Diese Reihenfolge ist eine bewusste Festlegung, siehe ADR-016.
- Listen aus `custom.yaml` **ergaenzen** die aus `default.yaml`, und modusabhaengige ergaenzen
  die modusuebergreifenden. Sie ersetzen einander nie, damit eine Team-Vorgabe nicht unbemerkt
  verschwinden kann.
- Ein eingeblendeter Gruppenpfad schliesst seine Unterpfade ein: `docker/gui` holt auch
  `docker/gui/jsp` zurueck, genau wie ein Ordner im Baum seine Unterordner enthaelt.

## Wirkung

Die Filterung ist reine Modelllogik und laeuft **vor** dem Baumaufbau. Dadurch entstehen keine
leeren Gruppen- und Ordnerknoten, die nach dem Ausschluss uebrig blieben.

Sie greift **immer**, auch bei ausgeschalteter Modulgliederung: Der Feststellbutton (REQ-074)
aendert die Darstellung, nicht den Inhalt. Zwei Begriffe, die sich getrennt erklaeren lassen.

Sie greift ausserdem **vor** der Textsuche (REQ-013), sodass die Suche keine Tasks aus
ausgeschlossenen Modulen findet - ein Treffer, den der sichtbare Baum nicht enthaelt, waere ein
Widerspruch.
