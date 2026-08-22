---
id: REQ-072
titel: Zuruecksetzen der persoenlichen Abweichungen
kategorie: persistenz
verifiziert: visuell
betrifft:
  - src/main/kotlin/de/example/gradletasksview/actions/GradleTaskResetExpandStateAction.kt
  - src/main/kotlin/de/example/gradletasksview/persistence/GradleTasksViewConfigStore.kt
quelle: Erweiterung ueber Plan.md hinaus
---

Ein Toolbar-Button loescht `custom.yaml`, sodass wieder ausschliesslich `default.yaml` gilt.

Das ist der einzige Weg, persoenliche Abweichungen zurueckzusetzen - insbesondere geschieht
das **nicht** mehr automatisch beim Wechsel in den Modus `Default`, siehe ADR-003.
