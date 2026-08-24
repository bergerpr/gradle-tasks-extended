---
id: REQ-013
titel: Filterung ueber Speed-Search
kategorie: interaktion
verifiziert: visuell
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksSpeedSearch.kt
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTaskViewRefresher.kt
quelle: Plan.md, Abschnitt "Filterung"
---

Die Ansicht filtert ueber `TreeSpeedSearch` auf dem **qualifizierten** Taskpfad
(`:app:buildImage`), und zwar in allen drei Modi. Der Taskname ist darin als Teilzeichenkette
enthalten, die Suche nach `buildImage` funktioniert also unveraendert; zusaetzlich laesst sich
mit `:app` auf ein Modul eingrenzen - in einem grossen Build die naheliegendste Filterung.

Die Filterung wirkt **datengetrieben vor** dem Aufbau von Sort/Nested: Der Refresher filtert
die Taskliste und baut den Baum daraus neu. Wuerde stattdessen nur der Speed-Search-Cursor
bewegt, blieben ausgefilterte Tasks durch die Verschachtelung sichtbar.

Bei einem ueber `execute` konfigurierten Knoten (REQ-085) steht die Beschriftung vor dem Pfad
(`Alle Images bauen :app:buildImage`), sonst waere er nur ueber den Task auffindbar, den er
ausfuehrt - nicht ueber das, was dasteht. Fuer einen echten Task aendert das nichts, dessen
Beschriftung ist im Pfad ohnehin enthalten.

Bei Ordnerknoten ist der Suchtext der angezeigte Name. Ein zusammengefasster Ordner (REQ-004)
wird damit ueber jedes seiner Segmente gefunden - `server/gui` sowohl ueber `server` als auch
ueber `gui`.

Das Ausblenden (REQ-075) greift noch davor. Die Suche findet daher keine Tasks aus
ausgeschlossenen Modulen - ein Treffer, den der sichtbare Baum nicht enthaelt, waere ein
Widerspruch.

`TreeSpeedSearch` feuert sein Change-Event auch dann, wenn sich der Text nicht geaendert hat
(`PropertyChangeSupport` unterdrueckt nur bei nicht-null `oldValue`). Ein Rebuild wird daher
erst nach einem echten Textwechsel ausgeloest - sonst wuerde jeder manuelle
Expand/Collapse-Zustand vom naechsten Phantom-Refresh zurueckgesetzt.

## Einbindung

`TreeSpeedSearch.installOn(tree, false) { treePath -> ... }` wird direkt auf dem eigenen
`JTree` installiert. Die Ursachenanalyse hinter der Textwechsel-Pruefung steht in ADR-012.
