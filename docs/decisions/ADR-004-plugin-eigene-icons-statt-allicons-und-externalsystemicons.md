---
id: ADR-004
titel: Plugin-eigene Icons statt AllIcons und ExternalSystemIcons
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/icons/GradleTasksIcons.kt
  - src/main/resources/icons/
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTreeCellRenderer.kt
ersetzt_vorgabe: Plan.md gab AllIcons.Nodes.Folder vor
quelle: Erweiterung ueber Plan.md hinaus
---

**Abgeloest durch ADR-026.** Die Baum-Icons sind wieder die der Plattform, die frueheren SVGs
und ihr Lizenztext sind entfernt. Was bleibt: das Elefanten-PNG am Streifen des ToolWindows
und `AllIcons` fuer die Toolbar-Knoepfe.

Die Baum-Icons kommen als eigene SVG-Ressourcen aus `src/main/resources/icons/` (abgeleitet
von einer Icon-Sammlung, MIT), je mit `_dark`-Variante. Das Streifen-Icon des ToolWindows ist davon
ausgenommen: Dort steht seit REQ-005 der Elefant des Plugins als PNG in 13 und 26 px, weil die
Vorlage ein Bild und keine Vektorgrafik ist; eine `_dark`-Variante gibt es nicht, das farbige
Icon passt in beide Themes.

So haengt die Darstellung weder davon ab, unter welchem Plattformmodul ein Icon gerade
ausgeliefert wird, noch davon, ob eine `AllIcons`-Konstante in der jeweiligen IDE-Version noch
auf eine existierende Ressource zeigt.

Geladen wird bewusst mit `IconLoader.findIcon(path, classLoader)` statt `getIcon(...)`:
Letzteres liefert bei falschem Pfad still ein leeres, transparentes Platzhalter-Icon zurueck,
das in Logs wie ein gueltiges 16x16-Icon aussieht. Bei `null` zeichnet das Plugin stattdessen
ein auffaelliges magentafarbenes Ersatzquadrat und warnt - ein Ressourcenproblem soll
unuebersehbar sein, nicht getarnt.

Toolbar-Buttons behalten dagegen `AllIcons` (Expand All, Collapse All, Reset, Group By): Das
sind IDE-weit einheitliche Symbole, deren Wiedererkennbarkeit hoeher wiegt als
Gestaltungsfreiheit.
