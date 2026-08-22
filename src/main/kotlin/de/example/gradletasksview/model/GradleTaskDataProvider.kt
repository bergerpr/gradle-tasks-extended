package de.example.gradletasksview.model

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.task.TaskData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import de.example.gradletasksview.util.ROOT_MODULE_PATH
import de.example.gradletasksview.util.normalizeModulePath
import de.example.gradletasksview.util.qualifiedTaskPath
import org.jetbrains.plugins.gradle.util.gradleIdentityPathOrNull
import org.jetbrains.plugins.gradle.util.gradlePathOrNull
import org.jetbrains.plugins.gradle.util.GradleConstants

/**
 * Liest bereits importierte Gradle-Task-Daten aus dem ExternalSystem-Datenmodell.
 * Loest keinen eigenen Gradle-Sync aus und greift nicht auf interne
 * View-Rendering-Klassen zu (siehe REQ-020, REQ-052 und ADR-001).
 *
 * Muss unter [com.intellij.openapi.application.readAction] aufgerufen werden, da
 * [ProjectDataManager] und [DataNode] Lesezugriffe auf das Projektmodell sind.
 *
 * Die Modulzugehoerigkeit stammt aus dem Eltern-[ModuleData]-Knoten des Task-Knotens; sie geht
 * durch das rekursive Einsammeln der Tasks sonst verloren (siehe REQ-074 und ADR-015).
 */
class GradleTaskDataProvider(private val project: Project) {

    fun loadTasks(): List<GradleTaskDescriptor> {
        val tasks = mutableListOf<GradleTaskDescriptor>()

        val projectsData = ProjectDataManager.getInstance()
            .getExternalProjectsData(project, GradleConstants.SYSTEM_ID)

        projectsData.forEach { info ->
            val projectStructure = info.externalProjectStructure ?: return@forEach
            val taskNodes = ExternalSystemApiUtil.findAllRecursively(projectStructure, ProjectKeys.TASK)

            taskNodes.forEach { taskNode ->
                tasks += toDescriptor(taskNode)
            }
        }

        return tasks
    }

    private fun toDescriptor(taskNode: DataNode<TaskData>): GradleTaskDescriptor {
        val data = taskNode.data
        val moduleData = taskNode.getParent(ModuleData::class.java)?.data

        // `gradleIdentityPath` schliesst bei Composite Builds den Praefix des eingebundenen
        // Builds ein und ist damit ueber den gesamten Composite hinweg eindeutig;
        // `gradlePath` gilt nur innerhalb eines Builds. Beide sind nullable, weil sie aus
        // frei gesetzten ModuleData-Properties stammen - faellt beides aus, bleibt nur das
        // Wurzelprojekt als vertretbare Annahme.
        val gradleProjectPath = moduleData
            ?.let { it.gradleIdentityPathOrNull ?: it.gradlePathOrNull }
            ?.let(::normalizeModulePath)
            ?: ROOT_MODULE_PATH

        // Verzeichnis des Moduls, nicht das der Projektwurzel - zusammen mit dem qualifizierten
        // Taskpfad laeuft der Task damit genau in seinem eigenen Modul.
        val externalProjectPath = moduleData?.linkedExternalProjectPath ?: data.linkedExternalProjectPath

        return GradleTaskDescriptor(
            taskName = data.name,
            taskPath = qualifiedTaskPath(gradleProjectPath, data.name),
            groupName = data.group,
            description = data.description,
            externalProjectPath = externalProjectPath,
            gradleProjectPath = gradleProjectPath,
            moduleName = moduleData?.moduleName ?: gradleProjectPath,
            stableId = buildStableId(externalProjectPath, gradleProjectPath, data.name)
        )
    }
}
