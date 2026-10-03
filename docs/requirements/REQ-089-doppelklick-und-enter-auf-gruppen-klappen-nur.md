---
id: REQ-089
titel: Doppelklick und Enter auf Gruppen klappen nur auf oder zu
kategorie: interaktion
verifiziert: konstruktiv
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTree.kt
quelle: Benutzerbericht "Doppelklick oder Enter auf einer Gruppe fuehrt einen Task aus"
---

Ein Doppelklick oder Enter auf einem Modul-, Gruppen-, Ordner- oder `Tasks`-Knoten klappt
diesen Knoten auf bzw. zu - und fuehrt **niemals** einen Task aus. Ausgefuehrt wird nur, wenn
die Aktion einen Task-Knoten selbst trifft (REQ-010, REQ-087).

Doppelklick: Das Auf-/Zuklappen erledigt die TreeUI schon beim zweiten `mousePressed`, und
weil `scrollsOnExpand` aktiv ist, scrollt der Baum beim Aufklappen so weit, dass die Kinder
sichtbar werden. Das `mouseClicked` desselben Doppelklicks kommt erst danach an - unter dem
Mauszeiger liegt dann oft schon ein Task der gerade aufgeklappten Gruppe, und genau der lief
frueher los. Der Doppelklick fuehrt deshalb nur aus, wenn die Zeile unter dem Mauszeiger die
**selektierte** ist: Selektiert wurde beim ersten Klick die Gruppe, und das Scrollen aendert
die Selektion nicht.

Enter: Dieselbe Aktion, die auf einem Task ausfuehrt, klappt jeden anderen Knoten mit Kindern
auf oder zu. Sie geht ueber `expandPath`/`collapsePath` und damit ueber dieselben
Expansion-Events wie ein Mausklick, sodass der neue Zustand genauso in `custom.yaml` landet.
Die Regeln von REQ-087 gelten unveraendert: nur bei genau einer selektierten Zeile, und nicht,
solange das Speed-Search-Popup offen ist.
