---
id: REQ-023
titel: Case-insensitive Sortierung mit stabiler Zweitsortierung
kategorie: daten
verifiziert: unit
betrifft:
  - src/main/kotlin/de/example/gradletasksview/model/TaskTreeSorting.kt
quelle: Plan.md, "Zu klaerende Produktdetails"
---

Sortiert wird case-insensitive, mit einer stabilen Zweitsortierung ueber den Originalwert.

Ohne die Zweitsortierung bekaemen Gross-/Kleinschreibungs-Duplikate wie `Build` und `build`
bei jedem Rebuild potenziell eine andere relative Reihenfolge - sichtbar als Sortierflackern.
