---
id: OPEN-004
titel: Manuelle Testmatrix nur teilweise abgearbeitet
prioritaet: niedrig
betrifft_anforderung:
  - REQ-042
  - REQ-031
  - REQ-074
  - REQ-075
  - REQ-077
betrifft:
  - build.gradle.kts
quelle: Plan.md, Abschnitt "Manuelle Tests"; OPEN-007
---

Seit dem Anlegen von `../gradle-tasks-extended-testproject` steht ein Multi-Projekt-Fixture
bereit: Wurzelprojekt mit eigenen Tasks, verschachteltes `:app:core`, gleichnamige Tasks in
`:app` und `:lib`, ueberlappende Gruppenpfade und ein Task ohne Gruppe. Damit ist die Matrix
erstmals ueberhaupt durchfuehrbar.

Abgedeckt sind bislang die Beispiel-Gruppenpfade (`docker/gui`, `docker/gui/jsp`,
`docker/gui/react`, `docker/server/gui`) in allen drei Modi, visuell geprueft - allerdings
noch gegen das alte Ein-Modul-Fixture im Plugin-Repository, nicht gegen das neue Projekt.

Offen sind: identische Tasknamen in verschiedenen Subprojekten und Mehrmodul-Builds - beides
jetzt mit dem neuen Fixture pruefbar -, Composite-Builds, mehrere gleichzeitig geoeffnete Projekte mit unterschiedlichem Modus,
IDE-Neustart als Persistenz-Verifikation, Gradle-Reimport bei aktivem `Nested`-Modus, Filter
aktiv und dann Moduswechsel, sowie mehrfach schnelles Umschalten als
Debouncing-Verifikation.

## Mit der Modulgliederung hinzugekommen

Die Modelllogik dahinter ist durch Unit-Tests abgedeckt (REQ-074, REQ-075); was nur in einer
laufenden IDE sichtbar wird, ist es nicht. Die `default.yaml` des Testprojekts ist bereits so
eingerichtet, dass diese Faelle beim Oeffnen sofort auftreten:

- Alle fuenf Kombinationen aus Modus und Feststellbutton sehen richtig aus, `:app:core` steht
  unter `:app`, und die Tasks des Wurzelprojekts sind sichtbar.
- Ein Doppelklick auf `buildImage` in `:app` erzeugt genau **eine** Ausgabezeile
  (`Projekt=:app`), nicht zwei. Die Gegenprobe auf der Kommandozeile steht in der README des
  Testprojekts.
- `:legacy` erscheint nur mit `docker/gui`, im `Sort`-Modus fehlt zusaetzlich `:app:core`.
- Dieselbe Gruppe laesst sich in zwei Modulen unterschiedlich aufgeklappt lassen, und der
  Zustand ueberlebt einen IDE-Neustart.
- Der Feststellbutton schreibt `showModules` nach `custom.yaml`, "Reset to Default" nimmt es
  zusammen mit allem anderen wieder zurueck.

## Mit dem umgebauten Default-Modus hinzugekommen

Die Baumform ist durch Unit-Tests abgedeckt (REQ-077); der visuelle Abgleich ist es nicht. Er
ist der eigentliche Nachweis dieser Anforderung:

- Die eigene Ansicht steht im `Default`-Modus neben der originalen Gradle-Ansicht der IDE,
  ohne dass ein Unterschied ausser dem Ordnerbaum auffaellt.
- Ein Modul ohne eigene Tasks zeigt keinen `Tasks`-Knoten, sondern direkt seine Untermodule.
- Der Feststellbutton ist in `Default` ausgegraut und angehakt; nach dem Wechsel zu `Nested`
  ist er wieder wirksam und steht auf dem zuvor gespeicherten Wert.
- Der `Tasks`-Knoten laesst sich nicht getrennt von seinem Modul zuklappen und taucht in der
  Speed-Search nicht als Treffer auf.
