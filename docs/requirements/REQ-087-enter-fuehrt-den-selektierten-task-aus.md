---
id: REQ-087
titel: Enter fuehrt den selektierten Task aus
kategorie: interaktion
verifiziert: manuell
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTree.kt
  - src/main/kotlin/de/berger/gradletasksview/execution/GradleTaskExecutor.kt
quelle: Benutzerbericht "Enter auf einem selektierten Task tut nichts"
---

Ist im Baum **genau eine** Zeile selektiert und ist diese ein Task, fuehrt Enter den Task aus -
dieselbe Ausfuehrung wie der Doppelklick (REQ-010), also ueber eine temporaere
Run-Konfiguration und mit dem normalen Run-Executor.

Bei einer Mehrfachauswahl passiert nichts. Das ist ausdruecklich so gewollt und weicht vom
Kontextmenue ab, das mehrere Tasks desselben Gradle-Projekts in einem Aufruf startet: Ein
Tastendruck soll nicht unbemerkt mehrere Laeufe ausloesen. Auf einem Modul-, Gruppen-, Ordner-
oder `Tasks`-Knoten fuehrt Enter nichts aus, sondern klappt den Knoten auf oder zu (REQ-089).

Numpad-Enter ist mit abgedeckt und braucht keinen eigenen Shortcut: AWT meldet fuer beide
Tasten denselben `keyCode` `VK_ENTER` und unterscheidet sie nur ueber `getKeyLocation()`, was
beim Abgleich eines `KeyStroke` keine Rolle spielt.

Umgesetzt ist das als `AnAction` mit `CommonShortcuts.ENTER`, die auf dem Baum registriert ist -
nicht als eigener `KeyListener` und nicht als InputMap-Eintrag. Nur so laeuft der Tastendruck
durch den `IdeKeyEventDispatcher` der Plattform, und ob er greift, entscheidet die Aktion in
`update`. Der Dispatcher fuehrt eine aktive Aktion **vor** den Key-Listenern der Komponente
aus, also auch vor der Speed Search (REQ-013) - wie Enter bei offener Suche wirkt, regelt
REQ-090: Auf einem Task startet er sofort.

Die Aktion ist bewusst nicht in der `plugin.xml` deklariert. Sie hat weder einen Platz im
Kontextmenue noch in der Toolbar und waere dort nur ein weiterer, im Keymap auftauchender
Eintrag ohne eigenen Nutzen.
