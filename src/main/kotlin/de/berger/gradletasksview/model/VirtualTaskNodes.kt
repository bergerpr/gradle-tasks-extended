package de.berger.gradletasksview.model

import de.berger.gradletasksview.util.normalizeGroupPath
import de.berger.gradletasksview.util.normalizeModulePath

/**
 * Ein in `default.yaml`/`custom.yaml` konfigurierter Baumknoten, der **keinen** neuen
 * Gradle-Task anlegt, sondern einen vorhandenen unter eigenem Namen an einer weiteren Stelle
 * zeigt (REQ-085).
 *
 * ```yaml
 * groups:
 *   ci/nightly:                  # entsteht dadurch, dass dieser Knoten darin liegt
 *     tasks:
 *       Alle Images bauen:
 *         execute: ":app:buildImage"
 * ```
 *
 * @property groupPath Gruppen-/Ordnerpfad, unter dem der Knoten erscheint. Die Gruppe muss es
 *   nicht schon geben - Gruppen entstehen in dieser Ansicht ausschliesslich daraus, dass
 *   Knoten sie als Pfad tragen.
 * @property displayName Beschriftung des Knotens und zugleich sein Schluessel innerhalb von
 *   [groupPath]. Ein gleichnamiger Gradle-Task muss nicht existieren.
 * @property execute Verweis auf den auszufuehrenden Task: qualifiziert (`":app:buildImage"`)
 *   oder blank (`"buildImage"`). Ohne diese Angabe entsteht kein Knoten - der Eintrag
 *   konfiguriert dann nur einen anderswo definierten.
 * @property scopeModulePath Modulblock, in dem der Eintrag steht, oder `null` fuer einen
 *   modulunabhaengigen Eintrag.
 */
data class VirtualTaskDefinition(
    val groupPath: String,
    val displayName: String,
    val execute: String,
    val scopeModulePath: String? = null
)

/**
 * Ergebnis von [expandVirtualTasks].
 *
 * @property tasks Die erzeugten Knoten - je Definition einer pro Modul, in dem der
 *   referenzierte Task existiert.
 * @property unresolved Definitionen, deren [VirtualTaskDefinition.execute] auf keinen
 *   vorhandenen Task zeigt. Sie erzeugen bewusst keinen Knoten: Ein Knoten, der beim
 *   Doppelklick ins Leere liefe, waere schlimmer als ein fehlender. Der Aufrufer protokolliert
 *   sie, wie eine fehlende Icon-Datei auch (REQ-085).
 */
data class VirtualTaskExpansion(
    val tasks: List<GradleTaskDescriptor>,
    val unresolved: List<VirtualTaskDefinition>
)

/**
 * Erzeugt aus [definitions] die zusaetzlichen Baumknoten. Reine Modelllogik ohne Plattform-
 * bezug: [tasks] sind die bereits geladenen echten Tasks, gegen die die Verweise aufgeloest
 * werden.
 *
 * Aufgerufen wird das **vor** dem Ausblenden (siehe [filterHiddenTasks]), damit ein am
 * Originalplatz ausgeblendeter Task hier trotzdem erscheinen kann - genau der Zweck der ganzen
 * Einrichtung (ADR-023).
 */
fun expandVirtualTasks(
    tasks: List<GradleTaskDescriptor>,
    definitions: List<VirtualTaskDefinition>
): VirtualTaskExpansion {
    if (definitions.isEmpty()) return VirtualTaskExpansion(emptyList(), emptyList())

    val created = mutableListOf<GradleTaskDescriptor>()
    val unresolved = mutableListOf<VirtualTaskDefinition>()
    val taken = mutableSetOf<Triple<String, String, String>>()

    // Modulbezogene Definitionen zuerst: Beschreiben eine modulbezogene und eine
    // modulunabhaengige Definition denselben Knoten, gewinnt die genauere - sonst stuende die
    // Zeile in diesem Modul doppelt da. Dieselbe Vorrangregel wie ueberall sonst im Format.
    definitions.sortedBy { it.scopeModulePath == null }.forEach { definition ->
        val matches = referencedTasks(tasks, definition)
        if (matches.isEmpty()) {
            unresolved += definition
            return@forEach
        }
        matches.forEach { referenced ->
            val node = virtualNodeOf(referenced, definition)
            if (taken.add(Triple(node.virtualGroupPath.orEmpty(), node.displayName, node.gradleProjectPath))) {
                created += node
            }
        }
    }

    return VirtualTaskExpansion(created, unresolved)
}

/**
 * Die Tasks, die [VirtualTaskDefinition.execute] meint.
 *
 * Ein qualifizierter Verweis (`":app:buildImage"`) trifft genau ein Modul, ein blanker Name
 * jedes Modul, das einen so heissenden Task hat - dieselbe Regel wie beim `tasks:`-Namensraum
 * der Konfiguration, der ebenfalls in allen Modulen gilt. Steht der Eintrag in einem
 * Modulblock, schraenkt der zusaetzlich ein.
 */
private fun referencedTasks(
    tasks: List<GradleTaskDescriptor>,
    definition: VirtualTaskDefinition
): List<GradleTaskDescriptor> {
    val reference = parseTaskReference(definition.execute) ?: return emptyList()
    val (referencedModule, referencedName) = reference

    return tasks.filter { task ->
        task.taskName == referencedName &&
            (referencedModule == null || task.gradleProjectPath == referencedModule) &&
            (definition.scopeModulePath == null || task.gradleProjectPath == definition.scopeModulePath)
    }
}

private fun virtualNodeOf(
    referenced: GradleTaskDescriptor,
    definition: VirtualTaskDefinition
): GradleTaskDescriptor {
    val groupPath = normalizeGroupPath(definition.groupPath)
    return referenced.copy(
        // groupName ist der einzige Ort, an dem die Ansicht die Einordnung ablesen kann - der
        // Knoten haengt dadurch im konfigurierten Pfad statt in der Gradle-Gruppe.
        groupName = groupPath,
        displayName = definition.displayName,
        virtualGroupPath = groupPath,
        stableId = buildVirtualStableId(referenced.stableId, groupPath, definition.displayName)
    )
}

/**
 * Die qualifizierten Pfade aller geladenen Tasks, die den blanken Namen aus [execute] tragen -
 * unabhaengig davon, in welchem Modul sie liegen.
 *
 * Dient allein der Fehlersuche: Loest ein Verweis nicht auf, sagt diese Liste, ob der Task gar
 * nicht existiert oder nur in einem anderen Modul liegt als angegeben. Ohne sie steht im Log
 * nur "nicht gefunden", und die naechste Frage - "und wo ist er dann?" - bleibt offen.
 */
fun referenceCandidates(tasks: List<GradleTaskDescriptor>, execute: String): List<String> {
    val name = parseTaskReference(execute)?.second ?: return emptyList()
    return tasks.filter { it.taskName == name }.map { it.taskPath }.distinct().sorted()
}

/**
 * Zerlegt einen Verweis in Modulpfad und Taskname: `":app:buildImage"` -> `":app"` +
 * `"buildImage"`, `":buildImage"` -> `":"` + `"buildImage"`, `"buildImage"` -> `null` +
 * `"buildImage"` (jedes Modul).
 *
 * `null` fuer einen leeren Verweis oder einen, der nur aus Doppelpunkten besteht.
 */
internal fun parseTaskReference(execute: String): Pair<String?, String>? {
    val trimmed = execute.trim()
    if (trimmed.isEmpty()) return null

    val separator = trimmed.lastIndexOf(':')
    if (separator < 0) return null to trimmed

    val taskName = trimmed.substring(separator + 1).trim().ifEmpty { return null }
    return normalizeModulePath(trimmed.substring(0, separator)) to taskName
}
