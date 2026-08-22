package de.example.gradletasksview.model

/**
 * Normalisierte, plugin-eigene Repraesentation eines Gradle-Tasks.
 * Wird ausschliesslich aus bereits importierten ExternalSystem/DataNode-Daten
 * abgeleitet (siehe [de.example.gradletasksview.model.GradleTaskDataProvider]) -
 * es findet kein eigener Gradle-Sync statt.
 */
data class GradleTaskDescriptor(
    val taskName: String,
    val taskPath: String,
    val groupName: String?,
    val description: String?,
    val externalProjectPath: String,
    val gradleProjectPath: String,
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
