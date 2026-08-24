package de.berger.gradletasksview.persistence

import de.berger.gradletasksview.model.GradleTaskViewMode

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
 * @property description Erlaeuternder Text, der als Tooltip des Knotens erscheint. Nur fuer
 *   Modul-, Gruppen- und Ordnerknoten gedacht; Tasks bringen ihre Beschreibung aus Gradle mit
 *   (REQ-082). Bewusst **nicht** in [modes] auswertbar - der Text soll genau einmal
 *   dastehen und in allen Ansichtsmodi derselbe sein.
 * @property hidden Blendet den Knoten samt Teilbaum aus.
 * @property execute Verweis auf einen vorhandenen Gradle-Task (`":app:buildImage"` oder
 *   `"buildImage"`). Nur in einem Eintrag unterhalb von [tasks] eines Gruppeneintrags
 *   sinnvoll: Dort macht er aus dem Eintrag einen eigenen Baumknoten, der diesen Task
 *   ausfuehrt, ohne dass es den Namen als Gradle-Task gibt (REQ-085).
 * @property tasks Nur bei einem **Gruppeneintrag** ausgewertet: die Knoten, die zusaetzlich in
 *   dieser Gruppe erscheinen. Ein Eintrag mit [execute] legt einen an, einer ohne
 *   konfiguriert nur einen anderswo angelegten - so kann `custom.yaml` einen in
 *   `default.yaml` definierten Knoten ausblenden, ohne seine Definition zu wiederholen.
 * @property modes Abweichende Werte je Ansichtsmodus; sie gehen den Feldern daneben vor.
 *   Ein Eintrag hier traegt selbst keine weiteren [modes] - eine zweite Verschachtelung
 *   haette keine Bedeutung.
 */
data class TaskViewNodeConfig(
    val expanded: Boolean? = null,
    val icon: String? = null,
    val description: String? = null,
    val hidden: Boolean? = null,
    val execute: String? = null,
    val tasks: Map<String, TaskViewNodeConfig> = emptyMap(),
    val modes: Map<GradleTaskViewMode, TaskViewNodeConfig> = emptyMap()
) {
    val isEmpty: Boolean
        get() = expanded == null && icon == null && description == null && hidden == null &&
            execute == null && tasks.isEmpty() && modes.isEmpty()

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

/**
 * Entfernt Eintraege, die nichts mehr aussagen - leere Knoteneintraege und danach leere
 * Modulbloecke.
 *
 * Gebraucht wird das, weil `custom.yaml` nur echte Abweichungen festhaelt: Stimmt ein Knoten
 * wieder mit dem aufgeloesten Wert ueberein, wird sein Feld auf `null` gesetzt, und was dann
 * uebrig bleibt, soll nicht als leerer Rumpf in der Datei stehen.
 */
fun TaskViewConfig.withoutEmptyEntries(): TaskViewConfig = copy(
    groups = groups.prunedNodes(),
    tasks = tasks.prunedNodes(),
    modules = modules
        .mapValues { (_, module) ->
            module.copy(
                groups = module.groups.prunedNodes(),
                tasks = module.tasks.prunedNodes()
            )
        }
        .filterValues { !it.isEmpty }
)

/**
 * Erst die Knoten in [TaskViewNodeConfig.tasks] aufraeumen, dann die Eintraege selbst - ein
 * Gruppeneintrag, der nur noch leergeraeumte Unterknoten enthielte, verschwindet dadurch
 * ebenfalls.
 */
private fun Map<String, TaskViewNodeConfig>.prunedNodes(): Map<String, TaskViewNodeConfig> =
    mapValues { (_, entry) -> entry.copy(tasks = entry.tasks.filterValues { !it.isEmpty }) }
        .filterValues { !it.isEmpty }
