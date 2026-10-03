---
id: REQ-005
titel: Ordner- und Task-Knoten haben Icons
kategorie: darstellung
verifiziert: visuell
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTreeCellRenderer.kt
  - src/main/resources/META-INF/plugin.xml
  - src/main/kotlin/de/berger/gradletasksview/icons/GradleTasksIcons.kt
quelle: Plan.md, Abschnitt "Zielverhalten / Nested"
---

Gruppenzeilen (`Sort`) und virtuelle Ordner (`Default`/`Nested`) tragen dasselbe Ordner-Icon,
aufgeklappt wie zugeklappt; Tasks tragen ein eigenes Task-Icon. Die Beschriftung von Containern
ist fett, die Task-Beschreibung erscheint grau dahinter.

Modulknoten (REQ-074) haben ein eigenes Symbol, denn ein Modul ist keine erfundene Gliederung.
Der `Tasks`-Strukturknoten des `Default`-Modus (REQ-077) traegt wie in der originalen
Gradle-Ansicht das Gruppen-Icon.

Alle diese Icons sind die der originalen Gradle-Ansicht und kommen von der Plattform
(`ExternalSystemIcons.Task`, `AllIcons.Nodes.ConfigFolder`, `AllIcons.Nodes.Module`); das
Plugin liefert dafuer keine eigenen Dateien und damit auch keine fremde Lizenz mit (ADR-026).
Ein ueber die YAML-Konfiguration vergebenes Icon hat Vorrang (REQ-071).

## Streifen-Icon des ToolWindows

Am seitlichen Streifen steht der Elefant aus dem Info-Dialog (REQ-080), zugeschnitten auf den
Kopf: Die ganze Figur mit Hantel zerfaellt bei 13 px zu einem unlesbaren Fleck, waehrend Ohren
und Ruessel auch dort noch als Elefant zu erkennen sind. Mitgeliefert werden `toolWindowElefant.png`
(13 px) und `toolWindowElefant@2x.png` (26 px) - PNG statt SVG, weil die Vorlage ein Bild ist
und keine Vektorgrafik. Das frueher hier verwendete Baumsymbol ist entfallen; es steht in der
Git-Historie.
