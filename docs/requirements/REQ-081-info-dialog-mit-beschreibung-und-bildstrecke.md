---
id: REQ-081
titel: Info-Dialog mit Beschreibung und Bildstrecke
kategorie: interaktion
verifiziert: manuell
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksConfigHelpDialog.kt
  - src/main/kotlin/de/berger/gradletasksview/ui/ScreenshotSlider.kt
  - src/main/kotlin/de/berger/gradletasksview/ui/ScreenshotViewerDialog.kt
  - src/main/resources/help/description.html
  - src/main/resources/help/screenshots/screenshots.txt
quelle: Wunsch des Benutzers
---

Der Info-Dialog (REQ-080) hat unter dem Kopf zwei Reiter:

- **Description** - ein durchgehender, scrollender Block: erster Absatz, dann die
  **Bildstrecke**, dann die Merkmalsliste (Ansichtsmodi, schaltbare Modulebene, Ausblenden,
  eigene Icons, Ausfuehrungsarten) und zum Schluss der Hinweis auf die Konfiguration und die
  Herkunft der Icons. Getrennt wird nur durch Abstaende - Text und Bilder standen zuerst in
  zwei Bereichen mit eigenem Bildlauf, was die Beschreibung an einer Stelle zerschnitt, an der
  inhaltlich nichts endet.
- **Configuration** - alles, was der Dialog vorher als Ganzes zeigte: Pfade, Unterschied
  zwischen Team-Vorgabe und persoenlicher Datei, Aufbau der YAML und Vorrangreihenfolge.

Die Trennung folgt der Frage, die jemand gerade hat: "Was kann das?" beim ersten Blick,
"Wo stelle ich das ein?" spaeter. Vorher stand beides in einer langen Seite, in der die
Beschreibung gefehlt hat.

Der Text liegt als `description.html` bei, allerdings nicht als vollstaendiges Dokument,
sondern als drei Fragmente, getrennt durch die Marken `<!-- screenshots -->` und
`<!-- footnote -->`. Gesetzt werden sie mit `text`/`comment` aus dem UI-DSL der Plattform: Das
bringt Umbruch, Abstaende und Themefarben mit, waehrend ein `JEditorPane` in einer Spalte seine
Hoehe bei jeder Breitenaenderung selbst nachrechnen muesste.

## Bildstrecke

Die Bilder liegen in `src/main/resources/help/screenshots/`, ihre Reihenfolge und
Bildunterschriften in `screenshots.txt` daneben - ein Eintrag pro Zeile,
`dateiname.png | Bildunterschrift`, `#` leitet einen Kommentar ein. Bewusst eine Indexdatei
statt eines Verzeichnis-Scans: In einem Jar laesst sich ein Ressourcenverzeichnis nicht
zuverlaessig auflisten, und so stehen Reihenfolge und Text an einer Stelle, die man von Hand
pflegt.

Ein Eintrag ohne passende Datei wird uebersprungen (Logeintrag, keine Luecke im Dialog) - der
Index darf also geplante Bilder auffuehren, bevor es sie gibt. Bleibt nichts uebrig, erklaert
ein Hinweistext, wohin die Dateien gehoeren, statt einen leeren Rahmen zu zeigen.

Geblaettert wird zyklisch ueber zwei Schalter mit Zaehler (`2 / 4`) und Bildunterschrift; am
Ende geht es vorne weiter, das erspart das Deaktivieren der Schalter. Bilder werden
seitenverhaeltnistreu eingepasst und nie vergroessert, damit ein Screenshot nicht unscharf
aufgeblasen wird.

## Vergroesserte Ansicht

Ein Klick auf das Bild oeffnet es in einem eigenen Fenster - die Screenshots zeigen eine
Tool-Window-Ansicht im Hochformat und bleiben in der Bildstrecke zwangslaeufig klein. Dort
laesst es sich einpassen, auf Originalgroesse stellen und stufenlos zoomen, mit vier Schaltern
und dem Mausrad (ohne Zusatztaste, denn im Bildbetrachter ist Zoomen die haeufigere Absicht als
Blaettern; zum Verschieben gibt es die Leisten des Bildlaufs).

Weitergeblaettert wird auch dort, mit zwei Pfeilen und den Pfeiltasten - und zwar **unter
Beibehaltung des Zoomfaktors**: Zwei Screenshots derselben Stelle lassen sich nur vergleichen,
wenn sie gleich gross bleiben, deshalb wird beim Wechsel nur das Bild getauscht. Der Titel
fuehrt Bildunterschrift und Zaehler; bei einem einzigen Bild bleiben die Pfeile sichtbar, aber
ausgegraut. Die Tastenkuerzel haengen an der Wurzelkomponente des Fensters und werden erst
nach `init()` registriert - vorher gibt es sie nicht - und ueber dessen `disposable` wieder
abgemeldet.

Das Fenster ist **nicht modal**, sodass der Info-Dialog daneben bedienbar bleibt, und merkt
sich seine Groesse unter einem eigenen Dimensionsschluessel. Der Zoombereich ist auf das
Zehnfache nach unten und das Achtfache nach oben begrenzt; beim Oeffnen wird eingepasst, aber
nie ueber die Originalgroesse hinaus vergroessert. Dass der Klick etwas bewirkt, zeigen
Handcursor und Kurzinfo auf dem Bild.

Das Zerlegen der Indexdatei steckt in `ScreenshotIndex` und ist damit ohne laufende IDE
testbar (REQ-060); das Anzeigen in `ScreenshotSlider`.
