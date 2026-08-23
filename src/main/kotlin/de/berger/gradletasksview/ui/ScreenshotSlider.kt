package de.berger.gradletasksview.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import de.berger.gradletasksview.GradleTasksViewBundle
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

/** Ein Bild des Sliders samt Bildunterschrift. */
data class Screenshot(val fileName: String, val caption: String)

/**
 * Liest das Verzeichnis der Screenshots (REQ-081). Bewusst eine Indexdatei statt eines
 * Verzeichnis-Scans: In einem Jar laesst sich ein Ressourcenverzeichnis nicht zuverlaessig
 * auflisten, und so stehen Reihenfolge und Bildunterschriften an einer Stelle, die man von
 * Hand pflegen kann.
 */
object ScreenshotIndex {

    /**
     * Ein Eintrag pro Zeile, `dateiname | Bildunterschrift`. Leerzeilen und Zeilen, die mit
     * `#` beginnen, sind Kommentar. Fehlt die Unterschrift, steht der Dateiname darunter.
     */
    fun parse(text: String): List<Screenshot> = text.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .map { line ->
            val fileName = line.substringBefore('|').trim()
            val caption = line.substringAfter('|', "").trim()
            Screenshot(fileName, caption.ifEmpty { fileName })
        }
        .filter { it.fileName.isNotEmpty() }
}

/**
 * Bildstrecke mit Vor- und Zurueck-Schaltern, Bildunterschrift und Zaehler (REQ-081).
 *
 * Fehlende Bilddateien werden uebersprungen statt als Luecke gezeigt: Der Index kann
 * Screenshots auffuehren, die noch nicht erstellt sind, ohne dass der Dialog kaputt aussieht.
 * Ist am Ende keines uebrig, erklaert ein Hinweistext, wo die Dateien hingehoeren.
 */
class ScreenshotSlider(
    screenshots: List<Screenshot>,
    private val project: Project? = null,
) : JPanel(BorderLayout()) {

    private val slides: List<Pair<Screenshot, BufferedImage>> = screenshots.mapNotNull { shot ->
        load(shot)?.let { shot to it }
    }

    private var index = 0

    private val canvas = object : JComponent() {
        init {
            // Ein Screenshot der Ansicht ist hier zwangslaeufig klein - der Klick fuehrt zur
            // grossen Fassung mit Zoom (REQ-081).
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            toolTipText = GradleTasksViewBundle.message("configHelp.screenshots.enlarge")
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) = openViewer()
            })
        }

        override fun paintComponent(g: Graphics) {
            val image = slides.getOrNull(index)?.second ?: return
            val scale = minOf(width.toDouble() / image.width, height.toDouble() / image.height, 1.0)
            val w = (image.width * scale).toInt()
            val h = (image.height * scale).toInt()
            (g as Graphics2D).setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR,
            )
            g.drawImage(image, (width - w) / 2, (height - h) / 2, w, h, null)
        }
    }

    private val caption = JLabel("", SwingConstants.CENTER)
    private val counter = JLabel("", SwingConstants.CENTER).apply { foreground = UIUtil.getContextHelpForeground() }

    init {
        if (slides.isEmpty()) {
            add(emptyHint(), BorderLayout.CENTER)
        } else {
            add(canvas, BorderLayout.CENTER)
            add(controls(), BorderLayout.SOUTH)
            show(0)
        }
    }

    private fun emptyHint(): JComponent = JLabel(
        GradleTasksViewBundle.message("configHelp.screenshots.missing"),
        SwingConstants.CENTER,
    ).apply { foreground = UIUtil.getContextHelpForeground() }

    private fun controls(): JComponent = JPanel(BorderLayout()).apply {
        border = JBUI.Borders.emptyTop(6)
        add(
            JPanel(FlowLayout(FlowLayout.CENTER, JBUI.scale(12), 0)).apply {
                add(ActionLink("") { step(-1) }.apply { setIcon(AllIcons.Actions.Play_back, false) })
                add(counter)
                add(ActionLink("") { step(1) }.apply { setIcon(AllIcons.Actions.Play_forward, false) })
            },
            BorderLayout.NORTH,
        )
        add(caption, BorderLayout.SOUTH)
    }

    /** Uebergibt die ganze Strecke, damit sich im grossen Fenster weiterblaettern laesst. */
    private fun openViewer() {
        if (slides.isEmpty()) return
        ScreenshotViewerDialog(project, slides, index).show()
    }

    /** Blaettert zyklisch - am Ende geht es vorne weiter, das erspart das Deaktivieren der Schalter. */
    private fun step(delta: Int) = show((index + delta + slides.size) % slides.size)

    private fun show(position: Int) {
        index = position
        caption.text = slides[position].first.caption
        counter.text = "${position + 1} / ${slides.size}"
        canvas.repaint()
    }

    // Die Screenshots der Ansicht sind hochkant; mit 600x320 blieben sie winzig.
    override fun getPreferredSize(): Dimension = JBUI.size(600, 420)

    private fun load(shot: Screenshot): BufferedImage? {
        val url = javaClass.getResource("$RESOURCE_DIR/${shot.fileName}")
        if (url == null) {
            LOG.info("Screenshot '${shot.fileName}' ist im Index aufgefuehrt, liegt aber nicht im Plugin.")
            return null
        }
        return runCatching { ImageIO.read(url) }
            .onFailure { LOG.warn("Screenshot '${shot.fileName}' konnte nicht gelesen werden", it) }
            .getOrNull()
    }

    companion object {
        const val RESOURCE_DIR: String = "/help/screenshots"
        const val INDEX_RESOURCE: String = "$RESOURCE_DIR/screenshots.txt"
        private val LOG = logger<ScreenshotSlider>()
    }
}
