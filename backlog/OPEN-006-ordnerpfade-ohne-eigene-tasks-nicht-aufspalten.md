---
id: OPEN-006
titel: Ordnerpfade ohne eigene Tasks nicht aufspalten
prioritaet: mittel
betrifft_anforderung:
  - REQ-004
  - REQ-021
  - REQ-071
betrifft:
  - src/main/kotlin/de/example/gradletasksview/model/TaskTreeBuilder.kt
  - src/main/kotlin/de/example/gradletasksview/model/NestedFolderNode.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTreeCellRenderer.kt
quelle: Anforderung des Benutzers
---

Im `Nested`-Modus wird heute **jedes** Segment eines Gruppenpfads zu einem eigenen
Ordnerknoten. Enthaelt `docker/server/gui` nur Tasks auf der Ebene `gui`, entstehen trotzdem
drei verschachtelte Knoten, von denen zwei nichts zeigen ausser dem naechsten Knoten. Das
kostet zwei Klicks und zwei Zeilen Hoehe, ohne eine einzige Information zu tragen.

Ein Ordner soll deshalb mit seinem einzigen Kind verschmelzen, solange er keine eigenen Tasks
hat. Aus drei Knoten wird dann eine Zeile mit der Beschriftung `docker/server/gui`. Sobald auf
der Ebene `server` ein Task hinzukommt, spaltet sich der Pfad wieder auf.

Dies ist die Voraussetzung fuer OPEN-007: Der dort beschriebene Default-Modus zeigt denselben
Ordnerbaum, und ohne diese Regel waere er noch tiefer verschachtelt als der heutige
`Nested`-Modus.

## Entschieden

### Zusammenfassungsregel

Ein Ordnerknoten wird mit seinem Kind verschmolzen, wenn **beides** zutrifft:

- Er hat keine eigenen Tasks.
- Er hat **genau ein** Kind, und dieses Kind ist ein Ordner.

Die Regel wird transitiv angewandt, `docker` + `server` + `gui` werden also zu einer einzigen
Zeile. Die Beschriftung ist der zusammengesetzte Pfad mit `/` als Trenner, also genau die
Schreibweise, in der der Gruppenname auch im Buildskript steht.

Das ist dieselbe Regel, die die IDE in der Projektansicht als "Compact Middle Packages"
verwendet. Sie hier nachzubilden ist kein Zufall: Benutzer kennen das Verhalten bereits, und
die Bedingung "keine eigenen Kinder ausser genau einem Ordner" ist die einzige, unter der sich
zwei Ebenen ueberhaupt verlustfrei zu einer Zeile zusammenziehen lassen.

### Zwei Kinder verhindern das Zusammenfassen

Hat `server` keine eigenen Tasks, aber die Kinder `gui` und `cli`, bleibt `server` ein eigener
Knoten mit zwei Kindern. Ein Zusammenfassen zu `server/gui` und `server/cli` als Geschwister
scheidet aus - `server` waere dann doppelt gezeichnet, und der gemeinsame Ast ginge verloren.
Die urspruengliche Formulierung ("nur Tasks in gui") deckt diesen Fall nicht ab; er ist hiermit
entschieden.

### Schluessel bleiben der volle Pfad

`FolderNodeData.folder.fullPath` bleibt der vollstaendige Pfad (`docker/server/gui`), auch wenn
mehrere Ebenen zu einer Zeile verschmolzen sind. Bestehende Eintraege in
`default.yaml`/`custom.yaml` und zugeordnete Icons wirken damit unveraendert weiter, und der
Schluesselraum bleibt derselbe wie in `Sort` (REQ-071, REQ-076).

**Folge, die bewusst in Kauf genommen wird:** Ein Eintrag fuer einen weggefallenen
Zwischenknoten - etwa `docker/server: false` - findet keinen Knoten mehr und bleibt wirkungslos.
Das ist nicht zu vermeiden, ohne den Schluesselraum mehrdeutig zu machen, und faellt praktisch
kaum ins Gewicht: Ein Zwischenknoten ohne eigene Tasks ist genau der, den zuzuklappen ohnehin
niemanden interessiert. Beim Umsetzen ist das in REQ-071 zu erwaehnen, nicht stillschweigend
hinzunehmen.

### Wo die Regel greift

Nur dort, wo aus Gruppenpfaden ein Ordnerbaum gebaut wird: im `Nested`-Modus und im neuen
Default-Modus aus OPEN-007. `Sort` und der heutige `Default` zeigen ohnehin flache Zeilen mit
dem vollen Gruppenpfad - dort gibt es nichts aufzuspalten und damit auch nichts
zusammenzufassen.

Nicht zusammengefasst wird ueber die Modulgrenze hinweg: Ein Modulknoten ist kein Ordner, und
`:app` + `docker` bleiben zwei Zeilen. Modul und Gruppe sind verschiedene Begriffe, und ihre
Pfade verwenden mit `:` und `/` bewusst verschiedene Trenner.

### Fest eingebaut, kein Schalter

Es gibt weder einen Toolbar-Button noch einen YAML-Schluessel dafuer. Die Regel ist das
Verhalten, nicht eine Option - ein Schalter brauchte einen weiteren persistierten Zustand und
eine Erklaerung, wofuer man ihn ausschalten wollen wuerde.

## Auswirkungen

- **Speed-Search** (REQ-013): Der Suchtext eines zusammengefassten Knotens ist die
  zusammengesetzte Beschriftung. `server/gui` wird damit sowohl von `server` als auch von `gui`
  gefunden - vorher nur von jeweils einem der beiden Knoten.
- **Copy Folder Path** (REQ-012) bleibt unveraendert, es kopiert weiter den vollen Pfad.
- **Ordner-Icon** (REQ-005) unveraendert; der zusammengefasste Knoten ist weiterhin ein
  Container und bekommt das Ordner-Icon nach Auf-/Zuklappzustand.
- Der Renderer zeigt bei Ordnern heute `folder.displayName` (ein einzelnes Segment). Fuer
  zusammengefasste Knoten muss daraus der zusammengesetzte Anzeigename werden - am saubersten
  als eigenes Feld am `NestedFolderNode`, damit `displayName` nicht zwei Bedeutungen bekommt.

## Testfaelle

Reine Modelllogik, also ohne Platform-Testframework abdeckbar - genau die Sorte Regel, bei der
sich ein Fehler nur als "sieht komisch aus" aeussert und sonst niemandem auffaellt.

| Fall | Erwartung |
|---|---|
| `docker/server/gui` mit Tasks nur in `gui` | eine Zeile `docker/server/gui` |
| dazu ein Task in `docker/server` | `docker/server` und darunter `gui` |
| dazu ein Task in `docker` | `docker`, `server`, `gui` - drei Zeilen wie heute |
| `server` ohne Tasks, Kinder `gui` und `cli` | `server` bleibt, zwei Kindzeilen |
| flacher Pfad `build` | unveraendert eine Zeile |
| zusammengefasster Knoten | `fullPath` ist weiterhin `docker/server/gui` |
| Icon-Eintrag auf `docker/server/gui` | greift auf der zusammengefassten Zeile |
| Gruppe `Other` (REQ-022) | wird wie jeder andere Pfad behandelt |

**Fertig, wenn:** Alle Faelle gruen und im Testprojekt erscheint `docker/server/gui` als eine
Zeile statt als drei.

## Katalog danach

- REQ-004 um die Zusammenfassungsregel ergaenzen.
- REQ-071 um den Hinweis ergaenzen, dass ein Eintrag auf einen weggefallenen Zwischenknoten
  wirkungslos bleibt.
- Einen ADR nur anlegen, falls beim Umsetzen eine Entscheidung dazukommt, die hier noch nicht
  steht; die Begruendungen oben gehoeren sonst in REQ-004.
