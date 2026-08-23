package de.berger.gradletasksview.toolwindow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Eigenstaendige ToolWindow-Ansicht (siehe ADR-001) statt Erweiterung der
 * internen Gradle-View. Registriert in `plugin.xml` unter der `id`
 * `GradleTasksExtended`.
 */
class GradleTasksExtendedToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = GradleTasksExtendedPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, null, false)
        toolWindow.contentManager.addContent(content)
    }
}
