---
id: REQ-072
titel: Zuruecksetzen der persoenlichen Abweichungen
kategorie: persistenz
verifiziert: visuell
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/actions/GradleTaskResetExpandStateAction.kt
  - src/main/kotlin/de/berger/gradletasksview/persistence/GradleTasksViewConfigStore.kt
quelle: Erweiterung ueber Plan.md hinaus
---

Ein Toolbar-Button loescht `custom.yaml`, sodass wieder ausschliesslich `default.yaml` gilt.

Zurueckgesetzt wird damit alles, was in dieser Datei steht: Expand/Collapse-Zustand, eigene
Icons, der Feststellbutton der Modulgliederung (REQ-074) und persoenliche
ausgeblendeten Knoten (REQ-075). Das ist konsistent - alles vom Benutzer Gesetzte liegt an
einem Ort -, geht aber ueber den Expand-Zustand hinaus und soll deshalb bewusst so benannt
sein.

Das ist der einzige Weg, persoenliche Abweichungen zurueckzusetzen - insbesondere geschieht
das **nicht** mehr automatisch beim Wechsel in den Modus `Default`, siehe ADR-003.
