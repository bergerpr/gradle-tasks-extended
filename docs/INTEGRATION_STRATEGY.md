# Integration Strategy (final)

Ergebnis des technischen Rechercheauftrags aus Plan.md, Abschnitt "Technischer
Rechercheauftrag vor Implementierungsstart". Alle Fundstellen wurden gegen die
`master`-Quellen von [`JetBrains/intellij-community`](https://github.com/JetBrains/intellij-community)
verifiziert (Stand: August 2026).

## Datenquelle fuer Tasks

- `com.intellij.openapi.externalSystem.service.project.manage.ProjectDataManager`
  `.getInstance().getExternalProjectsData(project, GradleConstants.SYSTEM_ID)`
  liefert `Collection<ExternalProjectInfo>` fuer bereits importierte Gradle-Projekte,
  **ohne** einen neuen Sync auszuloesen. Signatur verifiziert in
  `platform/external-system-api/.../ProjectDataManager.java`.
- `ExternalProjectInfo.getExternalProjectStructure()` liefert die Wurzel
  `DataNode<ProjectData>` des importierten Projektbaums.
- `com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.findAllRecursively(node, key)`
  durchsucht den gesamten Teilbaum rekursiv (im Gegensatz zu `findAll`, das nur
  direkte Kinder durchsucht) und liefert `Collection<DataNode<T>>`. Wird mit
  `ProjectKeys.TASK` (`Key<TaskData>`, siehe `ProjectKeys.java`) verwendet.
- `TaskData` (Paket `com.intellij.openapi.externalSystem.model.task`) stellt
  `getName()`, `getGroup()`, `getDescription()`, `getLinkedExternalProjectPath()`
  und `isInherited()` bereit. Es gibt **keinen** eigenen Gradle-Projektpfad
  (`:module:task`) auf `TaskData` selbst - dieser muesste ueber den Elternknoten
  (`DataNode<ModuleData>`) hergeleitet werden. Fuer dieses Projekt wird daher
  `gradleProjectPath` vorerst identisch zu `externalProjectPath` gesetzt (wie im
  Plan-Geruest), mit TODO zur Verfeinerung anhand eines echten Multi-Modul-Testprojekts
  (siehe Implementierungsschritt 4 im Plan).

## Task-Ausfuehrung

- `ExternalSystemUtil.runTask(ExternalSystemTaskExecutionSettings, executorId, project, systemId)`
  (die im Plan skizzierte 4-Parameter-Variante) ist in der aktuellen Quelle mit
  `@ApiStatus.Obsolete` markiert.
- Aktuelle, nicht veraltete API: `ExternalSystemUtil.runTask(TaskExecutionSpec)`,
  aufgebaut ueber den Builder `TaskExecutionSpec.create()` mit
  `.withProject(project).withSystemId(systemId).withExecutorId(executorId).withSettings(settings).build()`.
  Interface und Builder liegen in `com.intellij.openapi.externalSystem.util.task`
  (Kotlin-Quellen, `TaskExecutionSpec.kt` / `TaskExecutionSpecBuilder.kt`).
- `ExternalSystemTaskExecutionSettings` besitzt `externalProjectPath`, `taskNames`
  (`List<String>`) und `externalSystemIdString` als Kotlin-Properties (Java-Getter/Setter),
  exakt wie im Plan skizziert.
- **Entscheidung:** `GradleTaskExecutor` nutzt die moderne `TaskExecutionSpec`-API statt
  der veralteten 4-Parameter-Ueberladung, gemaess Plan-Vorgabe "APIs duerfen die
  aktuellste stabile Form nutzen".

## ToolWindow-Registrierung

- Eigenstaendiges `toolWindow`-Extension-Element in `plugin.xml` mit eigener `id`,
  `factoryClass` (implementiert `com.intellij.openapi.wm.ToolWindowFactory`) und
  `anchor="right"`. Keine Registrierung innerhalb der bestehenden Gradle-ToolWindow-Gruppe,
  da Weg B laut Plan bewusst keine fremden Komponenten patcht.
- Initialer Tree-Aufbau erfolgt ueber eine `postStartupActivity`
  (`com.intellij.openapi.startup.ProjectActivity`, `suspend fun execute(project)`),
  damit Daten auch dann geladen werden, wenn das ToolWindow noch nicht sichtbar ist.

## Speed-Search-Einbindung

- `com.intellij.ui.TreeSpeedSearch` wird direkt auf dem eigenen `JTree` installiert
  (`TreeSpeedSearch.installOn(tree) { treePath -> ... }`), matched gegen `taskName`.
- Threading: laut Plan werden Filterergebnisse **vor** dem Aufbau von `Sort`/`Nested`
  angewendet, damit gefilterte Tasks nicht durch die Verschachtelung erneut erscheinen.

## Threading

- `com.intellij.openapi.application.readAction { ... }` und `writeAction { ... }`
  (suspend-Top-Level-Funktionen, Paket `com.intellij.openapi.application`) sowie
  `kotlinx.coroutines.Dispatchers.EDT` sind in der aktuellen Plattform vorhanden und
  entsprechen exakt der im Plan geforderten Coroutinen-API (kein `runReadAction`-Legacy-Stil).
  Verifiziert in `platform/core-api/.../coroutines.kt`.

## Plugin-ID der Basis-Gradle-Integration (Korrektur gegenueber Plan.md)

Ein tatsaechlicher Kompilierlauf gegen IntelliJ IDEA Ultimate 2026.2 (Build 262.8665.258)
zeigte: Die im Plan durchgaengig verwendete ID `org.jetbrains.plugins.gradle` bezeichnet in
dieser Version **nicht mehr** die Basis-Gradle-Integration, sondern nur noch das optionale
Sub-Plugin "Gradle for Java" (Name im Marketplace weiterhin "Gradle"-nah, aber eigenstaendige
ID). Klassen wie `GradleConstants` liegen im Modul `intellij.gradle`, das zum Plugin mit der
ID **`com.intellij.gradle`** gehoert. Ohne diese Korrektur schlaegt die Kompilierung mit
`Unresolved reference 'GradleConstants'` fehl, da `bundledPlugin("org.jetbrains.plugins.gradle")`
nur die Jars von "Gradle for Java" auf den Compile-Classpath legt, nicht die der Basis-Integration.

**Korrektur:**
- `build.gradle.kts`: `bundledPlugin("com.intellij.gradle")` statt `bundledPlugin("org.jetbrains.plugins.gradle")`.
- `plugin.xml`: `<depends>com.intellij.gradle</depends>` statt `<depends>org.jetbrains.plugins.gradle</depends>`.
- Die Java-Package-Namen der Gradle-Klassen selbst (`org.jetbrains.plugins.gradle.util.GradleConstants`
  etc.) sind von dieser ID-Umbenennung unberuehrt und bleiben wie im Plan beschrieben.

## Geloeste Bugs aus der manuellen Testsitzung

- **Toolbar-Aktionen (Expand All/Collapse All) wirkungslos:** `ActionToolbar.targetComponent`
  war auf das umschliessende Panel (`GradleTasksExtendedPanel`) statt auf den eigentlichen
  `JTree` gesetzt. Unsere Aktionen ermitteln den Baum ueber
  `PlatformDataKeys.CONTEXT_COMPONENT`, das sich aus `targetComponent` ableitet - mit dem
  Panel als Ziel lieferte das immer `null`. Das Kontextmenue war nicht betroffen, da
  `PopupHandler.installPopupMenu(this, group, place)` (aufgerufen mit `this` = der Baum
  selbst) dort bereits korrekt das Ziel setzt. Fix: `toolbar.targetComponent = tree`.

- **"Rechtsklick klappt alle Ordner wieder auf" (Nested-Modus):** Kein tatsaechliches
  Rechtsklick-Verhalten, sondern ein periodischer "Phantom-Refresh" alle ca. 4-7 Sekunden.
  Ursache: `com.intellij.ui.TreeSpeedSearch`/`SpeedSearchBase` feuert intern periodisch ein
  `PropertyChangeEvent` fuer `ENTERED_PREFIX_PROPERTY_NAME` mit `oldValue=null` UND
  `newValue=null` (kein tatsaechlicher Aenderungswert). `java.beans.PropertyChangeSupport`
  unterdrueckt Events nur, wenn `oldValue` **nicht-null** ist und `oldValue.equals(newValue)`
  gilt - bei `oldValue=null` wird IMMER gefeuert, selbst wenn `newValue` ebenfalls `null`
  ist. Unser `speedSearch.addChangeListener { onFilterTextChanged(...) }` reagierte darauf
  blind mit einem vollstaendigen Rebuild via `GradleTaskViewRefresher`, und
  `applyPersistedExpansionState()` klappt in Sort/Default-Modus grundsaetzlich alles auf
  (Nested-Modus haette technisch die korrekten `custom.yaml`-Werte respektiert, aber jeder
  Rebuild macht einen manuellen Collapse-Klick durch das erneute Aufklappen wertlos, sobald
  der naechste Phantom-Refresh dazwischenfunkt). Der Rechtsklick "verursachte" das Symptom
  also nur durch zeitliches Zusammentreffen mit diesem Timer, nicht durch eigene Logik.
  Fix: in `GradleTasksTree` einen `lastKnownFilterText`-Wert selbst mitfuehren und
  `onFilterTextChanged` nur bei tatsaechlicher Textaenderung aufrufen, statt jedem
  gefeuerten Event blind zu vertrauen.
  - Gefunden wurde die wahre Haeufigkeit erst, nachdem auffiel, dass IntelliJs
    `Logger.warn(message, throwable)` wiederholte Aufrufe mit identischem Stacktrace nach
    dem zweiten Mal stillschweigend unterdrueckt ("Suppressed a frequent exception logged
    for the Nth time") - ein generisches `Throwable("stacktrace")` als zweites Argument
    verschleierte dadurch zunaechst die tatsaechliche Aufruffrequenz. Fuer kuenftiges
    Debug-Logging in dieser Codebase: eindeutige, sich aendernde Nachrichtentexte
    (z. B. mit Zaehler) statt eines konstanten Throwable-Arguments verwenden.
  - Nebenbei wurde in `GradleTasksTreeStateManager.applyPersistedExpansionState()` ein
    zweiter, unabhaengiger Bug behoben: die Traversierung stieg unbedingt in die Kinder
    JEDES Knotens ab, auch wenn ein Ordner selbst zugeklappt bleiben sollte - das rief
    `tree.expandPath()` fuer Kinder zugeklappter Ordner auf, was `JTree` dazu bringen kann,
    fehlende Vorfahren zwecks Darstellbarkeit mit aufzuklappen. Jetzt wird nur noch in
    Kinder eines Knotens abgestiegen, wenn dieser selbst aufgeklappt wird.

- **Tasks erscheinen nicht direkt beim Start (leere Baumansicht, bis der Benutzer manuell
  den Modus wechselt):** Der allererste, durch `GradleTaskViewRefresher.attach()`
  ausgeloeste Refresh kann laufen, bevor die bereits importierten Gradle-Projektdaten ueber
  `ProjectDataManager` tatsaechlich verfuegbar sind - insbesondere, wenn das ToolWindow beim
  Projektstart automatisch aus einer vorherigen Sitzung wiederhergestellt wird und dadurch
  sehr frueh sichtbar wird. Da in diesem Fall kein NEUER Gradle-Sync stattfindet (die Daten
  werden nur aus dem Cache uebernommen), feuert `ProjectDataImportListener.onImportFinished`
  nicht zuverlaessig, und es gibt kein anderes Event, auf das zuverlaessig gewartet werden
  koennte. Der zuvor ergaenzte `onFirstShown`-Hook half nicht in jedem Fall, da ein bereits
  wiederhergestelltes ToolWindow sofort als "sichtbar" gilt und daher keinen zusaetzlichen
  zeitlichen Abstand zum initialen Refresh bringt.
  Fix: `GradleTaskViewRefresher.scheduleInitialRefreshWithRetry()` wiederholt den ersten
  Refresh bei leerem Ergebnis automatisch mit steigendem Abstand (500ms, 1s, 2s, 4s), bis
  entweder Tasks gefunden werden oder die Versuche aufgebraucht sind - pragmatischer Ansatz,
  der ohne exaktes Wissen ueber den tatsaechlichen Bereitschafts-Zeitpunkt auskommt.

## YAML-basierte Expand/Collapse-Konfiguration (Erweiterung ueber Plan.md hinaus)

Auf expliziten Wunsch wurde eine zweistufige, dateibasierte Konfiguration des
Expand/Collapse-Zustands ergaenzt (nicht Teil des urspruenglichen Plans):

- `<Projektwurzel>/gradle/gradle_tasks_extended/default.yaml`: Team-weite, versionierbare
  Vorgabe. Wird nur angelegt, wenn sie noch nicht existiert (Existenzpruefung via
  `File.exists()` in `GroupExpandYaml.createStarterIfMissing`) - eine bereits vorhandene
  Datei wird nie ueberschrieben.
- `<Projektwurzel>/gradle/gradle_tasks_extended/custom.yaml`: Persoenliche Abweichungen,
  entstehen automatisch beim manuellen Auf-/Zuklappen. Hat Vorrang vor `default.yaml`.
- Format bewusst **flach** gehalten (keine verschachtelte `groups:`-Wrapper-Struktur):
  einfach `<Gruppen-/Ordnerpfad>: true|false` pro Zeile, geschrieben via SnakeYAML
  (`org.yaml:snakeyaml:2.6`, als eigene Abhaengigkeit gebuendelt - IntelliJ-Plugin-
  Klassenlader sind pro Plugin isoliert, daher unproblematisch bzgl. Versionskonflikten
  mit einer eventuellen Plattform-eigenen SnakeYAML-Version). Verifiziert per direktem
  SnakeYAML-Test: das erzeugte Format ist syntaktisch einwandfreies, minimalistisches YAML.
- Ein "Reset to Default"-Toolbar-Button (`AllIcons.General.Reset`) loescht `custom.yaml`.
- **Gilt fuer alle drei Ansichtsmodi** (Default/Sort/Nested), nicht nur Nested - das war
  eine bewusste Erweiterung gegenueber der ersten Implementierung, die sich noch an
  Plan.md orientierte ("Fuer Default/Sort werden Gruppenzeilen immer aufgeklappt
  dargestellt"). Als Konsequenz wurde auch das automatische Zuruecksetzen der
  Expand-Abweichungen beim Wechsel in den `Default`-Modus entfernt (`switchMode()` in
  `GradleTaskViewRefresher`), da es sonst die gerade erst modusuebergreifend nutzbare
  Konfiguration bei jedem Wechsel zunichtegemacht haette. Zuruecksetzen erfolgt seither
  ausschliesslich explizit ueber den Reset-Button.
- `GradleTasksTreeStateManager` ermittelt den Konfigurationsschluessel eines Knotens
  unabhaengig vom Modus ueber `GroupHeaderNodeData.groupPath` (Default/Sort) bzw.
  `FolderNodeData.folder.fullPath` (Nested) - beides derselbe von Menschen lesbare,
  Slash-getrennte Pfad wie in den YAML-Dateien.

## Manuelles Testen in der Sandbox (`./gradlew runIde`)

Die projektlokale, **persoenliche** Konfiguration darf beim Testen jederzeit zurueckgesetzt
oder geloescht werden - sie muss vor einem Sandbox-Lauf nicht gesichert und danach nicht
wiederhergestellt werden. Erwartet wird lediglich, dass eine solche Aenderung im
Testergebnis erwaehnt wird. Betroffen sind:

- `.idea/gradleTasksView.xml` - der persistierte Ansichtsmodus (`Default`/`Sort`/`Nested`).
  Muss ohnehin umgestellt werden, um alle drei Modi zu verifizieren, und wird beim Fehlen
  automatisch auf den Default-Wert `DEFAULT` zurueckgesetzt.
- `gradle/gradle_tasks_extended/custom.yaml` - die persoenlichen Expand/Collapse-
  Abweichungen. Wird beim naechsten manuellen Auf-/Zuklappen automatisch neu geschrieben;
  ohne die Datei gilt `default.yaml`.

Ein Sichern/Wiederherstellen dieser Dateien ist auch technisch nicht verlaesslich: Die
laufende Sandbox-IDE schreibt bzw. entfernt `.idea/gradleTasksView.xml` beim Beenden selbst.

Ausgenommen ist `gradle/gradle_tasks_extended/default.yaml` - die team-weite, versionierbare
Vorgabe, die das Plugin bewusst nie ueberschreibt. Sie wird wie jede andere versionierte
Projektdatei behandelt.

Praktischer Hinweis: Eine noch laufende Sandbox-IDE haelt `snakeyaml-2.6.jar` im Sandbox-
`plugins/`-Verzeichnis gesperrt und laesst dadurch `prepareSandbox` (und damit `buildPlugin`
und `runIde`) mit einem Kopierfehler fehlschlagen. Vor einem Rebuild also den vorherigen
Sandbox-Prozess wirklich beenden.

## Bekannte Risiken

- `gradleProjectPath`-Herleitung ist ein Platzhalter (siehe oben) und muss gegen ein
  echtes Multi-Modul-/Composite-Build-Testprojekt verifiziert werden, bevor
  `stableId`-Eindeutigkeit fuer identische Tasknamen in Subprojekten final verlaesslich ist.
- Die "Unified IntelliJ IDEA"-Aenderung (ab 2025.3 keine separate Community/Ultimate-
  Distribution mehr, siehe JetBrains-Blog "The Unified IntelliJ IDEA") aendert nichts an
  der Gradle-Plugin-Konfiguration: `IntelliJPlatformType.IntellijIdeaUltimate` /
  `intellijIdeaUltimate(version)` existieren in IntelliJ Platform Gradle Plugin 2.18.1
  weiterhin unveraendert als Zielplattform-Typ fuer den Build.
- IntelliJ Platform Gradle Plugin 2.18.1 benoetigt Gradle >= 9.0.0 (nicht 8.13, wie
  in aelterer Dokumentation vereinzelt kolportiert) - im Projekt-Wrapper auf Gradle 9.7.1
  fixiert.
- **Geloest: Tree-Row-Icons wurden nicht gezeichnet (`updateUI()` nach `setCellRenderer`).**
  Symptom war ueber mehrere Sitzungen: Task- und Gruppennamen erschienen korrekt, aber ohne
  jedes Icon - auch nicht mit anderen Plattform-Icons oder einem manuell gemalten Testicon.
  Die eigentliche Ursache liegt nicht beim Icon, sondern beim Renderer:

  `com.intellij.ui.treeStructure.Tree` ruft **in seinem eigenen Konstruktor** bereits
  `setCellRenderer(new NodeRenderer())` auf. Ein spaeteres `setCellRenderer(...)` aus der
  eigenen Unterklasse aktualisiert zwar `JTree.getCellRenderer()`, aber **nicht** das Feld
  `currentCellRenderer` in `javax.swing.plaf.basic.BasicTreeUI` - und genau dieses Feld liest
  `com.intellij.ui.tree.ui.DefaultTreeUI.getRenderer(...)` beim Zeichnen jeder Zeile. Der Baum
  zeichnete also dauerhaft mit `NodeRenderer` weiter, dessen Ausgabe fuer eigene
  `DefaultMutableTreeNode`-Nutzdaten genau `toString()` ohne Icon ist. `GradleTasksTreeCellRenderer`
  wurde nie aufgerufen.

  Nachgewiesen per Reflection auf `BasicTreeUI.currentCellRenderer` in einer laufenden Sandbox:

  ```
  vor  setCellRenderer(...):  ui.currentCellRenderer = com.intellij.ide.util.treeView.NodeRenderer
  nach setCellRenderer(...):  ui.currentCellRenderer = com.intellij.ide.util.treeView.NodeRenderer   <- Ursache
  nach updateUI():            ui.currentCellRenderer = de.example...GradleTasksTreeCellRenderer      <- Fix
  ```

  Fix: in `GradleTasksTree.init` direkt nach `cellRenderer = GradleTasksTreeCellRenderer()`
  einmal `updateUI()` aufrufen. Das installiert die TreeUI neu, deren `updateRenderer()` den
  aktuell gesetzten Renderer uebernimmt.

  Damit sind rueckwirkend auch die frueher als "ausgeschlossen" protokollierten Beobachtungen
  erklaerbar: Icon-Objekt, Icon-Groesse, Zeilenhoehe, Theme, Repaint und Plattformversion waren
  tatsaechlich alle unschuldig - der Renderer, in dem das Icon gesetzt wurde, lief schlicht nie.
  Ein zusaetzliches, gut sichtbares Symptom desselben Fehlers war, dass Gruppen-/Ordnernamen
  nicht fett und Task-Descriptions gar nicht dargestellt wurden.

- **Ordner-Icon ist modusuebergreifend identisch.** Gruppenzeilen in `Default`/`Sort`
  (`GroupHeaderNodeData`) und virtuelle Ordner in `Nested` (`FolderNodeData`) verwenden dasselbe
  Ordner-Icon inklusive Wechsel zwischen zu- und aufgeklappt. Aus Benutzersicht ist beides
  derselbe Knotentyp - ein aufklappbarer Container fuer Tasks -, lediglich die Beschriftung
  unterscheidet sich (voller Gruppenpfad vs. einzelnes Pfadsegment). Ein separates
  Gruppen-Icon gab es kurzzeitig, wurde aber bewusst wieder entfernt.

- **Eigene Icons statt Plattform-Icons.** Die Baum- und ToolWindow-Icons kommen inzwischen aus
  `src/main/resources/icons/` (siehe `icons/GradleTasksIcons.kt` und
  `src/main/resources/icons/LICENSE-icons.txt`), abgeleitet von einer Icon-Sammlung unter MIT-Lizenz.
  Zu jedem Icon existiert eine `_dark`-Variante, die die Plattform bei dunklen Themes
  automatisch waehlt. Geladen wird bewusst mit `IconLoader.findIcon(path, classLoader)`
  (nullable) statt `IconLoader.getIcon(...)`: Letzteres liefert bei falschem Pfad still ein
  leeres, transparentes Platzhalter-Icon zurueck, das in Logs wie ein gueltiges 16x16-Icon
  aussieht - genau die Art von Fehler, die die Diagnose oben so lange verschleiert hat.
  Bei `null` wird stattdessen ein auffaelliges magentafarbenes Ersatzquadrat gezeichnet und
  eine Warnung geloggt.
