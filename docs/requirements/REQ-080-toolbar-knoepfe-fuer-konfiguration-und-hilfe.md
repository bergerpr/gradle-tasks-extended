---
id: REQ-080
titel: Toolbar-Knoepfe fuer Konfiguration und Hilfe
kategorie: interaktion
verifiziert: manuell
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/actions/GradleTaskConfigActions.kt
  - src/main/kotlin/de/berger/gradletasksview/ui/YamlSnippetHighlighter.kt
  - src/main/resources/help/example-config.yaml
  - src/main/kotlin/de/berger/gradletasksview/GradleTasksBuildInfo.kt
  - build.gradle.kts
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksConfigHelpDialog.kt
  - src/main/kotlin/de/berger/gradletasksview/toolwindow/GradleTasksExtendedPanel.kt
  - src/main/resources/help/configuration.html
quelle: Wunsch des Benutzers
---

Ganz rechts in der Toolbar stehen zwei Knoepfe, das Info-Symbol rechts neben dem Zahnrad:

- **Zahnrad** (`Edit Default Configuration`) oeffnet die `default.yaml` des geoeffneten
  Projekts im Editor. Fehlt sie, wird sie vorher mit dem kommentierten Startinhalt angelegt
  (`ensureDefaultConfigExists`), damit die Aktion nie ins Leere laeuft. Bewusst die
  Team-Vorgabe und nicht `custom.yaml`: Letztere schreibt das Plugin selbst und enthaelt nur
  Abweichungen, die sich beim naechsten Auf- oder Zuklappen ohnehin aendern (REQ-070).
- **Info** (`Configuration Help`) oeffnet einen Dialog mit Kopfbereich und zwei Reitern
  (REQ-081). Der Reiter `Configuration` beschreibt, wo die Konfiguration liegt und wie sie
  aufgebaut ist: die drei Pfade des Projekts (`default.yaml`, `custom.yaml`, `icons/`), der
  Unterschied zwischen Team-Vorgabe und persoenlicher Datei, die vier festen Schluessel mit
  Beispiel, die moeglichen Felder eines Eintrags und die Vorrangreihenfolge (REQ-076).

## Kopf des Dialogs

Der Kopf ist zweispaltig: links das Logo (`elefant.png` mit transparentem Hintergrund, auf 64
px heruntergerechnet), rechts der Plugin-Name, darunter eine Zeile mit Version, Build-Commit
und Branch - und darunter, **zugeklappt**, ein Abschnitt "Details" mit der vollstaendigen
Kenndatentabelle: Version, Build (Commit und Erzeugungszeitpunkt), Branch, Plugin-ID,
Herausgeber, Installationsverzeichnis, Zielplattform, kleinster IDE-Build, laufende IDE und
JRE.

Die erste Fassung stellte all das mittig und immer sichtbar untereinander; damit ging die
halbe Dialoghoehe fuer Angaben drauf, die man nur im Fehlerfall braucht. Jetzt steht oben, was
man haeufig braucht, und der Rest ist einen Klick entfernt. Gebaut ist der Abschnitt mit
`collapsibleGroup` aus dem UI-DSL der Plattform; die naheliegende Klasse `HideableDecorator`
ist in allen Ueberladungen abgekuendigt (ADR-008). Der Kopf steht ueber den Reitern und scrollt nicht mit - sonst waere die Version genau dann
verschwunden, wenn man weiter unten nachliest.

Drei davon verdienen eine Erlaeuterung:Drei davon verdienen eine Erlaeuterung:

- **Branch** traegt den Zusatz "with uncommitted changes", wenn beim Bauen etwas nicht
  eingecheckt war - so ist einem Build anzusehen, ob er aus einem reproduzierbaren Stand
  stammt. Ins Task-Input geht nur das Ja/Nein, nicht die Liste der Aenderungen, sonst waere
  die Aufgabe bei jeder Bearbeitung veraltet.
- **Herausgeber** kommt aus `pluginVendor` in `gradle.properties`. Von dort traegt
  `patchPluginXml` ihn in die `plugin.xml` ein - die haelt ihn nicht mehr selbst, damit es
  nur eine Quelle gibt. `pluginVendorEmail` und `pluginVendorUrl` sind leer vorbelegt; sind
  sie gesetzt, haengt der Dialog sie mit Mittelpunkt getrennt an den Namen.
- **Installationsverzeichnis** beantwortet die Frage, welche Kopie gerade laeuft. Ermittelt
  wird es aus der Herkunft der eigenen Klasse (`<plugin>/lib/x.jar` liegt zwei Ebenen tiefer),
  angezeigt auf `~` gekuerzt und anklickbar, mit dem vollen Pfad im Tooltip. Die Bezeichnungen
stehen rechtsbuendig an der Mitte, die Werte linksbuendig, damit der Block als Ganzes mittig
wirkt, ohne dass die Werte zur Zickzacklinie werden. Der Kopf scrollt nicht mit - sonst waere
die Version genau dann verschwunden, wenn man weiter unten nachliest.

Das Logo laedt der `IconLoader` aus `src/main/resources/images/`, wo neben `elefant.png` (128
px) auch `elefant@2x.png` (256 px) liegt; auf hochaufloesenden Bildschirmen nimmt er
automatisch die groessere Variante. Deshalb ist der Kopf Swing und nicht Teil des HTML: Ein
`<img>` im `JEditorPane` zoege unabhaengig von der Skalierung immer dieselbe Pixelgroesse.
Erzeugt wurden beide aus dem Original im Projektwurzelverzeichnis, indem der zusammenhaengende
Aussenhintergrund transparent gemacht und auf den Inhalt beschnitten wurde - eine reine
Farbersetzung haette auch Stosszaehne und Zehen getroffen, die dieselbe helle Farbe haben.

Der beschreibende Teil darunter fuehrt die Konfigurationsdateien und die Felder eines Eintrags
jeweils als Tabelle auf. Die Pfade stehen dort ebenfalls auf `~` gekuerzt, weil ein voller Pfad breiter waere
als der Dialog und die Beschreibung in einen waagerechten Bildlauf zwaenge; verlinkt ist
weiterhin der absolute Pfad.

Das YAML-Beispiel ist eingefaerbt: Schluessel, Wahrheitswerte, Zeichenketten und Kommentare
bekommen ihre Farbe aus dem **aktiven Editor-Farbschema**
(`DefaultLanguageHighlighterColors` ueber `EditorColorsManager`), samt Hintergrund des
Editors - so sieht der Ausschnitt aus wie die Datei, die man gleich danach oeffnet, und zwar
in jedem Theme. Das Beispiel selbst steht als `help/example-config.yaml` neben der
HTML-Datei, damit es gueltiges YAML bleibt und nicht in Markup ertrinkt.

Die Zerlegung uebernimmt `YamlSnippetHighlighter` - ein kleiner eigener Zeilenleser statt des
YAML-Lexers der Plattform, der am optionalen YAML-Plugin haengt. Die Farben werden ihm als
`YamlHighlightPalette` hineingereicht, statt sie dort zu lesen; dadurch ist die Logik reine
Modelllogik und ohne laufende IDE testbar (REQ-060).

Die Kenndaten kommen aus einer beim Build erzeugten `build-info.properties`
(Gradle-Aufgabe `generateBuildInfo`), nicht aus dem Plugin-Deskriptor der Plattform: Alle
Zugaenge dorthin - `PluginManagerCore.getPlugin`, `PluginManager.getPluginByClass`,
`findEnabledPlugin` - sind mit `@ApiStatus.Internal` markiert und damit nach ADR-008
ausgeschlossen. Fehlt die Datei, zeigt die Tabelle `?` statt zu scheitern. Der Zeitstempel ist
bewusst kein Task-Input, sonst waere die Aufgabe bei jedem Build veraltet und zoege Jar und
Plugin-ZIP jedes Mal mit.

## Sonstiges

Die Pfade im Dialog sind anklickbar und werden aus [GradleTasksViewConfigStore] selbst
gelesen - eine Hilfe, die auf ein anderes Verzeichnis zeigt als das, aus dem das Plugin liest,
waere schlimmer als gar keine. Ein Klick schliesst den Dialog, weil er modal ist und die
geoeffnete Datei sonst unerreichbar dahinter laege; ein Verzeichnis geht stattdessen im
Dateimanager auf.

Der Hilfetext liegt als HTML-Ressource in `src/main/resources/help/configuration.html`, nicht
im Quellcode, und wird nur um die drei Pfade und die Theme-Farben ergaenzt. Beide Knoepfe
benutzen `AllIcons` (`General.Settings`, `General.Information`) - fuer Toolbar-Symbole wiegt
die IDE-weite Wiedererkennbarkeit hoeher als eigene Gestaltung (ADR-004).
