---
id: ADR-026
titel: Die Baum-Icons kommen von der Plattform
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/icons/GradleTasksIcons.kt
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTreeCellRenderer.kt
ersetzt_vorgabe: ADR-004 (plugin-eigene Icons als SVG-Ressourcen)
quelle: Wunsch des Benutzers - die Icons der IDE statt eigener Dateien
---

Die eingebauten Icons des Baums sind die der originalen Gradle-Ansicht der IDE:

| Knoten | Icon |
|---|---|
| Task | `ExternalSystemIcons.Task` |
| Gruppe (`Sort`), Ordner (`Default`/`Nested`) | `AllIcons.Nodes.ConfigFolder` |
| `Tasks`-Strukturknoten (`Default`, REQ-077) | `AllIcons.Nodes.ConfigFolder` |
| Modul | `AllIcons.Nodes.Module` |

Das Plugin referenziert diese Konstanten nur und liefert keine Bilddatei dafuer mit. Die
Icons passen so zum Rest der IDE, und Hell/Dunkel sowie New UI waehlt die Plattform selbst.

Der Grund fuer ADR-004 ist entfallen: Die Plattform-Icons wurden damals nicht gezeichnet, weil
der eigene Renderer nie lief (ADR-005) - nicht wegen der Icons. Dass eine Konstante in einer
kuenftigen IDE-Version verschwindet, faellt beim Kompilieren gegen diese Version auf; das
Plugin zielt ohnehin nur auf die neueste. Alle vier sind in 2026.2 weder deprecated noch
`@ApiStatus.Obsolete` oder `Internal` (gegen die Plattform-Jars geprueft).

Gruppen und Ordner haben **ein** Icon fuer beide Zustaende: Eine aufgeklappte Variante gibt es
in der Plattform nicht mehr, und Projekt- und Gradle-Ansicht zeigen aufgeklappt dasselbe
Symbol. Der `Tasks`-Knoten traegt wie im Original dasselbe Icon wie eine Gruppe; er bleibt
trotzdem erkennbar, weil er nur direkt unter einem Modul steht und immer `Tasks` heisst.

Ausgenommen bleibt das Streifen-Icon des ToolWindows - der Elefant ist ein eigenes Bild des
Plugins (REQ-005).
