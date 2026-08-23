package de.example.gradletasksview.persistence

/**
 * Adressiert einen konfigurierbaren Baumknoten in `default.yaml`/`custom.yaml`.
 *
 * Es gibt genau drei Formen, und sie entsprechen eins zu eins den Namensraeumen der Datei:
 * - [Module] - der Modulknoten selbst, ein Eintrag unter `modules:`.
 * - [Group] - ein Gruppen-/Ordnerpfad, unter `groups:` oder im `groups:` eines Modulblocks.
 * - [Task] - ein Taskname, unter `tasks:` oder im `tasks:` eines Modulblocks.
 *
 * Gruppe und Task sind bewusst zwei Typen und nicht ein Schluessel mit einem Namen: In Gradle
 * ist `build` beides, und ein gemeinsamer Namensraum liesse einen Eintrag auf die Gruppe und
 * den gleichnamigen Task zugleich wirken (siehe OPEN-009).
 *
 * [modulePath] ist `null`, wenn die Modulgliederung ausgeschaltet ist: Dann hat der
 * angeklickte Knoten keinen Modulbezug, und es greift der modulunabhaengige Eintrag auf
 * oberster Ebene der Datei.
 */
sealed interface TaskViewConfigKey {

    data class Module(val modulePath: String) : TaskViewConfigKey

    data class Group(val groupPath: String, val modulePath: String?) : TaskViewConfigKey

    data class Task(val taskName: String, val modulePath: String?) : TaskViewConfigKey

    companion object {
        fun module(modulePath: String): TaskViewConfigKey = Module(modulePath)

        fun group(groupPath: String, modulePath: String?): TaskViewConfigKey = Group(groupPath, modulePath)

        fun task(taskName: String, modulePath: String?): TaskViewConfigKey = Task(taskName, modulePath)
    }
}
