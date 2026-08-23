package de.berger.gradletasksview.model

import de.berger.gradletasksview.util.isModuleWithin
import de.berger.gradletasksview.util.moduleSegments
import de.berger.gradletasksview.util.normalizeGroupSegments

/**
 * Worauf eine [VisibilityRule] zielt. Die drei Formen entsprechen den Namensraeumen der
 * Konfigurationsdatei (`modules`, `groups`, `tasks`).
 */
sealed interface VisibilityTarget {

    /** Das Modul [path] **und alle Untermodule** - `":legacy"` erfasst auch `":legacy:child"`. */
    data class Module(val path: String) : VisibilityTarget

    /** Der Gruppen-/Ordnerpfad [path] **und alle Unterpfade** - `docker/gui` erfasst auch `docker/gui/jsp`. */
    data class Group(val path: String) : VisibilityTarget

    /** Genau der Task mit diesem Namen. */
    data class Task(val name: String) : VisibilityTarget
}

/**
 * Eine aus der Konfiguration flachgeklopfte Sichtbarkeitsregel. Reine Modelldaten ohne Bezug
 * zum Dateiformat - das Uebersetzen der YAML-Struktur ist Aufgabe des persistence-Pakets.
 *
 * @property target Worauf die Regel zielt.
 * @property scopeModulePath Modulblock, in dem der Eintrag steht, oder `null` fuer einen
 *   modulunabhaengigen Eintrag. Anders als [VisibilityTarget.Module] wird der Geltungsbereich
 *   **exakt** verglichen: Ein Eintrag im Block `":app"` gilt nicht in `":app:core"`, denn er
 *   beschreibt diesen einen Modulknoten und was darunter haengt.
 * @property hidden `true` blendet aus, `false` nimmt eine allgemeinere Ausblendung zurueck.
 * @property modeSpecific `true`, wenn die Regel aus dem `modes`-Block des aktiven Modus stammt.
 * @property fromCustomFile `true` fuer `custom.yaml`, `false` fuer `default.yaml`.
 */
data class VisibilityRule(
    val target: VisibilityTarget,
    val scopeModulePath: String? = null,
    val hidden: Boolean,
    val modeSpecific: Boolean = false,
    val fromCustomFile: Boolean = false
)

/**
 * Vorrang zwischen konkurrierenden Regeln, absteigend gewichtet:
 *
 * 1. **Wie genau die Regel den Knoten benennt** - erst der Task selbst, dann die Ordner von
 *    innen nach aussen, zuletzt das Modul. Ein Eintrag fuer `docker/gui/jsp` schlaegt einen
 *    fuer `docker/gui`, und der schlaegt den Modulausschluss.
 * 2. **Modulbezug** - ein Eintrag aus einem Modulblock schlaegt den modulunabhaengigen.
 * 3. **Modusbezug** - ein Eintrag aus dem `modes`-Block schlaegt den modusuebergreifenden;
 *    genau dafuer gibt es ihn.
 * 4. **`custom.yaml` vor `default.yaml`** - die persoenliche Datei kann eine Team-Vorgabe
 *    zuruecknehmen.
 *
 * Dieselbe Reihenfolge wie bei der Aufloesung von `expanded`/`icon` (Spezifitaet vor Modus vor
 * Datei); siehe OPEN-009.
 */
private val rulePrecedence: Comparator<VisibilityRule> =
    compareBy<VisibilityRule> { it.target.kindRank }
        .thenBy { it.target.pathLength }
        .thenBy { it.scopeModulePath != null }
        .thenBy { it.modeSpecific }
        .thenBy { it.fromCustomFile }

private val VisibilityTarget.kindRank: Int
    get() = when (this) {
        is VisibilityTarget.Task -> 2
        is VisibilityTarget.Group -> 1
        is VisibilityTarget.Module -> 0
    }

private val VisibilityTarget.pathLength: Int
    get() = when (this) {
        // Ein Taskname ist unteilbar; die Laenge unterscheidet hier nichts.
        is VisibilityTarget.Task -> 0
        is VisibilityTarget.Group -> normalizeGroupSegments(path).size
        is VisibilityTarget.Module -> moduleSegments(path).size
    }

/**
 * Entfernt alle Tasks, die durch [rules] ausgeblendet sind.
 *
 * Die Filterung laeuft **vor** dem Baumaufbau, damit durch das Ausblenden keine leeren
 * Gruppen- und Ordnerknoten zurueckbleiben. Ein ausgeblendeter Ordner oder ein ausgeblendetes
 * Modul nimmt seinen Teilbaum also dadurch mit, dass dessen Tasks wegfallen.
 *
 * Sie gilt unabhaengig davon, ob die Modulgliederung eingeschaltet ist - der Feststellbutton
 * aendert nur die Darstellung, nicht den Inhalt.
 */
fun filterHiddenTasks(
    tasks: List<GradleTaskDescriptor>,
    rules: List<VisibilityRule>
): List<GradleTaskDescriptor> {
    if (rules.isEmpty()) return tasks
    return tasks.filter { !isHidden(it, rules) }
}

/**
 * Ist der Knoten ausgeblendet, den [modulePath], [groupName] und [taskName] beschreiben?
 *
 * Wird ein Argument weggelassen, beschreibt der Aufruf einen weiter oben liegenden Knoten:
 * ohne [taskName] einen Gruppen-/Ordnerknoten, ohne [groupName] einen Modulknoten. Regeln, die
 * einen genaueren Knoten benennen als den gefragten, greifen dann nicht - eine ausgeblendete
 * Gruppe blendet nicht das Modul aus, in dem sie liegt.
 */
fun isNodeHidden(
    rules: List<VisibilityRule>,
    modulePath: String?,
    groupName: String? = null,
    taskName: String? = null
): Boolean {
    val matching = rules.filter { it.matches(modulePath, groupName, taskName) }
    return matching.maxWithOrNull(rulePrecedence)?.hidden ?: false
}

private fun isHidden(task: GradleTaskDescriptor, rules: List<VisibilityRule>): Boolean =
    isNodeHidden(rules, task.gradleProjectPath, task.groupName, task.taskName)

private fun VisibilityRule.matches(modulePath: String?, groupName: String?, taskName: String?): Boolean {
    // Der Geltungsbereich wird exakt verglichen: Ein Eintrag im Block ":app" beschreibt die
    // Ordner und Tasks unter genau diesem Modulknoten.
    if (scopeModulePath != null && scopeModulePath != modulePath) return false

    return when (target) {
        is VisibilityTarget.Module -> modulePath != null && isModuleWithin(modulePath, target.path)
        is VisibilityTarget.Group -> groupName != null && isGroupWithin(groupName, target.path)
        is VisibilityTarget.Task -> taskName != null && taskName == target.name
    }
}

/**
 * Ein Eintrag erfasst seine Unterpfade mit: `docker/gui` gilt auch fuer `docker/gui/jsp`. Das
 * entspricht dem, was man im Ordnerbaum sieht, und spiegelt die Regel fuer Modulpfade wider.
 */
private fun isGroupWithin(groupName: String, ancestor: String): Boolean {
    val segments = normalizeGroupSegments(groupName)
    val ancestorSegments = normalizeGroupSegments(ancestor)
    return segments.size >= ancestorSegments.size &&
        segments.subList(0, ancestorSegments.size) == ancestorSegments
}
