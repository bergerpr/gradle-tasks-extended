---
id: ADR-012
titel: Rebuild nur bei echter Aenderung des Filtertexts
betrifft:
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTree.kt
  - src/main/kotlin/de/berger/gradletasksview/ui/GradleTasksTreeStateManager.kt
quelle: Ursachenanalyse aus einer manuellen Testsitzung
---

`GradleTasksTree` fuehrt einen eigenen `lastKnownFilterText` mit und ruft
`onFilterTextChanged` nur bei einer tatsaechlichen Textaenderung auf, statt jedem gefeuerten
Change-Event zu vertrauen.

Ohne das erschien alle vier bis sieben Sekunden ein "Phantom-Refresh", der jeden manuellen
Collapse-Klick wieder aufklappte. Das Symptom wurde zunaechst dem Rechtsklick zugeschrieben,
weil es zeitlich damit zusammenfiel.

Ursache: `TreeSpeedSearch`/`SpeedSearchBase` feuert periodisch ein `PropertyChangeEvent` fuer
`ENTERED_PREFIX_PROPERTY_NAME` mit `oldValue=null` **und** `newValue=null`.
`java.beans.PropertyChangeSupport` unterdrueckt ein Event nur, wenn `oldValue` nicht-null ist
und `oldValue.equals(newValue)` gilt - bei `oldValue=null` wird immer gefeuert.

Zwei Lehren aus der Fehlersuche, die ueber diesen Fall hinaus gelten:

- IntelliJs `Logger.warn(message, throwable)` unterdrueckt wiederholte Aufrufe mit identischem
  Stacktrace nach dem zweiten Mal ("Suppressed a frequent exception logged for the Nth time").
  Ein konstantes `Throwable("stacktrace")` als zweites Argument verschleierte dadurch die
  tatsaechliche Aufruffrequenz. Fuer Debug-Logging hier also eindeutige, sich aendernde
  Nachrichtentexte verwenden, etwa mit Zaehler.
- Dabei fiel ein zweiter, unabhaengiger Fehler auf: Die Traversierung in
  `applyPersistedExpansionState()` stieg unbedingt in die Kinder jedes Knotens ab. Das rief
  `tree.expandPath()` auch fuer Kinder zugeklappter Ordner auf, was `JTree` dazu bringen kann,
  fehlende Vorfahren zwecks Darstellbarkeit mit aufzuklappen (siehe REQ-006).
