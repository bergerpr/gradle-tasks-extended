package de.berger.gradletasksview.execution

import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import de.berger.gradletasksview.model.GradleTaskDescriptor
import org.jetbrains.plugins.gradle.util.GradleConstants

/**
 * Fuehrt Gradle-Tasks ausschliesslich ueber die offizielle External-System-API aus,
 * niemals ueber eigene Prozessausfuehrung.
 *
 * Ausgefuehrt wird ueber eine **temporaere Run-Konfiguration**, nicht ueber
 * `ExternalSystemUtil.runTask` (ADR-022): Nur so taucht der Lauf im Konfigurations-Widget der
 * Kopfzeile auf, und nur so findet die Plattform ueberhaupt einen Runner fuer andere Executor
 * als Run und Debug.
 *
 * Bei Gradle landet am Ende der qualifizierte Taskpfad (`:app:buildImage`), damit der Task
 * genau in seinem Modul laeuft und nicht in jedem gleichnamigen (REQ-010). Zusammengesetzt
 * wird er allerdings von der Plattform, nicht hier: [ExternalSystemTaskExecutionSettings]
 * erwartet in `taskNames` den **blanken** Namen und stellt ihm den Gradle-Projektpfad des
 * Moduls voran, das unter `externalProjectPath` liegt. Ein hier bereits qualifizierter Pfad
 * wuerde deshalb ein zweites Mal praefigiert (`:app::app:buildImage`) - siehe ADR-018.
 */
class GradleTaskExecutor(private val project: Project) {

    /**
     * @param executorId ID des gewuenschten Executors. Vorgabe ist der normale Run-Executor,
     *   damit der Doppelklick unveraendert bleibt (REQ-010); die Ausfuehrungsarten des
     *   Kontextmenues kommen dagegen von der Plattform selbst (REQ-079).
     */
    fun execute(
        descriptors: List<GradleTaskDescriptor>,
        executorId: String = DefaultRunExecutor.EXECUTOR_ID,
    ) {
        val settings = gradleTaskExecutionSettings(descriptors) ?: return
        val executor = ExecutorRegistry.getInstance().getExecutorById(executorId) ?: return
        val configuration = ExternalSystemUtil.createExternalSystemRunnerAndConfigurationSettings(
            settings,
            project,
            GradleConstants.SYSTEM_ID,
        ) ?: return

        val runManager = RunManager.getInstance(project)
        runManager.setTemporaryConfiguration(configuration)
        ExecutionUtil.runConfiguration(configuration, executor)
        runManager.selectedConfiguration = configuration
    }

    fun execute(descriptor: GradleTaskDescriptor) = execute(listOf(descriptor))
}

/**
 * Baut die Ausfuehrungseinstellungen fuer eine Task-Auswahl - `null`, wenn nichts ausgewaehlt
 * ist oder die Tasks aus verschiedenen Gradle-Projekten stammen. Mehrere Tasks desselben
 * Projekts landen als Liste in einer Konfiguration und laufen damit in einem Gradle-Aufruf,
 * genau wie in der originalen Ansicht (deren `extractLocation` dieselbe Bedingung stellt).
 *
 * Die Funktion ist die einzige Stelle, an der diese Einstellungen entstehen: Sowohl die
 * Ausfuehrung als auch der `Location`-Datenschluessel des Baums greifen darauf zu, damit die
 * Plattform aus beidem dieselbe Konfiguration ableitet und einen bereits vorhandenen Eintrag
 * wiedererkennt.
 */
internal fun gradleTaskExecutionSettings(
    descriptors: List<GradleTaskDescriptor>,
): ExternalSystemTaskExecutionSettings? {
    val projectPath = descriptors.firstOrNull()?.externalProjectPath ?: return null
    if (descriptors.any { it.externalProjectPath != projectPath }) return null

    return ExternalSystemTaskExecutionSettings().apply {
        externalProjectPath = projectPath
        taskNames = descriptors.map { it.taskName }
        taskDescriptions = descriptors.map { it.description.orEmpty() }
        externalSystemIdString = GradleConstants.SYSTEM_ID.id
    }
}
