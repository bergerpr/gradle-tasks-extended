package de.example.gradletasksview.persistence

import de.example.gradletasksview.model.GradleTaskViewMode

/**
 * Konfiguration eines einzelnen Baumknotens.
 *
 * @property expanded Auf-/Zuklappzustand eines Modul-/Gruppen-/Ordnerknotens. `null` bedeutet
 *   "nicht konfiguriert" - dann gilt die naechste Ebene der Aufloesung bzw. der Standard
 *   "aufgeklappt". Bei Task-Eintraegen immer `null`, da Tasks Blattknoten sind.
 * @property icon Dateiname eines eigenen Icons relativ zu
 *   `gradle/gradle_tasks_extended/icons/`, das das eingebaute Plugin-Icon ersetzt.
 *   `null` bedeutet "eingebautes Icon verwenden".
 */
data class TaskViewNodeConfig(val expanded: Boolean? = null, val icon: String? = null) {
    val isEmpty: Boolean get() = expanded == null && icon == null
}

/**
 * Inhalt eines Modulblocks - eines Schluessels mit fuehrendem `:` wie `":app"` oder `":"`.
 *
 * @property node Konfiguration des Modulknotens selbst, aus den reservierten Schluesseln
 *   `expanded` und `icon` innerhalb des Blocks.
 * @property groups Gruppen-/Ordnerpfade, die nur innerhalb dieses Moduls gelten. Jeder
 *   Schluessel des Blocks, der nicht `expanded` oder `icon` heisst, ist ein solcher Pfad.
 */
data class TaskViewModuleConfig(
    val node: TaskViewNodeConfig = TaskViewNodeConfig(),
    val groups: Map<String, TaskViewNodeConfig> = emptyMap()
) {
    val isEmpty: Boolean get() = node.isEmpty && groups.isEmpty()
}

/**
 * Ausschluss- und Wiedereinschlussliste fuer Module.
 *
 * @property exclude Gradle-Projektpfade, die samt Untermodulen ausgeblendet werden.
 * @property include Ausnahmen dazu: Abbildung Modulpfad -> Gruppenpfade, die trotz eines
 *   Ausschlusses sichtbar bleiben. Eine leere Liste schliesst das ganze Modul wieder ein.
 *
 * `include` ist bewusst eine Abbildung und keine flache Liste: Ein Eintrag `":legacy:docker/gui"`
 * liesse sich nicht eindeutig zerlegen, da sowohl `:legacy` mit Gruppe `docker/gui` als auch
 * `:legacy:docker` mit Gruppe `gui` gueltige Gradle-Pfade waeren (siehe ADR-014).
 */
data class TaskViewFilter(
    val exclude: List<String> = emptyList(),
    val include: Map<String, List<String>> = emptyMap()
) {
    val isEmpty: Boolean get() = exclude.isEmpty() && include.isEmpty()

    /**
     * Vereinigt zwei Listenpaare. Listen aus `custom.yaml` **ergaenzen** die aus `default.yaml`
     * und ersetzen sie nie, damit eine Team-Vorgabe nicht unbemerkt verschwinden kann; wer sie
     * loswerden will, nutzt `include` als Gegenmittel (siehe REQ-075).
     */
    operator fun plus(other: TaskViewFilter): TaskViewFilter = TaskViewFilter(
        exclude = (exclude + other.exclude).distinct(),
        include = (include.keys + other.include.keys).associateWith { module ->
            (include[module].orEmpty() + other.include[module].orEmpty()).distinct()
        }
    )
}

/**
 * Ansichtseinstellungen aus dem reservierten Schluessel `gradle_tasks_extended`.
 *
 * @property showModules Vorgabe fuer den Feststellbutton der Modulgliederung. `null` bedeutet
 *   "nicht konfiguriert"; der eingebaute Standard ist **aus**, damit sich fuer bestehende
 *   Projekte ohne Konfiguration nichts aendert.
 * @property filter Modusuebergreifende Exclude-/Include-Listen.
 * @property modeFilters Zusaetzliche Listen je Ansichtsmodus; sie ergaenzen [filter].
 */
data class TaskViewSettings(
    val showModules: Boolean? = null,
    val filter: TaskViewFilter = TaskViewFilter(),
    val modeFilters: Map<GradleTaskViewMode, TaskViewFilter> = emptyMap()
) {
    val isEmpty: Boolean get() = showModules == null && filter.isEmpty && modeFilters.all { it.value.isEmpty }
}

/**
 * Inhalt einer `default.yaml`/`custom.yaml`.
 *
 * Auf oberster Ebene stehen drei Arten von Schluesseln nebeneinander:
 * - der reservierte Schluessel `gradle_tasks_extended` mit den [settings],
 * - Schluessel mit fuehrendem `:` als Modulbloecke ([modules]),
 * - alle uebrigen Schluessel als modulunabhaengige Knoteneintraege ([entries]).
 *
 * Ein [entries]-Schluessel ist entweder ein Gruppen-/Ordnerpfad (`docker/gui/jsp`, oder eine
 * flache Gruppe wie `help` in Default/Sort) oder ein Taskname
 * (`printGroup-docker-server-gui`). Beide teilen sich denselben Namensraum; welcher Eintrag
 * greift, entscheidet der Knotentyp beim Rendern, ein Gruppenpfad kann also nie versehentlich
 * ein Task-Icon setzen. Diese Eintraege gelten in **allen** Modulen.
 *
 * Pfade, die hier nicht vorkommen, gelten als aufgeklappt und verwenden das eingebaute Icon.
 */
data class TaskViewConfig(
    val entries: Map<String, TaskViewNodeConfig> = emptyMap(),
    val modules: Map<String, TaskViewModuleConfig> = emptyMap(),
    val settings: TaskViewSettings = TaskViewSettings()
)
