package de.example.gradletasksview.model

import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.task.TaskData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.gradle.util.GradleConstants

/**
 * Liest bereits importierte Gradle-Task-Daten aus dem ExternalSystem-Datenmodell.
 * Loest keinen eigenen Gradle-Sync aus und greift nicht auf interne
 * View-Rendering-Klassen zu (Weg B, siehe Plan.md "Architekturentscheidung").
 *
 * Muss unter [com.intellij.openapi.application.readAction] aufgerufen werden, da
 * [ProjectDataManager] und [com.intellij.openapi.externalSystem.model.DataNode]
 * Lesezugriffe auf das Projektmodell sind.
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
                tasks += toDescriptor(taskNode.data)
            }
        }

        return tasks
    }

    private fun toDescriptor(data: TaskData): GradleTaskDescriptor {
        // gradleProjectPath ist aktuell mit externalProjectPath identisch - siehe
        // docs/INTEGRATION_STRATEGY.md, Abschnitt "Bekannte Risiken", fuer die
        // noch ausstehende Verfeinerung anhand des Eltern-ModuleData-Knotens.
        val externalProjectPath = data.linkedExternalProjectPath
        val gradleProjectPath = data.linkedExternalProjectPath

        return GradleTaskDescriptor(
            taskName = data.name,
            taskPath = data.name,
            groupName = data.group,
            description = data.description,
            externalProjectPath = externalProjectPath,
            gradleProjectPath = gradleProjectPath,
            stableId = buildStableId(externalProjectPath, gradleProjectPath, data.name)
        )
    }
}
