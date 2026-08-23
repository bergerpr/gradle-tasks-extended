package de.berger.gradletasksview.actions

import com.intellij.execution.Executor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAware

/**
 * Die Ausfuehrungsarten im Kontextmenue - Run, Debug, Run with Coverage und was sonst an
 * Executors registriert ist (REQ-079).
 *
 * Die Eintraege sind nicht selbst gebaut, sondern die **Kontext-Aktionen der Plattform**:
 * Zu jedem registrierten [Executor] gehoert ueber `contextActionId` eine Aktion (`RunClass`,
 * `DebugClass`, `RunCoverage`, ...), die aus dem `ConfigurationContext` eine temporaere
 * Run-Konfiguration ableitet, sie ausfuehrt und im Konfigurations-Widget der Kopfzeile
 * auswaehlt. Damit sie hier greifen, veroeffentlicht der Baum den Datenschluessel
 * `Location.DATA_KEY` (siehe `GradleTasksTree.uiDataSnapshot` und ADR-022).
 *
 * Die originale Gradle-Ansicht macht es in `ExternalSystemTaskMenu` genauso - inklusive der
 * Ableitung ueber die Erweiterungsliste statt ueber `ExecutorRegistry.getRegisteredExecutors`,
 * das abgekuendigt ist (ADR-008). Neu hinzukommende Executor erscheinen dadurch von selbst.
 */
class GradleTaskRunActionGroup : DefaultActionGroup(), DumbAware {

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val project = e?.project ?: return super.getChildren(e)
        val actionManager = e.actionManager

        return Executor.EXECUTOR_EXTENSION_NAME.extensionList
            .filter { it.isApplicable(project) }
            .mapNotNull { actionManager.getAction(it.contextActionId) }
            .toTypedArray() + super.getChildren(e)
    }
}
