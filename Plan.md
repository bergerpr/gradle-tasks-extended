# Plan.md – IntelliJ Gradle-Task-Ansicht erweitern

> **Änderungshinweis (Review-Iteration 2):** Dieser Plan wurde nach einem unabhängigen Rubber-Duck-Review überarbeitet. Wesentliche Änderungen: Architektur wechselt von "interne Gradle-View erweitern" zu "eigene ToolWindow-Ansicht", Build-Tooling auf IntelliJ Platform Gradle Plugin 2.x fixiert, Zielprodukt auf IntelliJ IDEA Ultimate (aktuellste Version, keine Altversions-Kompatibilität) festgelegt, Marketplace-Vertrieb ausgeschlossen (private Installation via Disk-Install), Threading-Modell (Coroutinen/`readAction()`) explizit spezifiziert.

## Projekt-Rahmenbedingungen (verbindlich)

Diese Rahmenbedingungen sind vor Implementierungsstart final und dürfen nicht implizit variieren:

| Parameter | Festlegung |
|---|---|
| Zielprodukt | IntelliJ IDEA **Ultimate** (aktuellste unterstützte Version) |
| Versionsspanne | Nur die aktuelle Version wird unterstützt. Kein `untilBuild`-Zwang für ältere Linien, kein Support für ältere Major-Versionen. |
| Build-Tooling | **IntelliJ Platform Gradle Plugin 2.x** (`org.jetbrains.intellij.platform`), kein Gradle IntelliJ Plugin 1.x [web:2][web:6] |
| Vertriebsweg | **Kein** JetBrains-Marketplace-Vertrieb. Rein private Installation via "Install Plugin from Disk" (ZIP/JAR) [web:32]. Kostenpflichtiger Marketplace-Vertrieb würde 15 % Provision kosten (max. 25 % laut Vertrag) [web:21][web:17]; kostenloser Marketplace-Vertrieb ist gebührenfrei, wird hier aber ebenfalls nicht genutzt, da rein privater Gebrauch vorgesehen ist. |
| Architektur-Grundentscheidung | **Eigene ToolWindow-Ansicht** statt Erweiterung interner Gradle-View-Klassen (Begründung siehe Abschnitt "Architekturentscheidung") |
| Threading-Modell | Kotlin-Coroutinen mit `readAction()` / `writeAction()` (IntelliJ Platform ≥ 2024.1), kein `runReadAction()`-Legacy-Stil [web:8] |

---

## Ziel

Entwickle ein IntelliJ-IDEA-Plugin (Ultimate) in Kotlin, das die Gradle-Task-Darstellung um einen Toolbar-Dropdown mit drei Darstellungsmodi erweitert:

1. **Default**  
   Entspricht optisch und funktional der originalen IntelliJ-Gradle-Ansicht.

2. **Sort**  
   Gruppen und Tasks werden sortiert:
   - Gruppen nach vollständigem Group-Task-Namen lexikografisch.
   - Tasks innerhalb einer Gruppe nach Task-Namen lexikografisch.

3. **Nested**  
   Gleiche Sortierung wie in `Sort`, jedoch werden Gruppenpfade mit `/` hierarchisch als Ordnerstruktur dargestellt:
   - Beispiel: `docker/gui/react` wird zu `docker → gui → react`.
   - Tasks werden in jedem Ordner **vor** Unterordnern dargestellt.
   - Ordner erhalten ein Folder-Icon.
   - Ordner sind standardmäßig aufgeklappt.
   - Expand/Collapse-Zustand bleibt über IDE-Neustarts erhalten.

Beim Wechsel zurück zu **Default** werden alle gespeicherten Zustände dieser Erweiterung gelöscht.

Wichtig: Alle bestehenden Task-Interaktionen (Doppelklick-Ausführung, Kontextmenü, Copy, Filterung, Selektion, Tooltips) müssen in allen drei Modi funktionsfähig bleiben.

---

## Architekturentscheidung

### Zwei mögliche Wege

**Weg A – Interne Gradle-View erweitern**  
Direkter Zugriff auf JetBrains-interne Klassen (z. B. `ExternalSystemNode`, `ExternalSystemTasksTreeModel`, Package `com.intellij.openapi.externalSystem.view`). Diese sind nicht als stabile öffentliche API deklariert und können sich zwischen Versionen ändern oder verschwinden, ohne Vorwarnung oder Deprecation-Zyklus.

**Weg B – Eigene ToolWindow-Ansicht**  
Eine eigenständige `ToolWindowFactory` mit eigenem `JTree`, die sich Gradle-Task-Daten über offizielle bzw. deutlich stabilere Kanäle holt (`ExternalSystemApiUtil`, `GradleSettings`, `ExternalProjectDataCache`, ggf. Gradle Tooling API) und Darstellung, Sortierung, Verschachtelung und Persistenz vollständig selbst kontrolliert. Task-Ausführung erfolgt über offizielle `ExternalSystemUtil`-Einstiegspunkte statt über Delegation an interne View-Interna.

### Entscheidung

**Weg B wird als primäre und einzige Strategie umgesetzt.**

Begründung:

- Das Projekt ist neu, privat genutzt und muss nur die aktuellste IDE-Version unterstützen – es gibt daher keinen Zwang, sich an eine möglicherweise fragile interne API zu binden, nur um Entwicklungszeit zu sparen.
- Weg A hätte pro IDE-Update ein erneutes Verifizieren/Anpassen der internen Klassen erfordert, was bei einem privaten Tool ohne Marketplace-Support-Team unverhältnismäßig ist.
- Weg B ist zwar aufwändiger in der Ersteinrichtung (eigene Datenbeschaffung, eigenes Rendering, eigene Action-Delegation), bleibt aber über IDE-Updates hinweg deutlich stabiler, weil er sich nur auf halbwegs dokumentierte External-System-Einstiegspunkte stützt.
- Der eigene Tree hat volle Kontrolle über Icons, Sortierung, Verschachtelung und Persistenz – exakt das, was für `Sort` und `Nested` gebraucht wird, ohne interne Rendering-Klassen patchen zu müssen.

**Konsequenz für "Erhalt bestehender Funktionen":** Da wir keinen fremden Tree wiederverwenden, müssen Doppelklick-Ausführung, Kontextmenü und Copy-Aktionen in der eigenen View **nachgebaut** werden – nicht per Delegation an eine fremde Action-Instanz, sondern durch direkten Aufruf derselben offiziellen Ausführungs-APIs, die auch die originale Gradle-Ansicht nutzt (siehe Abschnitt "Task-Ausführung").

---

## Scope und Annahmen

### In Scope

- Kotlin-basierte IntelliJ-Plugin-Entwicklung für IntelliJ IDEA Ultimate (aktuellste Version).
- Eigene ToolWindow-Ansicht ("Gradle Tasks Extended" o. ä.) mit Toolbar-Dropdown.
- Modi `Default`, `Sort`, `Nested`.
- Sortierung der Gradle-Task-Gruppen und Tasks.
- Hierarchische Ordneransicht aus `/`-getrennten Gruppenpfaden.
- Persistenz von ausgewähltem Modus und Expand/Collapse-Zuständen (projektbezogen).
- Zurücksetzen dieser Daten beim Wechsel auf `Default`.
- Nachbau von Doppelklick, Kontextmenü, Copy und Filterung in der eigenen View.
- Private Installation via ZIP-Distribution, inklusive Installationsdokumentation.

### Out of Scope

- Veränderung der Gradle-Task-Definitionen oder Projektstruktur.
- Eigener Gradle-Import oder eigene Gradle-Sync-Logik (wir lesen nur bereits importierte Projektdaten).
- Unterstützung älterer IntelliJ-Versionen oder anderer JetBrains-IDEs (PyCharm, WebStorm etc.).
- JetBrains-Marketplace-Veröffentlichung.
- Umbau weiterer Gradle-Ansichten wie Dependencies, Runs oder Build Scan.
- Reflection-Zugriff auf interne, nicht-öffentliche APIs (siehe Architekturentscheidung).

### Zu klärende Produktdetails (Gate vor Implementierungsstart)

Diese Punkte müssen vor Schritt 1 der Implementierungsreihenfolge final entschieden sein:

- Sortierung case-sensitive oder case-insensitive?
  - **Festlegung:** case-insensitive, mit stabiler Zweitsortierung über den Originalwert (verhindert Sortierflackern bei Groß-/Kleinschreibungs-Duplikaten).
- Umgang mit Tasks ohne `group`?
  - **Festlegung:** eigene Gruppe `Other`, dargestellt als letzter Ordner/letzte Gruppe unabhängig von der alphabetischen Sortierung der übrigen Gruppen.
- Separator für Gruppenpfade?
  - **Festlegung:** `/` als einziger Separator; führende, folgende und doppelte `/` werden entfernt bzw. zusammengefasst; leere Segmente werden ignoriert.
- Umgang mit Sonderzeichen (Leerzeichen, Unicode, `\`) in Gruppenpfaden?
  - **Festlegung:** Segmente werden nur getrimmt, ansonsten unverändert als Ordnername verwendet. Keine zusätzliche Normalisierung, um Informationsverlust zu vermeiden.
- Modus- und Expand-State-Speicherung global oder pro Projekt?
  - **Festlegung:** pro Projekt (`Service.Level.PROJECT`), da Gradle-Gruppenstrukturen projektspezifisch sind.
- Eindeutigkeit von Tasknamen bei Composite Builds / Mehrmodul-Projekten mit identischen Namen in unterschiedlichen Subprojekten?
  - **Festlegung:** `stableId` wird immer aus der Kombination `externalProjectPath + gradleProjectPath + taskName` gebildet, niemals aus dem sichtbaren Namen allein (siehe Datenmodell).

---

## Zielverhalten

### Default

- Die Ansicht zeigt Gruppen und Tasks in der Reihenfolge, wie sie von Gradle/External System geliefert werden (keine eigene Sortierung).
- Keine virtuellen Ordnerknoten.
- Beim Wechsel zu `Default`:
  - gespeicherter Modus wird auf `DEFAULT` gesetzt,
  - gespeicherte Expand/Collapse-States für `Nested` werden gelöscht,
  - gespeicherte `knownFolderPaths` werden gelöscht,
  - die Ansicht wird ohne Sortierung und ohne Verschachtelung neu aufgebaut.

### Sort

Beispiel-Eingabe:

```text
docker/gui
docker/gui/jsp
docker/gui/react
docker/server/gui
```

Sortierreihenfolge (alphabetisch nach vollständigem Gruppenpfad):

```text
docker/gui
docker/gui/jsp
docker/gui/react
docker/server/gui
```

- Tasks innerhalb jeder Gruppe werden nach Task-Namen sortiert.
- Keine zusätzlichen Ordnerknoten – die flache Gruppenliste bleibt erhalten, nur die Reihenfolge ändert sich.

### Nested

Aus folgender logischer Task-Gruppierung:

```text
docker/gui
- build-all-gui

docker/gui/jsp
- build-jsp
- run-jsp

docker/gui/react
- build-react

docker/server/gui
- build-image
```

wird:

```text
docker/
|--- gui/
|    |--- build-all-gui
|    |--- jsp/
|    |    |--- build-jsp
|    |    |--- run-jsp
|    |--- react/
|         |--- build-react
|
|--- server/
     |--- gui/
          |--- build-image
```

Regeln:

- Jede Pfadkomponente wird als virtueller Ordnerknoten dargestellt.
- Ordner werden alphabetisch sortiert, Tasks werden alphabetisch sortiert.
- Innerhalb eines Ordners erscheinen **Tasks vor Unterordnern**.
- Ordner erhalten das IntelliJ-Standard-Folder-Icon (`AllIcons.Nodes.Folder`).
- Alle Ordner sind bei erstmaliger Anzeige aufgeklappt.
- Der Benutzer kann Ordner ein- und ausklappen; der Zustand bleibt projektbezogen und pfadbezogen über Neustarts erhalten.

---

## Technischer Rechercheauftrag vor Implementierungsstart

Auch mit der Entscheidung für Weg B (eigene View) müssen vor Codestart folgende Punkte in `intellij-community`-Quellen bzw. offizieller Doku verifiziert werden:

- Welche Klassen/Utilities liefern bereits importierte Gradle-Task-Daten zuverlässig, ohne selbst einen Sync auszulösen (Kandidaten: `ExternalSystemApiUtil.findProjectNode`, `ProjectDataManager`, `ExternalProjectDataCache`, `GradleSettings`).
- Welche offizielle API zur Task-Ausführung genutzt werden kann (Kandidat: `ExternalSystemUtil.runTask` mit `ExternalSystemTaskExecutionSettings`), damit Doppelklick-Ausführung nicht selbst neu implementiert werden muss.
- Wie Gradle-Task-Metadaten (Name, Gruppe, Beschreibung, Task-Pfad) exakt in `DataNode`-Strukturen abgelegt sind (Kandidat: `ProjectKeys.TASK`, `TaskData`).
- Ob und wie sich ein `ToolWindow` mit eigenem Icon in der Gradle-Toolwindow-Gruppe registrieren lässt oder ob es als eigenständiges Toolwindow (z. B. rechte Seitenleiste) erscheint.
- Wie Filterung/Suchleiste (`SpeedSearch` bzw. `TreeSpeedSearch`) in eigenen `JTree`-Implementierungen korrekt eingebunden wird.

Ergebnis dieser Recherche wird vor Schritt 1 der Implementierungsreihenfolge als kurze technische Notiz festgehalten:

```text
Integration Strategy (final):
- Datenquelle für Tasks: <Klasse/API>
- Task-Ausführung: <Klasse/API>
- ToolWindow-Registrierung: <Ansatz>
- Speed-Search-Einbindung: <Ansatz>
- Bekannte Risiken: <Liste>
```

---

## Plugin-Struktur

```text
src/main/kotlin/
  de/example/gradletasksview/
    GradleTasksViewBundle.kt
    toolwindow/
      GradleTasksExtendedToolWindowFactory.kt
      GradleTasksExtendedPanel.kt
    actions/
      GradleTaskViewModeAction.kt
      GradleTaskViewModeActionGroup.kt
      GradleTaskExecuteAction.kt
      GradleTaskCopyAction.kt
    model/
      GradleTaskDescriptor.kt
      NestedFolderNode.kt
      TaskTreeBuilder.kt
      TaskTreeSorting.kt
      GradleTaskDataProvider.kt
    persistence/
      GradleTaskViewSettings.kt
      GradleTaskViewSettingsState.kt
    ui/
      GradleTasksTree.kt
      GradleTasksTreeCellRenderer.kt
      GradleTasksTreeStateManager.kt
      GradleTaskViewRefresher.kt
      GradleTasksSpeedSearch.kt
    execution/
      GradleTaskExecutor.kt
    util/
      GradleTaskPathUtil.kt
      TreePathUtil.kt

src/main/resources/
  META-INF/
    plugin.xml
  messages/
    GradleTasksViewBundle.properties

docs/
  INSTALLATION.md
```

### `plugin.xml` – Kernangaben

```xml
<idea-plugin>
    <id>de.example.gradletasksview</id>
    <name>Gradle Tasks Extended</name>
    <vendor>Privat</vendor>

    <depends>com.intellij.modules.platform</depends>
    <depends>org.jetbrains.plugins.gradle</depends>

    <extensions defaultExtensionNs="com.intellij">
        <toolWindow id="GradleTasksExtended"
                    factoryClass="de.example.gradletasksview.toolwindow.GradleTasksExtendedToolWindowFactory"
                    anchor="right"
                    icon="AllIcons.Nodes.ConfigFolder"/>
    </extensions>
</idea-plugin>
```

Die explizite `<depends>`-Deklaration auf `org.jetbrains.plugins.gradle` stellt sicher, dass das Gradle-Plugin garantiert geladen ist, bevor unsere Klassen (`ProjectKeys.TASK`, `TaskData` etc.) referenziert werden. In IntelliJ IDEA Ultimate ist dieses Plugin standardmäßig gebündelt.

### `build.gradle.kts` – Kernangaben (IntelliJ Platform Gradle Plugin 2.x)

```kotlin
plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    id("org.jetbrains.intellij.platform") version "2.x.x"
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create("IU", "<neueste unterstützte Ultimate-Version>")
        bundledPlugin("org.jetbrains.plugins.gradle")
        instrumentationTools()
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild.set("<aktuelle Build-Nummer>")
            // kein untilBuild -> nur aktuelle Version unterstützt
        }
    }
}
```

Hinweis: `create("IU", ...)` bindet das Projekt explizit an **IntelliJ IDEA Ultimate** (`IU`), nicht an Community (`IC`) [web:39][web:38]. Die konkrete Versionsnummer wird beim Rechercheauftrag/Setup-Schritt final eingetragen, da sich "neueste Version" mit der Zeit ändert.

---

## Datenmodell

### ViewMode

```kotlin
enum class GradleTaskViewMode {
    DEFAULT,
    SORT,
    NESTED
}
```

### Normalisierte Task-Repräsentation

```kotlin
data class GradleTaskDescriptor(
    val taskName: String,
    val taskPath: String,
    val groupName: String?,
    val description: String?,
    val externalProjectPath: String,
    val gradleProjectPath: String,
    val stableId: String
)
```

`stableId` wird deterministisch gebildet:

```kotlin
fun buildStableId(
    externalProjectPath: String,
    gradleProjectPath: String,
    taskName: String
): String = "$externalProjectPath::$gradleProjectPath::$taskName"
```

Das verhindert Kollisionen bei identischen Tasknamen in unterschiedlichen Subprojekten oder Composite Builds.

### Virtueller Ordnerknoten

```kotlin
data class NestedFolderNode(
    val displayName: String,
    val fullPath: String,
    val childrenTasks: MutableList<GradleTaskDescriptor> = mutableListOf(),
    val childrenFolders: MutableMap<String, NestedFolderNode> = linkedMapOf()
)
```

### Sortierregeln

```kotlin
val folderComparator = compareBy<String>(String.CASE_INSENSITIVE_ORDER) { it }

val taskComparator = compareBy<GradleTaskDescriptor>(
    String.CASE_INSENSITIVE_ORDER
) { it.taskName }
```

---

## Datenbeschaffung (Weg B)

### Grundprinzip

Die eigene View liest bereits importierte Gradle-Projektdaten aus dem `ExternalSystem`-Datenmodell, löst **keinen eigenen Sync** aus und greift nicht auf interne View-Rendering-Klassen zu.

```kotlin
class GradleTaskDataProvider(private val project: Project) {

    fun loadTasks(): List<GradleTaskDescriptor> {
        val tasks = mutableListOf<GradleTaskDescriptor>()

        val projectsData = ProjectDataManager.getInstance()
            .getExternalProjectsData(project, GradleConstants.SYSTEM_ID)

        projectsData.forEach { info ->
            val projectStructure = info.externalProjectStructure ?: return@forEach
            collectTasks(projectStructure, tasks)
        }

        return tasks
    }

    private fun collectTasks(
        node: DataNode<*>,
        out: MutableList<GradleTaskDescriptor>
    ) {
        ExternalSystemApiUtil.findAll(node, ProjectKeys.TASK).forEach { taskNode ->
            val data = taskNode.data
            out += GradleTaskDescriptor(
                taskName = data.name,
                taskPath = data.name,
                groupName = data.group,
                description = data.description,
                externalProjectPath = data.linkedExternalProjectPath,
                gradleProjectPath = data.linkedExternalProjectPath,
                stableId = buildStableId(
                    data.linkedExternalProjectPath,
                    data.linkedExternalProjectPath,
                    data.name
                )
            )
        }

        node.children.forEach { collectTasks(it, out) }
    }
}
```

Dieser Code ist ein **Gerüst**, das im Rechercheauftrag gegen die tatsächliche `TaskData`-API-Signatur der Zielversion verifiziert werden muss (Feldnamen können variieren).

### Threading

Da `ProjectDataManager`- und `DataNode`-Zugriffe Lesezugriffe auf das Projektmodell sind, müssen sie unter einem Read-Lock laufen. Gemäß aktuellem Threading-Modell (IntelliJ Platform ≥ 2024.1) wird die Coroutine-basierte API verwendet [web:8]:

```kotlin
suspend fun loadTasksAsync(): List<GradleTaskDescriptor> =
    readAction {
        gradleTaskDataProvider.loadTasks()
    }
```

Tree-Rebuilds nach dem Laden erfolgen auf dem EDT via `withContext(Dispatchers.EDT)`. Kein Zugriff auf `DataNode`/`ProjectDataManager` darf direkt auf dem EDT ohne Read-Lock erfolgen; kein Rebuild des `JTree`-Modells darf auf einem Hintergrundthread erfolgen.

---

## Aufbau des Nested-Baums

### Algorithmus

1. Alle Tasks über `GradleTaskDataProvider` laden.
2. Für jede Task den Gruppenpfad bestimmen (`groupName` oder `Other`).
3. Gruppenpfad auf `/` splitten, leere Segmente entfernen.
4. Für jedes Segment einen virtuellen Ordner erzeugen oder wiederverwenden.
5. Die Task am letzten Ordner des Pfades ablegen.
6. Rekursiv alle Tasks und Unterordner pro Ordner sortieren.
7. Beim Rendering zuerst Tasks, dann Unterordner ausgeben.

### Pseudocode

```kotlin
fun buildNestedTree(tasks: List<GradleTaskDescriptor>): NestedFolderNode {
    val root = NestedFolderNode(displayName = "", fullPath = "")

    tasks.forEach { task ->
        val groupPath = task.groupName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "Other"

        val segments = groupPath
            .split("/")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        var current = root
        val pathParts = mutableListOf<String>()

        segments.forEach { segment ->
            pathParts += segment
            val fullPath = pathParts.joinToString("/")
            current = current.childrenFolders.getOrPut(segment) {
                NestedFolderNode(displayName = segment, fullPath = fullPath)
            }
        }

        current.childrenTasks += task
    }

    sortRecursively(root)
    return root
}

fun sortRecursively(node: NestedFolderNode) {
    node.childrenTasks.sortWith(taskComparator)
    node.childrenFolders.values.forEach { sortRecursively(it) }
}
```

---

## Toolbar-Dropdown

### Anforderungen

- Der Button befindet sich in der Toolbar der eigenen ToolWindow-Ansicht (keine Fremdintegration in eine andere Toolbar nötig, da wir eine eigene View besitzen).
- Dropdown-/Popup-Action mit den Optionen `Default`, `Sort`, `Nested`.
- Der aktive Modus ist über Checkmark bzw. Button-Beschriftung erkennbar.
- Moduswechsel aktualisiert die Ansicht sofort.

### IntelliJ-Action-Struktur

```kotlin
class GradleTaskViewModeActionGroup : ActionGroup("Task View", true) {
    override fun getChildren(e: AnActionEvent?): Array<AnAction> = arrayOf(
        SetGradleTaskViewModeAction(GradleTaskViewMode.DEFAULT),
        SetGradleTaskViewModeAction(GradleTaskViewMode.SORT),
        SetGradleTaskViewModeAction(GradleTaskViewMode.NESTED)
    )
}

class SetGradleTaskViewModeAction(
    private val mode: GradleTaskViewMode
) : ToggleAction(mode.name) {

    override fun isSelected(e: AnActionEvent): Boolean {
        val project = e.project ?: return false
        return GradleTaskViewSettings.getInstance(project).state.selectedMode == mode
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        if (!state) return
        val project = e.project ?: return
        e.project?.service<GradleTaskViewRefresher>()?.switchMode(mode)
    }
}
```

Da die Toolbar Teil unserer eigenen `ToolWindowFactory` ist, wird die `ActionGroup` direkt beim Panel-Aufbau registriert (`ActionManager.getInstance().createActionToolbar(...)`) – kein Patchen einer fremden Komponente nötig.

---

## Persistenz

### Persistente Einstellungen

```kotlin
@State(
    name = "GradleTasksViewSettings",
    storages = [Storage("gradleTasksView.xml")]
)
@Service(Service.Level.PROJECT)
class GradleTaskViewSettings : PersistentStateComponent<GradleTaskViewSettingsState> {

    private var myState = GradleTaskViewSettingsState()

    override fun getState(): GradleTaskViewSettingsState = myState

    override fun loadState(state: GradleTaskViewSettingsState) {
        myState = state
    }

    companion object {
        fun getInstance(project: Project): GradleTaskViewSettings =
            project.service()
    }
}
```

### Persistierter State

```kotlin
data class GradleTaskViewSettingsState(
    var selectedMode: GradleTaskViewMode = GradleTaskViewMode.DEFAULT,
    var expandedFolderPaths: MutableSet<String> = mutableSetOf(),
    var knownFolderPaths: MutableSet<String> = mutableSetOf()
)
```

### Eindeutige Expand-State-Keys

Format:

```text
<externalProjectPath>::<gradleProjectPath>::<folderPath>
```

Beispiel:

```text
C:\workspace\my-app::::docker/gui/jsp
```

### Persistenzregeln

- Einklappen: Pfad aus `expandedFolderPaths` entfernen (bleibt in `knownFolderPaths`).
- Aufklappen: Pfad zu `expandedFolderPaths` hinzufügen.
- Unbekannter Ordner (nicht in `knownFolderPaths`): automatisch als expandiert behandeln und zu beiden Sets hinzufügen.
- Wechsel zu `Default`: `selectedMode = DEFAULT`, `expandedFolderPaths.clear()`, `knownFolderPaths.clear()`.
- Wechsel zwischen `Sort` und `Nested`: Expand-State bleibt erhalten und wird bei Rückkehr zu `Nested` wieder angewendet.

---

## Erhalt der bestehenden Funktionen (Nachbau in eigener View)

Da Weg B keine fremden Nodes wiederverwendet, werden folgende Funktionen **explizit nachgebaut**, jeweils unter Nutzung derselben offiziellen Ausführungs-APIs wie die originale Ansicht:

### Task-Ausführung (Doppelklick)

```kotlin
class GradleTaskExecutor(private val project: Project) {

    fun execute(descriptor: GradleTaskDescriptor) {
        val settings = ExternalSystemTaskExecutionSettings().apply {
            externalProjectPath = descriptor.externalProjectPath
            taskNames = listOf(descriptor.taskName)
            externalSystemIdString = GradleConstants.SYSTEM_ID.id
        }

        ExternalSystemUtil.runTask(
            settings,
            DefaultRunExecutor.EXECUTOR_ID,
            project,
            GradleConstants.SYSTEM_ID
        )
    }
}
```

Dieses Gerüst muss im Rechercheauftrag gegen die exakte Signatur von `ExternalSystemUtil.runTask` in der Zielversion verifiziert werden.

### Kontextmenü

- Eigene `ActionGroup` mit: Task ausführen, Task-Namen kopieren, Task-Pfad kopieren, Ordner auf-/zuklappen (nur für Folder-Nodes).
- Bei gemischter Selektion aus Task- und Folder-Nodes: nur Aktionen aktivieren, die für beide Typen sinnvoll sind (z. B. Expand/Collapse All).

### Copy

- `CopyProvider`-Implementierung auf dem `JTree`, die für Task-Nodes `taskPath`, für Folder-Nodes `fullPath` in die Zwischenablage kopiert.

### Filterung

- Eigene `GradleTasksSpeedSearch`-Klasse basierend auf `TreeSpeedSearch`, die auf `taskName` matched.
- Filterung wird **vor** dem Aufbau von `Sort`/`Nested` angewendet, damit ausgefilterte Tasks nicht durch die Verschachtelung erneut sichtbar werden.

---

## Refresh-Strategie

### Trigger

- Gradle-Projekt wurde (re-)importiert (`ExternalSystemTaskNotificationListener` bzw. `ProjectDataImportListener`).
- Filtertext ändert sich.
- Aktiver View-Modus ändert sich.
- ToolWindow wird erstmals sichtbar.
- Projekt wird geöffnet.

### Ablauf

```text
Event
→ readAction: Tasks über GradleTaskDataProvider laden
→ optional filtern
→ je nach Modus Modell erzeugen (Default/Sort/Nested)
→ EDT: Tree-Model aktualisieren
→ Expand-State wiederherstellen (nur Nested)
→ Selektion über stableId wiederherstellen
```

### Performance bei großen Projekten

Bei vielen Tasks/tiefen Gruppenpfaden wird der Rebuild **debounced** (z. B. 300 ms Coalescing über `Alarm` oder `MergingUpdateQueue`), damit aufeinanderfolgende Sync-Events keine mehrfachen vollständigen Rebuilds auslösen. Ein kompletter Rebuild bei jedem einzelnen Notification-Event ist bei großen Multi-Modul-Projekten nicht akzeptabel.

---

## Moduswechsel

### Wechsel zu Sort

```text
→ Tasks laden (readAction)
→ flache Gruppenliste sortieren (Gruppenpfad, dann Taskname)
→ Tree aktualisieren (EDT)
→ Selektion wiederherstellen
```

### Wechsel zu Nested

```text
→ Tasks laden (readAction)
→ NestedFolderNode-Baum erzeugen
→ Tree aktualisieren (EDT)
→ gespeicherte Expand-States anwenden
→ neue unbekannte Ordner expandieren
→ Selektion wiederherstellen
```

### Wechsel zu Default

```text
→ Plugin-Persistenz zurücksetzen (expandedFolderPaths, knownFolderPaths)
→ Tasks unsortiert/ungruppiert gemäß Originaldatenreihenfolge anzeigen
→ Selektion wiederherstellen
```

---

## Edge Cases

- **Mehrere offene Projekte:** `GradleTaskViewSettings` ist `Service.Level.PROJECT`-gebunden; jedes Projektfenster hat eine eigene Instanz. Der `GradleTaskViewRefresher` darf niemals als Application-Level-Singleton mit projektübergreifendem State implementiert werden.
- **Große Codebasen / viele Tasks:** Debouncing beim Refresh (siehe oben); Baum-Aufbau erfolgt als reine In-Memory-Transformation ohne PSI-Zugriff, daher unabhängig von Codebasengröße – nur von Task-Anzahl.
- **Identische Tasknamen in unterschiedlichen Subprojekten / Composite Builds:** durch `stableId` (siehe Datenmodell) eindeutig unterscheidbar.
- **Tasks ohne Gruppe:** landen in der Gruppe `Other` (siehe "Zu klärende Produktdetails").
- **Sonderzeichen in Gruppenpfaden:** Segmente werden nur getrimmt, nicht weiter normalisiert.
- **Andere Plugins mit ähnlicher Funktionalität:** Da wir ein komplett eigenes ToolWindow registrieren (eigene `id` in `plugin.xml`), gibt es keine Kollision mit der originalen Gradle-Ansicht oder anderen Plugins, die diese patchen könnten.

---

## Testing

### Unit Tests (reine Modelllogik, ohne Platform-Testframework)

- Gruppenpfad `docker/gui` erzeugt `docker → gui`.
- Mehrere Tasks derselben Gruppe werden nach Taskname sortiert.
- Mehrere Unterordner werden alphabetisch sortiert.
- Tasks werden vor Unterordnern ausgegeben.
- Leere Gruppen, führende/folgende/doppelte `/` werden korrekt behandelt.
- `stableId` ist bei identischen Tasknamen in unterschiedlichen Subprojekten eindeutig.
- Wechsel auf `Default` löscht `expandedFolderPaths` und `knownFolderPaths`.

```text
Input:
docker/gui -> build-all-gui
docker/gui/jsp -> build-jsp
docker/gui/jsp -> run-jsp
docker/gui/react -> build-react

Expected:
docker
  gui
    build-all-gui
    jsp
      build-jsp
      run-jsp
    react
      build-react
```

### Integration Tests (IntelliJ Platform Test Framework)

Basis: `BasePlatformTestCase` bzw. `HeavyPlatformTestCase` mit `JavaCodeInsightTestFixture`. Für Gradle-Projektdaten wird ein Mock-`DataNode`-Baum aufgebaut (kein echter Gradle-Sync im Test), um Testlaufzeit und Flakiness gering zu halten.

Zu testen:

- ToolWindow und Toolbar-Dropdown sind vorhanden und zeigen die drei Modi.
- Umschalten auf `Sort`/`Nested` erzeugt die erwartete Baumstruktur aus einem vorbereiteten Mock-`DataNode`-Baum.
- Folder-Icons sind gesetzt.
- Ordner sind initial expandiert; Einklappen bleibt nach simuliertem Neustart (State speichern/laden) erhalten.
- `Default` stellt die unsortierte Ansicht wieder her und löscht den Plugin-State.
- Task-Ausführung ruft `ExternalSystemUtil.runTask` mit den erwarteten `ExternalSystemTaskExecutionSettings` auf (Verifikation via Mock/Spy).
- Kontextmenü-Actions sind für Task-Nodes und Folder-Nodes korrekt aktiviert/deaktiviert.
- Filterung blendet Tasks in allen drei Modi korrekt aus/ein.

### UI-Automatisierung – bewusste Entscheidung

UI-Interaktionstests (Klick auf Toolbar-Dropdown, Klick auf Baumknoten) werden **bewusst nicht automatisiert** (kein `remote-robot`-Setup), da es sich um ein privates Einzelprojekt ohne CI-Infrastruktur für UI-Tests handelt. Diese Fälle werden manuell getestet (siehe unten). Diese Entscheidung ist explizit dokumentiert, keine unbeabsichtigte Lücke.

### Manuelle Tests

Mit einem Testprojekt, das folgende Gruppen besitzt:

```text
docker/gui
docker/gui/jsp
docker/gui/react
docker/server/gui
build
verification
help
```

Zusätzlich manuell prüfen:

- Tasks ohne Gruppe.
- Identische Tasknamen in unterschiedlichen Subprojekten.
- Mehrmodul-Projekte und Composite Builds.
- Mehrere gleichzeitig geöffnete Projekte mit unterschiedlichem Modus/State.
- IDE-Neustart (Persistenz-Verifikation).
- Gradle-Reimport bei aktivem `Nested`-Modus.
- Filter aktiv, dann Moduswechsel.
- Mehrfach schnelles Umschalten der Modi (Debouncing-Verifikation).
- Doppelklick, Kontextmenü, Copy in allen drei Modi.

---

## Installationsdokumentation (privat, ohne Marketplace)

Da keine Marketplace-Veröffentlichung erfolgt, wird eine `docs/INSTALLATION.md` mit folgendem Inhalt erstellt:

1. Build-Befehl: `./gradlew buildPlugin` erzeugt eine ZIP-Datei unter `build/distributions/`.
2. In IntelliJ IDEA Ultimate: **Settings → Plugins → Zahnrad-Icon → Install Plugin from Disk...** [web:32].
3. Die erzeugte ZIP-Datei auswählen, IDE neu starten.
4. Hinweis: Die Datei-Erweiterung muss kleingeschrieben `.zip` sein, sonst bleibt der OK-Button in manchen IDE-Versionen deaktiviert [web:34].
5. Nach Update des Plugins: alte Version zunächst deinstallieren oder direkt über "Install Plugin from Disk" überschreiben, danach IDE neu starten.

---

## Akzeptanzkriterien

### Toolbar und Modi

- [ ] Die eigene ToolWindow-Ansicht besitzt einen Toolbar-Dropdown mit `Default`, `Sort`, `Nested`.
- [ ] Der aktuell aktive Modus ist sichtbar (Checkmark).
- [ ] Der Wechsel des Modus aktualisiert die Ansicht ohne IDE-Neustart.

### Default

- [ ] Tasks werden unsortiert/ungruppiert gemäß Originaldatenreihenfolge angezeigt.
- [ ] Beim Wechsel auf `Default` werden alle plugin-spezifischen Persistenzdaten gelöscht.

### Sort

- [ ] Gruppen sind nach vollständigem Gruppenpfad sortiert.
- [ ] Tasks innerhalb jeder Gruppe sind nach Taskname sortiert.

### Nested

- [ ] Slash-getrennte Gruppen werden als verschachtelte Ordner dargestellt.
- [ ] Ordner haben ein Folder-Icon.
- [ ] Tasks stehen in einem Ordner vor Unterordnern.
- [ ] Ordner sind initial geöffnet; Zustand überlebt IDE-Neustart, projektbezogen.

### Funktionserhalt

- [ ] Doppelklick führt die Task über `ExternalSystemUtil.runTask` aus.
- [ ] Kontextmenü mit Ausführen/Copy/Expand-Collapse funktioniert.
- [ ] Copy funktioniert für Task- und Folder-Nodes.
- [ ] Filterung funktioniert in allen drei Modi.
- [ ] Task-Selektion bleibt bei Rebuilds über `stableId` möglichst erhalten.
- [ ] Mehrere offene Projekte haben unabhängigen State.

---

## Implementierungsreihenfolge

1. Technischen Rechercheauftrag abschließen (Datenquelle, Task-Ausführung, ToolWindow-Registrierung, Speed-Search) und Ergebnis dokumentieren.
2. Minimalprojekt mit IntelliJ Platform Gradle Plugin 2.x, Kotlin, Ziel `IU` (aktuellste Version) aufsetzen.
3. `plugin.xml` mit `<depends>` auf `org.jetbrains.plugins.gradle` und eigener `toolWindow`-Extension anlegen.
4. `GradleTaskDataProvider` implementieren und gegen ein echtes Testprojekt mit den Beispiel-Gruppenpfaden verifizieren.
5. `GradleTaskDescriptor` und `stableId`-Erzeugung implementieren und unit-testen.
6. Sortierlogik für `Sort` implementieren und unit-testen.
7. Nested-Modell inklusive Ordnerhierarchie implementieren und unit-testen.
8. Eigenen `JTree` mit Renderer (inkl. Folder-Icon) implementieren.
9. Toolbar-Dropdown und `GradleTaskViewRefresher` implementieren.
10. Persistenz der Modusauswahl und Expand-States implementieren.
11. `GradleTaskExecutor` (Doppelklick-Ausführung) implementieren und gegen echte Task-Ausführung verifizieren.
12. Kontextmenü, Copy, Speed-Search-Filterung implementieren.
13. Debounced Refresh bei Gradle-Reimport-Events implementieren.
14. Selektionserhalt über `stableId` implementieren.
15. Verhalten beim Wechsel auf `Default` implementieren und State-Clearing testen.
16. Integrationstests mit Mock-`DataNode`-Bäumen implementieren.
17. Manuelle Tests mit Multi-Modul-Testprojekt durchführen.
18. `docs/INSTALLATION.md` finalisieren, ZIP-Build verifizieren, lokale Installation testen.

---

## Hinweise für die umsetzende KI

- Weg B (eigene View) ist die verbindliche Architektur – keine Rückkehr zu internen Gradle-View-Klassen ohne erneute Rücksprache.
- Alle Lesezugriffe auf Projektdaten (`ProjectDataManager`, `DataNode`) laufen über `readAction { }`; alle Tree-Model-Mutationen laufen auf dem EDT.
- Task-Ausführung erfolgt ausschließlich über `ExternalSystemUtil.runTask`, niemals über eigene Prozessausführung oder Kommandozeilenaufrufe.
- `stableId` ist die einzige verlässliche Identität für Selektionserhalt und Persistenz – niemals sichtbare Namen allein verwenden.
- Debouncing bei Refresh ist Pflicht, kein Nice-to-have, sobald mehr als eine Handvoll Tasks vorhanden sind.
- Es gibt keine Rückwärtskompatibilitätsanforderung zu älteren IntelliJ-Versionen – APIs dürfen die aktuellste stabile Form nutzen (Coroutinen-Threading, IntelliJ Platform Gradle Plugin 2.x).
- Marketplace-spezifische Schritte (Signierung, Produktcode, Obfuskierung) sind nicht Teil dieses Projekts.
