package de.example.gradletasksview.model

import de.example.gradletasksview.util.isModuleWithin
import de.example.gradletasksview.util.moduleSegments
import de.example.gradletasksview.util.normalizeGroupSegments

/**
 * Eine einzelne Ausschluss- bzw. Wiedereinschlussregel, flachgeklopft aus den
 * `exclude`/`include`-Listen der Konfiguration. Reine Modelldaten ohne Bezug zum
 * Dateiformat - das Uebersetzen der YAML-Struktur in diese Regeln ist Aufgabe des
 * persistence-Pakets (siehe REQ-075).
 *
 * @property modulePath Gradle-Projektpfad, auf den die Regel zielt; sie gilt fuer dieses Modul
 *   und alle Untermodule.
 * @property groupPaths Nur bei [include]: Gruppenpfade, die trotz eines Ausschlusses sichtbar
 *   bleiben. Eine leere Liste schliesst das ganze Modul wieder ein.
 * @property include `true` = wieder einblenden, `false` = ausblenden.
 * @property modeSpecific `true`, wenn die Regel aus dem `modes`-Block des aktiven
 *   Ansichtsmodus stammt, `false` fuer die modusuebergreifenden Listen.
 */
data class ModuleFilterRule(
    val modulePath: String,
    val groupPaths: List<String> = emptyList(),
    val include: Boolean = false,
    val modeSpecific: Boolean = false
)

/**
 * Vorrang zwischen konkurrierenden Regeln, absteigend gewichtet:
 *
 * 1. **Spezifitaet** - der laengere Modulpfad gewinnt. `:app:core` schlaegt `:app`.
 * 2. **Modusbezug** - bei gleicher Spezifitaet gewinnt die Regel aus dem `modes`-Block, denn
 *    genau dafuer gibt es sie: um eine modusuebergreifende Vorgabe fuer einen Modus zu
 *    korrigieren.
 * 3. **`include` vor `exclude`** - zu viel anzuzeigen ist harmloser, als jemandem
 *    stillschweigend Tasks vorzuenthalten; ein fehlender Task faellt oft erst auf, wenn man
 *    ihn sucht.
 *
 * Die Reihenfolge von 2 und 3 ist eine bewusste Festlegung, siehe ADR-016.
 */
private val rulePrecedence: Comparator<ModuleFilterRule> =
    compareBy<ModuleFilterRule> { moduleSegments(it.modulePath).size }
        .thenBy { it.modeSpecific }
        .thenBy { it.include }

/**
 * Entfernt alle Tasks, die durch [rules] ausgeblendet sind.
 *
 * Die Filterung laeuft **vor** dem Baumaufbau, damit durch den Ausschluss keine leeren Gruppen-
 * und Ordnerknoten zurueckbleiben, und sie gilt unabhaengig davon, ob die Modulgliederung
 * eingeschaltet ist - der Feststellbutton aendert nur die Darstellung, nicht den Inhalt.
 */
fun filterTasksByModule(
    tasks: List<GradleTaskDescriptor>,
    rules: List<ModuleFilterRule>
): List<GradleTaskDescriptor> {
    if (rules.isEmpty()) return tasks

    // Die Entscheidung haengt nur am Modulpfad, nicht am einzelnen Task: einmal pro Modul
    // ausrechnen statt einmal pro Task.
    val decisions = HashMap<String, ModuleVisibility>()
    return tasks.filter { task ->
        decisions.getOrPut(task.gradleProjectPath) { moduleVisibility(task.gradleProjectPath, rules) }
            .allows(task.groupName)
    }
}

/**
 * Sichtbarkeit eines Moduls: entweder ganz/gar nicht, oder auf bestimmte Gruppenpfade
 * eingeschraenkt.
 */
private class ModuleVisibility(private val visible: Boolean, private val onlyGroupPaths: List<String>) {

    fun allows(groupName: String?): Boolean {
        if (!visible) return false
        if (onlyGroupPaths.isEmpty()) return true
        val segments = normalizeGroupSegments(groupName)
        return onlyGroupPaths.any { isGroupWithin(segments, it) }
    }

    /**
     * Ein eingeblendeter Gruppenpfad schliesst seine Unterpfade mit ein: `docker/gui` holt
     * auch `docker/gui/jsp` zurueck. Das entspricht dem, was man im Ordnerbaum sieht, und
     * spiegelt die Regel fuer Modulpfade wider, wo `:app` ebenfalls `:app:core` umfasst.
     */
    private fun isGroupWithin(segments: List<String>, allowed: String): Boolean {
        val allowedSegments = normalizeGroupSegments(allowed)
        return segments.size >= allowedSegments.size &&
            segments.subList(0, allowedSegments.size) == allowedSegments
    }
}

private fun moduleVisibility(modulePath: String, rules: List<ModuleFilterRule>): ModuleVisibility {
    val matching = rules.filter { isModuleWithin(modulePath, it.modulePath) }

    // Ohne passenden Ausschluss ist alles eingeschlossen; ein `include` allein schraenkt nie
    // ein, es ist immer nur die Ausnahme zu einem `exclude`.
    if (matching.none { !it.include }) return ModuleVisibility(visible = true, onlyGroupPaths = emptyList())

    val winner = matching.maxWith(rulePrecedence)
    if (!winner.include) return ModuleVisibility(visible = false, onlyGroupPaths = emptyList())

    // Bei gleicher Spezifitaet ergaenzen sich die Listen (modusuebergreifend + modusbezogen,
    // default.yaml + custom.yaml), statt dass eine die andere ersetzt.
    val winningSpecificity = moduleSegments(winner.modulePath).size
    val groupPaths = matching
        .filter { it.include && moduleSegments(it.modulePath).size == winningSpecificity }
        .flatMap { it.groupPaths }
        .distinct()

    return ModuleVisibility(visible = true, onlyGroupPaths = groupPaths)
}
