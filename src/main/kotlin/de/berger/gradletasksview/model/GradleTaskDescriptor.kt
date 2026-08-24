package de.berger.gradletasksview.model

/**
 * Normalisierte, plugin-eigene Repraesentation eines Gradle-Tasks.
 * Wird ausschliesslich aus bereits importierten ExternalSystem/DataNode-Daten
 * abgeleitet (siehe [de.berger.gradletasksview.model.GradleTaskDataProvider]) -
 * es findet kein eigener Gradle-Sync statt.
 *
 * @property taskName Blanker Taskname, wie er in der Ansicht steht (`buildImage`).
 * @property taskPath Qualifizierter Gradle-Taskpfad (`:app:buildImage`). Anzeige-, Such- und
 *   Kopierwert; fuer die Ausfuehrung wird er nicht uebergeben, die Plattform setzt ihn aus
 *   [taskName] und [externalProjectPath] selbst zusammen (siehe ADR-018).
 * @property gradleProjectPath Gradle-Projektpfad des besitzenden Moduls (`:`, `:app`,
 *   `:app:core`) - nicht das Dateisystemverzeichnis.
 * @property moduleName Anzeigename des Moduls aus dem IDE-Projektmodell; dient nur der
 *   Darstellung, nie als Identitaet.
 * @property externalProjectPath Verzeichnis, in dem Gradle fuer diesen Task aufgerufen wird.
 * @property displayName Beschriftung im Baum. Bei einem echten Task ist das der [taskName];
 *   ein per `execute` konfigurierter Knoten (REQ-085) traegt hier seinen frei gewaehlten
 *   Namen. Getrennt gefuehrt, weil [taskName] der blanke Gradle-Name bleiben muss - aus ihm
 *   entsteht die Ausfuehrung (ADR-018).
 * @property virtualGroupPath Gruppenpfad des Konfigurationseintrags, aus dem dieser Knoten
 *   entstanden ist, oder `null` fuer einen echten Task. Er macht den Knoten adressierbar:
 *   Icon, Tooltip und `hidden` haengen an Gruppenpfad + [displayName], nicht am Tasknamen -
 *   deshalb nimmt ein `hidden` auf den referenzierten Task diesen Knoten nicht mit (ADR-023).
 */
data class GradleTaskDescriptor(
    val taskName: String,
    val taskPath: String,
    val groupName: String?,
    val description: String?,
    val externalProjectPath: String,
    val gradleProjectPath: String,
    val moduleName: String,
    val stableId: String,
    val displayName: String = taskName,
    val virtualGroupPath: String? = null
) {
    /** `true` fuer einen ueber `execute` konfigurierten Knoten (REQ-085). */
    val isVirtual: Boolean get() = virtualGroupPath != null
}

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

/**
 * Identitaet eines ueber `execute` konfigurierten Knotens (REQ-085): die des referenzierten
 * Tasks, erweitert um den Ort des Eintrags.
 *
 * Noetig, weil derselbe Task zusaetzlich an seinem eigenen Platz steht und ein zweiter Knoten
 * mit gleicher [GradleTaskDescriptor.stableId] die Selektionswiederherstellung nach einem
 * Rebuild auf den falschen von beiden setzen wuerde (REQ-024).
 */
fun buildVirtualStableId(
    referencedStableId: String,
    groupPath: String,
    displayName: String
): String = "$referencedStableId@@$groupPath::$displayName"
