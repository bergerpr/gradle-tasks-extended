package de.berger.gradletasksview.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import de.berger.gradletasksview.GradleTasksViewBundle
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.KeyEvent
import java.awt.event.MouseWheelEvent
import java.awt.image.BufferedImage
import javax.swing.Action
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.KeyStroke
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Zeigt einen Screenshot der Bildstrecke gross an (REQ-081). In der Bildstrecke selbst bleibt
 * ein Screenshot der Tool-Window-Ansicht klein - hier laesst er sich einpassen, auf
 * Originalgroesse stellen und stufenlos zoomen.
 *
 * Bewusst ein eigenes, nicht modales Fenster statt eines Popups: So kann man den Ausschnitt
 * offen lassen und daneben weiterlesen, und der Bildlauf des Elternteils bleibt bedienbar.
 */
class ScreenshotViewerDialog(
    project: Project?,
    private val slides: List<Pair<Screenshot, BufferedImage>>,
    startIndex: Int,
) : DialogWrapper(project, false) {

    private var index = startIndex.coerceIn(slides.indices)

    private val canvas = ZoomableImage(slides[index].second)

    // Bei einem einzigen Bild bleiben die Pfeile sichtbar, aber ausgegraut - das erklaert
    // sich selbst besser, als sie wegzulassen.
    private val previousAction =
        viewerAction("configHelp.viewer.previous", AllIcons.Actions.Play_back, { slides.size > 1 }) { step(-1) }
    private val nextAction =
        viewerAction("configHelp.viewer.next", AllIcons.Actions.Play_forward, { slides.size > 1 }) { step(1) }

    init {
        updateTitle()
        setOKButtonText(GradleTasksViewBundle.message("configHelp.close"))
        isModal = false
        init()

        // Erst nach init(): Vorher gibt es die Wurzelkomponente des Dialogs noch nicht, an der
        // die Pfeiltasten haengen. Ueber `disposable` gehen sie mit dem Fenster wieder weg.
        previousAction.registerCustomShortcutSet(shortcut(KeyEvent.VK_LEFT), rootPane, disposable)
        nextAction.registerCustomShortcutSet(shortcut(KeyEvent.VK_RIGHT), rootPane, disposable)
    }

    private fun shortcut(keyCode: Int) = CustomShortcutSet(KeyStroke.getKeyStroke(keyCode, 0))

    override fun createCenterPanel(): JComponent = JPanel(BorderLayout()).apply {
        add(toolbar(), BorderLayout.NORTH)
        add(
            JBScrollPane(canvas).apply {
                border = JBUI.Borders.empty()
                preferredSize = preferredViewSize()
            },
            BorderLayout.CENTER,
        )
    }

    /** Nur "Close" - hier ist nichts zu bestaetigen. */
    override fun createActions(): Array<Action> = arrayOf(okAction)

    /**
     * Eigener Dimensionsschluessel, damit sich das Fenster die vom Benutzer gewaehlte Groesse
     * merkt - und zwar getrennt vom Info-Dialog, aus dem es aufgeht.
     */
    override fun getDimensionServiceKey(): String = "GradleTasksExtended.ScreenshotViewer"

    private fun toolbar(): JComponent {
        val group = DefaultActionGroup(
            previousAction,
            nextAction,
            Separator.getInstance(),
            viewerAction("configHelp.viewer.zoomIn", AllIcons.General.Add) { canvas.zoomBy(1.25) },
            viewerAction("configHelp.viewer.zoomOut", AllIcons.General.Remove) { canvas.zoomBy(0.8) },
            viewerAction("configHelp.viewer.fit", AllIcons.General.FitContent) { canvas.fit() },
            viewerAction("configHelp.viewer.actualSize", AllIcons.General.ActualZoom) { canvas.zoomTo(1.0) },
        )
        return ActionManager.getInstance()
            .createActionToolbar(ActionPlaces.POPUP, group, true)
            .apply { targetComponent = canvas }
            .component
    }

    /**
     * Wechselt das Bild und **behaelt den Zoom** - genau darum geht es hier: Zwei Screenshots
     * derselben Stelle lassen sich nur vergleichen, wenn sie gleich gross bleiben. Auch die
     * Position des Bildlaufs bleibt, weil nur das Bild ausgetauscht wird.
     */
    private fun step(delta: Int) {
        if (slides.size < 2) return
        index = (index + delta + slides.size) % slides.size
        canvas.setImage(slides[index].second)
        updateTitle()
    }

    private fun updateTitle() {
        title = "${slides[index].first.caption}  (${index + 1}/${slides.size})"
    }

    private fun viewerAction(
        key: String,
        icon: Icon,
        enabled: () -> Boolean = { true },
        run: () -> Unit,
    ): AnAction = object : AnAction({ GradleTasksViewBundle.message(key) }, icon) {
        override fun actionPerformed(e: AnActionEvent) = run()

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = enabled()
        }

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
    }

    /** Hoechstens Bildgroesse, hoechstens Bildschirmgroesse mit Rand - je nachdem, was kleiner ist. */
    private fun preferredViewSize(): Dimension {
        val bounds = JBUI.size(1100, 800)
        val image = slides[index].second
        return Dimension(min(image.width + 24, bounds.width), min(image.height + 24, bounds.height))
    }
}

/**
 * Zeichnet ein Bild in einem frei waehlbaren Massstab und meldet die skalierte Groesse als
 * bevorzugte Groesse - dadurch uebernimmt der umgebende [JBScrollPane] das Blaettern, ohne dass
 * hier eigene Bildlaufleisten noetig waeren.
 */
private class ZoomableImage(private var image: BufferedImage) : JComponent() {

    private var scale = 1.0

    /** Tauscht das Bild aus, ohne den Massstab anzufassen. */
    fun setImage(newImage: BufferedImage) {
        image = newImage
        revalidate()
        repaint()
    }

    init {
        // Mausrad zoomt ohne Zusatztaste: Im Bildbetrachter ist das die haeufigste Absicht,
        // und zum Blaettern gibt es die Leisten des Bildlaufs.
        addMouseWheelListener { event: MouseWheelEvent ->
            zoomBy(if (event.wheelRotation < 0) 1.1 else 1 / 1.1)
        }
    }

    fun zoomBy(factor: Double) = zoomTo(scale * factor)

    fun zoomTo(newScale: Double) {
        scale = newScale.coerceIn(MIN_SCALE, MAX_SCALE)
        revalidate()
        repaint()
    }

    /** Passt das Bild in den sichtbaren Bereich ein, ohne es ueber Originalgroesse zu vergroessern. */
    fun fit() {
        val view = parent?.size ?: return
        if (view.width <= 0 || view.height <= 0) return
        zoomTo(min(view.width.toDouble() / image.width, view.height.toDouble() / image.height).coerceAtMost(1.0))
    }

    override fun addNotify() {
        super.addNotify()
        fit()
    }

    override fun getPreferredSize(): Dimension = Dimension(
        max(1, (image.width * scale).roundToInt()),
        max(1, (image.height * scale).roundToInt()),
    )

    override fun paintComponent(g: Graphics) {
        val width = (image.width * scale).roundToInt()
        val height = (image.height * scale).roundToInt()
        (g as Graphics2D).setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR,
        )
        g.drawImage(image, (getWidth() - width) / 2, (getHeight() - height) / 2, width, height, null)
    }

    private companion object {
        const val MIN_SCALE = 0.1
        const val MAX_SCALE = 8.0
    }
}
