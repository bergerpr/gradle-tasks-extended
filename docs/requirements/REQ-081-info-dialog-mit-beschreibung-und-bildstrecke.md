---
id: REQ-081
titel: Info-Dialog mit Beschreibung und Bildstrecke
kategorie: interaktion
verifiziert: manuell
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksConfigHelpDialog.kt
  - src/main/kotlin/de/berger/gradletasksview/ui/ScreenshotSlider.kt
  - src/main/resources/help/description.html
  - src/main/resources/help/screenshots/screenshots.txt
quelle: Wunsch des Benutzers
---

Der Info-Dialog (REQ-080) hat unter dem Kopf zwei Reiter:

- **Description** - eine kurze Beschreibung, was das Plugin tut: die drei Ansichtsmodi, die
  schaltbare Modulebene, das Ausblenden von Knoten, eigene Icons und die Ausfuehrungsarten im
  Kontextmenue. Darunter eine **Bildstrecke** mit Screenshots, die dasselbe zeigen.
- **Configuration** - alles, was der Dialog vorher als Ganzes zeigte: Pfade, Unterschied
  zwischen Team-Vorgabe und persoenlicher Datei, Aufbau der YAML und Vorrangreihenfolge.

Die Trennung folgt der Frage, die jemand gerade hat: "Was kann das?" beim ersten Blick,
"Wo stelle ich das ein?" spaeter. Vorher stand beides in einer langen Seite, in der die
Beschreibung gefehlt hat.

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

Das Zerlegen der Indexdatei steckt in `ScreenshotIndex` und ist damit ohne laufende IDE
testbar (REQ-060); das Anzeigen in `ScreenshotSlider`.
