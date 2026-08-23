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
 * Ansichtseinstellungen aus dem Schluessel `settings`.
 *
 * Sichtbarkeit steht bewusst **nicht** hier: Sie ist ein `hidden` am jeweiligen Knoten und
 * folgt damit denselben Vorrangregeln wie `expanded` und `icon` (siehe OPEN-009).
 *
 * @property showModules Vorgabe fuer den Feststellbutton der Modulgliederung. `null` bedeutet
 *   "nicht konfiguriert"; der eingebaute Standard ist **aus**, damit sich fuer bestehende
 *   Projekte ohne Konfiguration nichts aendert.
 */
data class TaskViewSettings(val showModules: Boolean? = null) {
    val isEmpty: Boolean get() = showModules == null
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
