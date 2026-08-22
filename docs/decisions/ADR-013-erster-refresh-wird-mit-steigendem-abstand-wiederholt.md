---
id: ADR-013
titel: Erster Refresh wird mit steigendem Abstand wiederholt
betrifft:
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTaskViewRefresher.kt
  - src/main/kotlin/de/example/gradletasksview/ui/GradleTasksTree.kt
quelle: Ursachenanalyse aus einer manuellen Testsitzung
---

`scheduleInitialRefreshWithRetry()` wiederholt den allerersten Refresh bei leerem Ergebnis
automatisch mit steigendem Abstand (500 ms, 1 s, 2 s, 4 s).

Ohne das blieb die Baumansicht direkt nach dem IDE-Start leer, bis der Benutzer von Hand den
Modus wechselte. Der von `attach()` ausgeloeste erste Refresh kann laufen, bevor die bereits
importierten Projektdaten ueber `ProjectDataManager` verfuegbar sind - besonders, wenn das
ToolWindow beim Projektstart aus einer vorherigen Sitzung wiederhergestellt wird.

Auf ein Event zu warten funktioniert hier nicht: Da kein **neuer** Sync stattfindet, sondern
nur der Cache uebernommen wird, feuert `ProjectDataImportListener.onImportFinished` nicht
zuverlaessig, und ein anderes verlaessliches "Daten bereit"-Signal existiert nicht. Auch der
`onFirstShown`-Hook half nicht, weil ein wiederhergestelltes ToolWindow sofort als sichtbar
gilt und damit keinen zeitlichen Abstand bringt.

Der Backoff ist bewusst pragmatisch: Er kommt ohne exaktes Wissen ueber den tatsaechlichen
Bereitschaftszeitpunkt aus.
