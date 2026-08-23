---
id: ADR-014
titel: Format der Modulkonfiguration in der YAML
betrifft:
  - src/main/kotlin/de/example/gradletasksview/persistence/TaskViewConfigYaml.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/TaskViewConfig.kt
  - src/main/kotlin/de/example/gradletasksview/util/GradleModulePathUtil.kt
quelle: OPEN-005, Abschnitt "Entschieden"
---

**Abgeloest durch ADR-020.** Die oberste Ebene hat jetzt vier feste Schluessel
(`settings`, `groups`, `tasks`, `modules`) statt dreier Sorten, die an Konventionen zu
erkennen waren. Der Grund steht dort: Gruppen und Tasks teilten sich hier einen
Namensraum, was mit `hidden` zu stillem Datenverlust gefuehrt haette. Was bleibt, ist die
Entscheidung, Modulschluessel absolut zu halten.

Drei Formentscheidungen fuer die Erweiterung von `default.yaml`/`custom.yaml` um Modulbezug
und Ansichtseinstellungen (REQ-076). Alle drei haben denselben Zweck: bestehende Dateien
sollen unveraendert weiterlaufen, denn das Plugin schreibt `default.yaml` bewusst nie um
(REQ-070) - eine Migration scheidet damit aus.

## Ein reservierter Schluessel statt eines Wrappers

Die Ansichtseinstellungen (`showModules`, `exclude`, `include`, `modes`) stehen unter einem
reservierten Schluessel auf oberster Ebene; die Knoteneintraege bleiben flach daneben. Haette
man stattdessen alle Knoteneintraege unter einen `nodes:`-Wrapper geschoben, waere jede
bestehende Datei auf einen Schlag wirkungslos geworden.

Der Schluessel heisst **`gradle_tasks_extended`**, wie das Konfigurationsverzeichnis, und nicht
etwa `view`: Eine echte Gradle-Gruppe dieses Namens ist praktisch ausgeschlossen, eine
Kollision mit einem Knoteneintrag damit ebenfalls.

## Der fuehrende Doppelpunkt unterscheidet Modul von Gruppe

Ein Schluessel mit fuehrendem `:` ist ein Modulblock, jeder andere ein Gruppenpfad oder
Taskname. Das Merkmal ist nicht erfunden, sondern ergibt sich aus Gradle selbst: Projektpfade
beginnen immer mit `:`, Gruppenpfade nie. `":app"` ist damit zweifelsfrei ein Modul,
`docker/gui/jsp` zweifelsfrei ein Knoteneintrag - ohne dass die Datei eine zusaetzliche
Verschachtelungsebene braucht. Das Wurzelprojekt heisst konsequent `":"`.

Innerhalb eines Modulblocks sind `expanded` und `icon` reserviert und konfigurieren den
Modulknoten selbst; jeder andere Schluessel ist ein Gruppenpfad dieses Moduls. Modulknoten sind
damit genauso konfigurierbar wie Gruppen.

## Modulschluessel bleiben absolut

`":app:core"` steht auf oberster Ebene, nicht innerhalb des `":app"`-Blocks - obwohl die
*Anzeige* verschachtelt ist. Waere es umgekehrt, liesse sich innerhalb eines Modulblocks nicht
mehr entscheiden, ob ein Unterschluessel ein Untermodul oder ein Gruppenpfad ist. Beides sind
gueltige Namen, und die Datei wird von Hand gepflegt.

Anzeigehierarchie und Konfigurationsschluessel sind also bewusst entkoppelt. Der Preis ist,
dass die Datei die Baumform nicht nachzeichnet; der Gewinn ist, dass jeder Schluessel fuer sich
allein eindeutig bleibt.

## Warum `include` eine Abbildung ist

Aus demselben Grund ist `include` eine Abbildung Modulpfad -> Gruppenpfade und keine flache
Liste: Ein Eintrag `":legacy:docker/gui"` liesse sich nicht eindeutig zerlegen - `:legacy` mit
Gruppe `docker/gui` oder `:legacy:docker` mit Gruppe `gui`? Die Abbildung passt auch inhaltlich,
denn `include` ist immer die Ausnahme zu einem ausgeschlossenen Modul (REQ-075).
