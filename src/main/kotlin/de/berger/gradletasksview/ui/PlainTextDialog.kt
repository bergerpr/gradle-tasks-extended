package de.berger.gradletasksview.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import de.berger.gradletasksview.GradleTasksViewBundle
import java.awt.Font
import javax.swing.Action
import javax.swing.JComponent

/**
 * Zeigt einen mitgelieferten Text unveraendert an - derzeit die Lizenz der Icons (REQ-081).
 * Bewusst ohne Umbruch und in fester Schrittweite: Lizenztexte sind auf feste Zeilenbreiten
 * gesetzt, ein automatischer Umbruch macht sie unruhig.
 */
class PlainTextDialog(project: Project?, dialogTitle: String, private val text: String) :
    DialogWrapper(project, false) {

    init {
        title = dialogTitle
        setOKButtonText(GradleTasksViewBundle.message("configHelp.close"))
        isModal = false
        init()
    }

    override fun createCenterPanel(): JComponent = JBScrollPane(
        JBTextArea(text).apply {
            isEditable = false
            lineWrap = false
            font = Font(Font.MONOSPACED, Font.PLAIN, font.size)
            caretPosition = 0
        },
    ).apply {
        preferredSize = JBUI.size(640, 480)
        border = JBUI.Borders.empty()
    }

    override fun createActions(): Array<Action> = arrayOf(okAction)
}
