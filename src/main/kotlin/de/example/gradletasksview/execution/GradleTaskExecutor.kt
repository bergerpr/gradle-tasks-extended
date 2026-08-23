package de.example.gradletasksview.execution

import com.intellij.coverage.CoverageExecutor
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.externalSystem.util.task.TaskExecutionSpec
import com.intellij.openapi.project.Project
import de.example.gradletasksview.model.GradleTaskDescriptor
import org.jetbrains.plugins.gradle.util.GradleConstants

/**
 * Fuehrt Gradle-Tasks ausschliesslich ueber die offizielle External-System-API aus,
 * niemals ueber eigene Prozessausfuehrung. Nutzt die aktuelle, nicht veraltete
 * [TaskExecutionSpec]-API statt der als `@ApiStatus.Obsolete` markierten
 * `ExternalSystemUtil.runTask(settings, executorId, project, systemId)`-Ueberladung
 * (siehe ADR-006).
 *
 * Bei Gradle landet am Ende der qualifizierte Taskpfad (`:app:buildImage`), damit der Task
 * genau in seinem Modul laeuft und nicht in jedem gleichnamigen (REQ-010). Zusammengesetzt
 * wird er allerdings von der Plattform, nicht hier: [ExternalSystemTaskExecutionSettings]
 * erwartet in `taskNames` den **blanken** Namen und stellt ihm den Gradle-Projektpfad des
 * Moduls voran, das unter `externalProjectPath` liegt. Ein hier bereits qualifizierter Pfad
 * wuerde deshalb ein zweites Mal praefigiert (`:app::app:buildImage`) - siehe ADR-018.
 *
 * Run, Debug und Run with Coverage unterscheiden sich ausschliesslich in der Executor-ID
 * (REQ-079). Die Plattform haelt es in ihrer eigenen
 * `com.intellij.openapi.externalSystem.action.task.RunExternalSystemTaskAction` genauso: Sie
 * variiert nur die ueber die `ExecutorRegistry` aufgeloeste ID, waehrend Settings und
 * Aufrufweg gleich bleiben.
 */
class GradleTaskExecutor(private val project: Project) {

    /**
     * @param executorId ID des gewuenschten Executors, z. B. [DefaultRunExecutor.EXECUTOR_ID],
     *   [DefaultDebugExecutor.EXECUTOR_ID] oder [CoverageExecutor.EXECUTOR_ID]. Vorgabe ist
     *   der normale Run-Executor, damit Doppelklick (REQ-010) und alle Altaufrufe unveraendert
     *   bleiben.
     */
    fun execute(
        descriptor: GradleTaskDescriptor,
        executorId: String = DefaultRunExecutor.EXECUTOR_ID,
    ) {
        val settings = ExternalSystemTaskExecutionSettings().apply {
            externalProjectPath = descriptor.externalProjectPath
            taskNames = listOf(descriptor.taskName)
            externalSystemIdString = GradleConstants.SYSTEM_ID.id
        }

        val spec = TaskExecutionSpec.create()
            .withProject(project)
            .withSystemId(GradleConstants.SYSTEM_ID)
            .withExecutorId(executorId)
            .withSettings(settings)
            .build()

        ExternalSystemUtil.runTask(spec)
    }
}
