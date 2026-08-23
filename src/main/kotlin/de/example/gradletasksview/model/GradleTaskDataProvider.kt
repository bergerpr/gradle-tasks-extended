package de.example.gradletasksview.model

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
 * [ProjectDataManager] und die `DataNode`-Baeume Lesezugriffe auf das Projektmodell sind.
 *
 * Eingesammelt wird modulweise: erst die [ModuleData]-Knoten, dann je Modul dessen
 * Task-Kinder. Die Modulzugehoerigkeit ist damit bekannt, statt sie nachtraeglich ueber den
 * Elternknoten nachschlagen zu muessen - siehe ADR-019 (loest ADR-015 in diesem Punkt ab).
 */
class GradleTaskDataProvider(private val project: Project) {

    fun loadTasks(): List<GradleTaskDescriptor> {
        val tasks = mutableListOf<GradleTaskDescriptor>()

        val projectsData = ProjectDataManager.getInstance()
            .getExternalProjectsData(project, GradleConstants.SYSTEM_ID)

        projectsData.forEach { info ->
            val projectStructure = info.externalProjectStructure ?: return@forEach
            val moduleNodes = ExternalSystemApiUtil.findAllRecursively(projectStructure, ProjectKeys.MODULE)

            moduleNodes.forEach { moduleNode ->
                val moduleData = moduleNode.data
                // Nur die direkten Task-Kinder dieses Modulknotens: Die Gradle-Integration
                // haengt Tasks genau dort ein. Ein rekursives Einsammeln wuerde bei
                // verschachtelten Modulknoten fremde Tasks mit einsammeln und ihnen das
                // falsche Modul zuordnen.
                ExternalSystemApiUtil.findAll(moduleNode, ProjectKeys.TASK).forEach { taskNode ->
                    tasks += toDescriptor(taskNode.data, moduleData)
                }
            }
        }

        return tasks
    }

    private fun toDescriptor(data: TaskData, moduleData: ModuleData): GradleTaskDescriptor {
        // `gradleIdentityPath` schliesst bei Composite Builds den Praefix des eingebundenen
        // Builds ein und ist damit ueber den gesamten Composite hinweg eindeutig;
        // `gradlePath` gilt nur innerhalb eines Builds. Beide sind nullable, weil sie aus
        // frei gesetzten ModuleData-Properties stammen - faellt beides aus, bleibt nur das
        // Wurzelprojekt als vertretbare Annahme.
        val gradleProjectPath = (moduleData.gradleIdentityPathOrNull ?: moduleData.gradlePathOrNull)
            ?.let(::normalizeModulePath)
            ?: ROOT_MODULE_PATH

        // Verzeichnis des Moduls, nicht das der Projektwurzel: Die Plattform leitet daraus das
        // Praefix ab, mit dem sie den blanken Tasknamen zum qualifizierten Pfad ergaenzt, und
        // fuehrt den Task damit genau in seinem eigenen Modul aus (siehe ADR-018).
        val externalProjectPath = moduleData.linkedExternalProjectPath

        return GradleTaskDescriptor(
            taskName = data.name,
            taskPath = qualifiedTaskPath(gradleProjectPath, data.name),
            groupName = data.group,
            description = data.description,
            externalProjectPath = externalProjectPath,
            gradleProjectPath = gradleProjectPath,
            moduleName = moduleData.moduleName,
            stableId = buildStableId(externalProjectPath, gradleProjectPath, data.name)
        )
    }
}
