---
id: REQ-005
titel: Ordner- und Task-Knoten haben Icons
kategorie: darstellung
verifiziert: visuell
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTreeCellRenderer.kt
  - src/main/resources/META-INF/plugin.xml
  - src/main/kotlin/de/berger/gradletasksview/icons/GradleTasksIcons.kt
  - src/main/resources/icons/
quelle: Plan.md, Abschnitt "Zielverhalten / Nested"
---

Gruppenzeilen (`Sort`) und virtuelle Ordner (`Default`/`Nested`) tragen dasselbe Ordner-Icon,
das dem Auf-/Zuklappzustand folgt; Tasks tragen ein eigenes Task-Icon. Die Beschriftung von
Containern ist fett, die Task-Beschreibung erscheint grau dahinter.

Modulknoten (REQ-074) und der `Tasks`-Strukturknoten des `Default`-Modus (REQ-077) haben je
ein eigenes Symbol - beide sind keine erfundene Gliederung und sollen auf einen Blick von
einem Ordner unterscheidbar sein.

Abweichend von der urspruenglichen Vorgabe (`AllIcons.Nodes.Folder`) kommen plugin-eigene
SVG-Ressourcen zum Einsatz - Begruendung siehe ADR-004, technische Ursache siehe ADR-005.

## Streifen-Icon des ToolWindows

Am seitlichen Streifen steht der Elefant aus dem Info-Dialog (REQ-080), zugeschnitten auf den
Kopf: Die ganze Figur mit Hantel zerfaellt bei 13 px zu einem unlesbaren Fleck, waehrend Ohren
und Ruessel auch dort noch als Elefant zu erkennen sind. Mitgeliefert werden `toolWindowElefant.png`
(13 px) und `toolWindowElefant@2x.png` (26 px) - PNG statt SVG, weil die Vorlage ein Bild ist
und keine Vektorgrafik. Das frueher hier verwendete Baumsymbol ist entfallen; es steht in der
Git-Historie.
