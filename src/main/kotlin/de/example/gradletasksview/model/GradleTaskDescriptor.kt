package de.example.gradletasksview.model

/**
 * Normalisierte, plugin-eigene Repraesentation eines Gradle-Tasks.
 * Wird ausschliesslich aus bereits importierten ExternalSystem/DataNode-Daten
 * abgeleitet (siehe [de.example.gradletasksview.model.GradleTaskDataProvider]) -
 * es findet kein eigener Gradle-Sync statt.
 *
 * @property taskName Blanker Taskname, wie er in der Ansicht steht (`buildImage`).
 * @property taskPath Qualifizierter Gradle-Taskpfad (`:app:buildImage`). Damit laesst sich der
 *   Task eindeutig genau in seinem Modul ausfuehren (siehe REQ-074).
 * @property gradleProjectPath Gradle-Projektpfad des besitzenden Moduls (`:`, `:app`,
 *   `:app:core`) - nicht das Dateisystemverzeichnis.
 * @property moduleName Anzeigename des Moduls aus dem IDE-Projektmodell; dient nur der
 *   Darstellung, nie als Identitaet.
 * @property externalProjectPath Verzeichnis, in dem Gradle fuer diesen Task aufgerufen wird.
 */
data class GradleTaskDescriptor(
    val taskName: String,
    val taskPath: String,
    val groupName: String?,
    val description: String?,
    val externalProjectPath: String,
    val gradleProjectPath: String,
    val moduleName: String,
    val stableId: String
)

const val UNNAMED_GROUP_DISPLAY_NAME = "Other"

/**
 * [stableId] wird immer aus externalProjectPath + gradleProjectPath + taskName gebildet,
 * niemals aus dem sichtbaren Namen allein - das verhindert Kollisionen bei identischen
 * Tasknamen in unterschiedlichen Subprojekten oder Composite Builds.
 */
fun buildStableId(
    externalProjectPath: String,
    gradleProjectPath: String,
    taskName: String
): String = "$externalProjectPath::$gradleProjectPath::$taskName"
