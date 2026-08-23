---
id: OPEN-004
titel: Manuelle Testmatrix nur teilweise abgearbeitet
prioritaet: niedrig
betrifft_anforderung:
  - REQ-042
  - REQ-078
  - REQ-031
  - REQ-074
  - REQ-075
  - REQ-077
  - REQ-079
  - REQ-080
  - REQ-081
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

## Mit dem neuen Konfigurationsformat hinzugekommen

Format, Aufloesung und Sichtbarkeitsregeln sind durch Unit-Tests abgedeckt (REQ-075, REQ-076);
was nur in einer laufenden IDE sichtbar wird, ist es nicht. Die `default.yaml` des Testprojekts
ist bereits im neuen Format geschrieben:

- Der Baum sieht aus wie vorher: `:legacy` nur mit `docker/gui`, `:tools` als Behaelter ohne
  eigene Tasks, `:app:core` im `Sort`-Modus ausgeblendet.
- Ein `Collapse All` erzeugt eine **kurze** `custom.yaml` - nur noch die Knoten, die vom
  aufgeloesten Wert abweichen. Klappt man denselben Knoten wieder auf, verschwindet sein
  Eintrag.
- **Hide** auf `docker/gui` in `:app` laesst die Gruppe in `:lib` stehen; der Eintrag landet
  im Modulblock von `:app`.
- **Hide** auf einem Task, dessen Name auch als Gruppe vorkommt, trifft nur den Task - dafuer
  gibt es die getrennten Namensraeume (REQ-078, ADR-020).
- **Show Hidden** zeigt die ausgeblendeten Knoten ausgegraut; **Show Again** holt sie zurueck,
  auch die aus `default.yaml` stammenden (dann steht ein `hidden: false` in `custom.yaml`).
- Bei ausgeschalteter Modulgliederung blendet **Hide** den Knoten in allen Modulen aus.

## Mit den Ausfuehrungsarten hinzugekommen

REQ-079 ist reine Plattform-Anbindung und deshalb nur in einer laufenden IDE nachweisbar;
Unit-Tests gibt es dafuer nicht:

- Das Kontextmenue auf einem Task zeigt Run, Debug und Run with Coverage mit den Namen und
  Icons der Plattform (`Run ':app:buildImage'`), darunter "Modify Run Configuration"; auf
  einem Ordner- oder Modulknoten sind sie inaktiv.
- Jeder Start legt einen temporaeren Eintrag im Konfigurations-Widget der Kopfzeile an und
  waehlt ihn aus - auch der Doppelklick, der weiterhin den normalen Run startet.
- Ein zweiter Start desselben Tasks erzeugt keinen zweiten Eintrag, sondern benutzt den
  vorhandenen wieder.
- `Debug` haelt an einem Breakpoint an; `Run with Coverage` oeffnet die Coverage-Auswertung.
  Zu beobachten ist, wie sich die Plattform bei einem Task verhaelt, der nichts zu messen hat.
- "Modify Run Configuration" oeffnet den Konfigurationsdialog mit vorbelegtem Taskpfad.
- Mehrere Tasks desselben Moduls selektiert: ein Aufruf mit beiden Tasknamen. Tasks aus
  verschiedenen Modulen selektiert: die Ausfuehrungseintraege sind inaktiv.

## Mit den Konfigurationsknoepfen hinzugekommen

- Zahnrad und Info stehen ganz rechts in der Toolbar, das Info-Symbol rechts vom Zahnrad.
- Das Zahnrad oeffnet die `default.yaml` des Testprojekts im Editor; nach dem Loeschen der
  Datei legt derselbe Klick sie mit dem kommentierten Startinhalt neu an.
- Der Info-Dialog zeigt die drei Pfade des geoeffneten Projekts - nicht die eines anderen -,
  ist in hellem und dunklem Theme lesbar und laesst sich scrollen.
- Ein Klick auf `default.yaml` oder `custom.yaml` schliesst den Dialog und oeffnet die Datei;
  der Klick auf `icons/` oeffnet das Verzeichnis im Dateimanager.
- Bei zwei gleichzeitig geoeffneten Projekten zeigt jede Ansicht ihre eigenen Pfade (REQ-042).
- Im Kopf steht das Logo links, ohne hellen Kasten um den Elefanten - der Hintergrund muss in
  hellem **und** dunklem Theme durchscheinen.
- Rechts daneben Name und eine Zeile mit Version, Commit und Branch; "Details" ist beim
  Oeffnen zugeklappt und der Kopf nimmt hoechstens ein Viertel des Dialogs ein.
- Aufgeklappt zeigt "Details" alle Kenndaten, die mit `gradle.properties` bzw. der Sandbox-IDE
  uebereinstimmen und nirgends `?` zeigen; die Beschreibung darunter weicht dabei nach unten,
  ohne dass der Dialog springt.
- Der Kopf bleibt stehen, waehrend die Beschreibung darunter scrollt.
- Auf einem hochaufloesenden Bildschirm ist das Logo scharf (dann greift `elefant@2x.png`).
- Die Zeile "Branch" zeigt den Branch des Builds und bei nicht eingecheckten Aenderungen den
  Zusatz; "Installed at" zeigt das Sandbox-Verzeichnis, oeffnet es per Klick im Dateimanager
  und haelt den vollen Pfad im Tooltip.
- Die Tabellen im beschreibenden Teil stehen linksbuendig, und der Dialog braucht keinen
  waagerechten Bildlauf.
- Das YAML-Beispiel ist eingefaerbt und die Kommentarspalte bleibt ausgerichtet; nach einem
  Wechsel des Editor-Farbschemas (hell/dunkel) passt sich die Faerbung beim naechsten Oeffnen
  des Dialogs an.

## Mit den Reitern und der Bildstrecke hinzugekommen

- Der Dialog oeffnet auf `Description`; Absatz, Bildstrecke, Merkmalsliste und Fussnote stehen
  in einem durchgehenden Block, der als Ganzes scrollt - keine zwei Bildlaeufe nebeneinander.
- Der Link in der Fussnote oeffnet den Lizenztext der Icons.
- Die Bildstrecke blaettert mit beiden Schaltern zyklisch durch alle Bilder, Zaehler und
  Bildunterschrift wandern mit, und die Bilder werden nie ueber ihre Groesse hinaus
  aufgeblasen.
- Solange keine Screenshots mitgeliefert sind, steht dort der Hinweis auf das
  Ressourcenverzeichnis - kein leerer Rahmen.
- Ein Eintrag in `screenshots.txt` ohne passende Datei laesst den Dialog unveraendert
  funktionieren.
- Der Reiter `Configuration` zeigt unveraendert Pfade, Beispiel und Vorrangreihenfolge.
- Ein Klick auf ein Bild oeffnet es gross; Einpassen, Originalgroesse, die beiden Zoomschalter
  und das Mausrad wirken, und der Info-Dialog dahinter bleibt bedienbar.
- Das Betrachterfenster merkt sich seine Groesse ueber ein erneutes Oeffnen hinweg.
- Im Betrachter blaettern Pfeile und Pfeiltasten weiter, ohne den Zoomfaktor zu aendern; der
  Titel zaehlt mit, und das Fenster oeffnet bei dem Bild, das die Bildstrecke gerade zeigt.
- Am seitlichen Streifen steht der Elefant statt des alten Baumsymbols, in hellem und dunklem
  Theme erkennbar und auf einem hochaufloesenden Bildschirm scharf.
