package de.example.gradletasksview.persistence

import de.example.gradletasksview.model.GradleTaskViewMode

/**
 * Konfiguration eines einzelnen Baumknotens.
 *
 * Alle Felder sind dreiwertig: `null` heisst "nicht konfiguriert", dann entscheidet die
 * naechste Stufe der Aufloesung bzw. der eingebaute Standard. Nur so laesst sich "nicht
 * gesetzt" von einem ausdruecklich gesetzten Wert unterscheiden - ein Unterschied, der bei
 * [hidden] darueber entscheidet, ob ein spezifischerer Eintrag eine allgemeinere Vorgabe
 * zuruecknehmen kann.
 *
 * @property expanded Auf-/Zuklappzustand. Bei Tasks ohne Wirkung, da Blattknoten.
 * @property icon Dateiname eines eigenen Icons relativ zu
 *   `gradle/gradle_tasks_extended/icons/`, das das eingebaute Plugin-Icon ersetzt.
 * @property hidden Blendet den Knoten samt Teilbaum aus.
 * @property modes Abweichende Werte je Ansichtsmodus; sie gehen den Feldern daneben vor.
 *   Ein Eintrag hier traegt selbst keine weiteren [modes] - eine zweite Verschachtelung
 *   haette keine Bedeutung.
 */
data class TaskViewNodeConfig(
    val expanded: Boolean? = null,
    val icon: String? = null,
    val hidden: Boolean? = null,
    val modes: Map<GradleTaskViewMode, TaskViewNodeConfig> = emptyMap()
) {
    val isEmpty: Boolean
        get() = expanded == null && icon == null && hidden == null && modes.isEmpty()

    /** Der Eintrag ohne seine [modes] - der Teil, der modusuebergreifend gilt. */
    val allModes: TaskViewNodeConfig get() = if (modes.isEmpty()) this else copy(modes = emptyMap())
}

/**
 * Inhalt eines Modulblocks unterhalb von `modules:` - eines Gradle-Projektpfads wie `":app"`
 * oder `":"` fuer das Wurzelprojekt.
 *
 * @property node Konfiguration des Modulknotens selbst, aus den Feldern, die direkt im Block
 *   stehen.
 * @property groups Gruppen-/Ordnerpfade, die nur innerhalb dieses Moduls gelten.
 * @property tasks Tasknamen, die nur innerhalb dieses Moduls gelten.
 */
data class TaskViewModuleConfig(
    val node: TaskViewNodeConfig = TaskViewNodeConfig(),
    val groups: Map<String, TaskViewNodeConfig> = emptyMap(),
    val tasks: Map<String, TaskViewNodeConfig> = emptyMap()
) {
    val isEmpty: Boolean get() = node.isEmpty && groups.isEmpty() && tasks.isEmpty()
}

/**
 * Ausschluss- und Wiedereinschlussliste fuer Module.
 *
 * @property exclude Gradle-Projektpfade, die samt Untermodulen ausgeblendet werden.
 * @property include Ausnahmen dazu: Abbildung Modulpfad -> Gruppenpfade, die trotz eines
 *   Ausschlusses sichtbar bleiben. Eine leere Liste schliesst das ganze Modul wieder ein.
 *
 * Geht mit OPEN-009, AP-2 in `hidden` auf; bis dahin unveraendert in Kraft.
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
 * Ansichtseinstellungen aus dem Schluessel `settings`.
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
 * Die oberste Ebene hat genau vier feste Schluessel, damit nichts darunter mehrdeutig ist:
 * [settings], [groups], [tasks] und [modules]. Insbesondere haben Gruppenpfade und Tasknamen
 * getrennte Namensraeume - in Gradle ist `build` sowohl eine verbreitete Gruppe als auch ein
 * verbreiteter Taskname, und ohne die Trennung wuerde ein Eintrag beide zugleich treffen
 * (siehe OPEN-009).
 *
 * [groups] und [tasks] gelten in allen Modulen; die gleichnamigen Abbildungen innerhalb eines
 * Modulblocks gelten nur dort und gehen ihnen vor.
 */
data class TaskViewConfig(
    val settings: TaskViewSettings = TaskViewSettings(),
    val groups: Map<String, TaskViewNodeConfig> = emptyMap(),
    val tasks: Map<String, TaskViewNodeConfig> = emptyMap(),
    val modules: Map<String, TaskViewModuleConfig> = emptyMap()
)
