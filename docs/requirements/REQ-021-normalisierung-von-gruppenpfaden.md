---
id: REQ-021
titel: Normalisierung von Gruppenpfaden
kategorie: daten
verifiziert: unit
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/util/GradleTaskPathUtil.kt
quelle: Plan.md, "Zu klaerende Produktdetails"
---

`/` ist der einzige Separator. Fuehrende, folgende und doppelte Schraegstriche werden
entfernt bzw. zusammengefasst, leere Segmente ignoriert.

Segmente werden lediglich getrimmt und ansonsten unveraendert als Ordnername verwendet -
keine weitere Normalisierung von Leerzeichen, Unicode oder Backslashes, um Informationsverlust
zu vermeiden.
