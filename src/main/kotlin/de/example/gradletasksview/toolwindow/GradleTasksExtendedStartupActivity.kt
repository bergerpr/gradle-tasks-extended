package de.example.gradletasksview.toolwindow

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import de.example.gradletasksview.ui.GradleTaskViewRefresher

/**
 * Erzeugt [GradleTaskViewRefresher] bereits beim Oeffnen des Projekts (siehe
 * REQ-032, Ausloeser "Projekt wird geoeffnet"),
 * damit der Reimport-Listener auch dann aktiv ist, wenn das ToolWindow noch
 * nicht sichtbar war.
 */
class GradleTasksExtendedStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.service<GradleTaskViewRefresher>()
    }
}
