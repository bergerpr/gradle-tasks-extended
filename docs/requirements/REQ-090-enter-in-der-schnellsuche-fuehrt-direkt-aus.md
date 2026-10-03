---
id: REQ-090
titel: Enter in der Schnellsuche fuehrt den gefundenen Task direkt aus
kategorie: interaktion
verifiziert: konstruktiv
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTree.kt
quelle: Benutzerbericht "Bei der Schnellsuche muss man zweimal Enter druecken"
---

Steht die Schnellsuche (REQ-013) auf einem Task, fuehrt **ein** Enter ihn aus und schliesst
die Suche dabei. Frueher beendete der erste Enter nur die Suche, der Baum wurde ohne Filter neu
aufgebaut, und erst ein zweiter Enter startete den weiterhin selektierten Task.

Steht die Suche auf einem Modul-, Gruppen-, Ordner- oder `Tasks`-Knoten, beendet Enter wie
gewohnt nur die Suche - ein Auf- oder Zuklappen (REQ-089) gibt es dabei nicht, denn das Ende
der Suche baut den Baum ohnehin neu auf. Bei einer Mehrfachauswahl gilt dasselbe (REQ-087).

Moeglich ist das, weil die Enter-Aktion ueber den `IdeKeyEventDispatcher` laeuft (REQ-087):
Der fuehrt eine aktive Aktion aus, bevor die Key-Listener der Komponente - darunter der der
Speed Search - den Tastendruck sehen. Die Aktion meldet sich bei offener Suche deshalb genau
dann als aktiv, wenn ein einzelner Task selektiert ist. Sie startet den Task zuerst und ruft
danach `TreeSpeedSearch.hidePopup()`: Das Schliessen setzt den Filter zurueck und baut den Baum
neu, der Task steht zu diesem Zeitpunkt aber schon fest.
