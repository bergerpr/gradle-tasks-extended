---
id: REQ-078
titel: Einzelne Knoten ueber das Kontextmenue ausblenden
kategorie: interaktion
verifiziert: konstruktiv
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/actions/GradleTasksTreeContextActions.kt
  - src/main/kotlin/de/berger/gradletasksview/actions/GradleTaskShowHiddenToggleAction.kt
  - src/main/kotlin/de/berger/gradletasksview/persistence/GradleTasksViewConfigStore.kt
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTaskViewRefresher.kt
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTreeCellRenderer.kt
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTree.kt
quelle: OPEN-008
---

Ein Rechtsklick auf einen Knoten bietet **Hide** an. Der Knoten verschwindet aus der Ansicht,
und zwar **nur an dieser Stelle**: Derselbe Gruppenpfad oder Taskname in einem anderen Modul
bleibt sichtbar. Festgehalten wird das als `hidden: true` in `custom.yaml` (REQ-075), der
Ausschluss ueberlebt also einen IDE-Neustart.

Wo der Eintrag landet, ergibt sich aus dem Schluesselraum (REQ-076): Ist die Modulgliederung
an, adressiert der angeklickte Knoten einen Modulblock; ist sie aus, hat er keinen Modulbezug,
und der Eintrag blendet ihn in allen Modulen aus. Das ist keine Einschraenkung, sondern die
einzige sinnvolle Lesart - in dieser Ansicht *ist* die Zeile die Zusammenfassung aller Module.

Die Aktion wirkt auf die gesamte Selektion. Knoten ohne Konfigurationsschluessel - der
`Tasks`-Knoten des `Default`-Modus (REQ-077) - werden uebersprungen, statt die Aktion fuer die
ganze Auswahl zu sperren.

Geschrieben wird auch hier nur eine echte Abweichung (REQ-070): Ein Knoten, der bereits durch
ein ausgeblendetes Modul verschwindet, bekommt keinen eigenen Eintrag.

## Zurueckholen

Ein Feststellbutton in der Toolbar, **Show Hidden**, zeigt ausgeblendete Knoten wieder an -
ausgegraut, damit der Unterschied zum regulaeren Inhalt sofort erkennbar ist. Auf ihnen heisst
der Kontextmenueeintrag dann **Show Again** und entfernt den Eintrag wieder bzw. traegt ein
`hidden: false` ein, wenn eine allgemeinere Regel dagegensteht.

Ohne diesen Weg gaebe es nur "Reset to Default" (REQ-072), das Aufklappzustand, Icons und
Modulgliederung mit zuruecknehmen wuerde - fuer einen einzelnen Ordner ein grober Behelf.

Der Zustand des Buttons ist reine Ansichtssache und wird **nicht** persistiert: Er gehoert zum
Aufraeumen, nicht zur Konfiguration. Beim naechsten Start sieht der Baum wieder so aus, wie er
eingerichtet wurde.

Bei gemischter Auswahl - teils sichtbare, teils ausgeblendete Knoten - blendet die Aktion aus.
Das laesst sich anschliessend geschlossen wieder zuruecknehmen, waehrend die Gegenrichtung die
Auswahl auseinanderrisse.
