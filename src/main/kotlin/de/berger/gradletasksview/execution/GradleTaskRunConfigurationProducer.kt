package de.berger.gradletasksview.execution

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemTaskConfigurationType
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import de.berger.gradletasksview.model.GradleTaskDescriptor
import org.jetbrains.plugins.gradle.util.GradleConstants

/** Die Task-Auswahl des eigenen Baums, wie sie im Action-System sichtbar ist (REQ-079). */
val SELECTED_GRADLE_TASKS: DataKey<List<GradleTaskDescriptor>> =
    DataKey.create("GradleTasksExtended.SelectedTasks")

/**
 * Leitet aus der Auswahl im eigenen Baum eine Gradle-Run-Konfiguration ab (REQ-079).
 *
 * Daran haengt das gesamte Ausfuehrungsmenue: Die Kontext-Aktionen der Executors und
 * "Modify Run Configuration" arbeiten ueber einen `ConfigurationContext`, der seine
 * Konfigurationen von den registrierten Producern erfragt. Ohne einen eigenen Producer
 * muesste der Baum die Auswahl als `ExternalSystemTaskLocation` veroeffentlichen - die
 * einzige Form, die der Gradle-Producer der Plattform akzeptiert, und mit
 * `@ApiStatus.Internal` markiert (ADR-022, REQ-052).
 *
 * Gelesen wird deshalb ein plugin-eigener Datenschluessel; die `Location` des Baums ist nur
 * noch ein gewoehnliches [com.intellij.execution.PsiLocation] auf das Modulverzeichnis, denn
 * ohne irgendeine Location fragt der `ConfigurationContext` erst gar keine Producer.
 */
class GradleTaskRunConfigurationProducer : LazyRunConfigurationProducer<ExternalSystemRunConfiguration>() {

    override fun getConfigurationFactory(): ConfigurationFactory = gradleConfigurationType().factory

    override fun setupConfigurationFromContext(
        configuration: ExternalSystemRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>,
    ): Boolean {
        val settings = settingsFrom(context) ?: return false
        val project = context.project ?: return false

        configuration.settings.setFrom(settings)
        configuration.name = AbstractExternalSystemTaskConfigurationType.generateName(project, settings)
        return true
    }

    /**
     * Erkennt eine bereits bestehende Konfiguration wieder - sonst legte jeder Start eine
     * weitere an, statt die vorhandene erneut auszufuehren.
     */
    override fun isConfigurationFromContext(
        configuration: ExternalSystemRunConfiguration,
        context: ConfigurationContext,
    ): Boolean {
        val settings = settingsFrom(context) ?: return false
        return configuration.settings.externalProjectPath == settings.externalProjectPath &&
            configuration.settings.taskNames == settings.taskNames
    }

    private fun settingsFrom(context: ConfigurationContext): ExternalSystemTaskExecutionSettings? {
        val descriptors = SELECTED_GRADLE_TASKS.getData(context.dataContext).orEmpty()
        return gradleTaskExecutionSettings(descriptors)
    }

    private fun gradleConfigurationType(): AbstractExternalSystemTaskConfigurationType =
        checkNotNull(ExternalSystemUtil.findConfigurationType(GradleConstants.SYSTEM_ID)) {
            "Kein Konfigurationstyp fuer ${GradleConstants.SYSTEM_ID} - ist die Gradle-Integration aktiv?"
        }
}
