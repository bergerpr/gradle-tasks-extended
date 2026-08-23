package de.berger.gradletasksview.ui

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.HyperlinkEventAction
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.IconUtil
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import de.berger.gradletasksview.GradleTasksBuildInfo
import de.berger.gradletasksview.GradleTasksViewBundle
import de.berger.gradletasksview.actions.openInEditor
import de.berger.gradletasksview.persistence.GradleTasksViewConfigStore
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.io.File
import javax.swing.Action
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.event.HyperlinkEvent

/**
 * Die Dokumentation hinter dem Info-Knopf der Toolbar (REQ-080): wo die Konfiguration liegt,
 * was die beiden Dateien voneinander unterscheidet und wie ein Eintrag aufgebaut ist.
 *
 * Der Text steht als HTML-Ressource in `src/main/resources/help/configuration.html` und nicht
 * im Quellcode - so laesst er sich lesen und pflegen wie ein Dokument. Eingesetzt werden nur
 * die drei Pfade des geoeffneten Projekts, und zwar aus [GradleTasksViewConfigStore] selbst:
 * Eine Hilfe, die auf ein anderes Verzeichnis zeigt als das, aus dem gelesen wird, waere
 * schlimmer als gar keine.
 */
class GradleTasksConfigHelpDialog(private val project: Project) : DialogWrapper(project) {

    private val store = project.service<GradleTasksViewConfigStore>()

    init {
        title = GradleTasksViewBundle.message("configHelp.title")
        setOKButtonText(GradleTasksViewBundle.message("configHelp.close"))
        init()
    }

    override fun createCenterPanel(): JComponent {
        val tabs = JBTabbedPane().apply {
            addTab(GradleTasksViewBundle.message("configHelp.tab.description"), createDescriptionTab())
            addTab(GradleTasksViewBundle.message("configHelp.tab.configuration"), createConfigurationTab())
        }

        // Der Kopf bleibt ueber den Reitern stehen - sonst waere die Version genau dann weg,
        // wenn man in der Konfiguration nachliest.
        return JPanel(BorderLayout()).apply {
            add(createHeader(), BorderLayout.NORTH)
            add(tabs, BorderLayout.CENTER)
            preferredSize = JBUI.size(780, 720)
        }
    }

    /**
     * Ein durchgehender, scrollender Block: erster Absatz, Bildstrecke, Merkmalsliste,
     * Fussnote. Frueher standen Text und Bilder in getrennten Bereichen mit eigenem Bildlauf
     * - das zerschnitt die Beschreibung an einer Stelle, an der inhaltlich nichts endet.
     *
     * Gesetzt ueber das UI-DSL der Plattform, weil dessen `text`/`comment` Umbruch, Abstaende
     * und Themefarben mitbringen; ein `JEditorPane` in einer Spalte muesste seine Hoehe bei
     * jeder Breitenaenderung selbst nachrechnen.
     */
    private fun createDescriptionTab(): JComponent {
        val parts = page(DESCRIPTION_RESOURCE).split(SCREENSHOT_MARKER, FOOTNOTE_MARKER)
        val links = HyperlinkEventAction { event -> openLink(event.description) }

        val content = panel {
            row { text(parts.getOrElse(0) { "" }, action = links) }
            row { cell(ScreenshotSlider(screenshots(), project)).align(AlignX.FILL) }.topGap(TopGap.SMALL)
            row { text(parts.getOrElse(1) { "" }, action = links) }.topGap(TopGap.SMALL)
            row { comment(parts.getOrElse(2) { "" }, action = links) }.topGap(TopGap.SMALL)
        }.apply { border = JBUI.Borders.empty(8, 10) }

        return JBScrollPane(content).apply { border = JBUI.Borders.empty() }
    }

    /** Pfade und Aufbau der Konfiguration - der Teil, der frueher den ganzen Dialog fuellte. */
    private fun createConfigurationTab(): JComponent =
        JBScrollPane(htmlPane(helpHtml())).apply { border = JBUI.Borders.empty(4) }

    private fun htmlPane(html: String): JEditorPane = JEditorPane().apply {
        editorKit = HTMLEditorKitBuilder.simple()
        isEditable = false
        background = UIUtil.getPanelBackground()
        text = html
        caretPosition = 0
        addHyperlinkListener { event ->
            if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) openLink(event.description)
        }
    }

    private fun screenshots(): List<Screenshot> =
        ScreenshotIndex.parse(page(ScreenshotSlider.INDEX_RESOURCE))

    /**
     * Kopfzeile: links das Logo, rechts Name, eine Zusammenfassung in einer Zeile und die
     * ausklappbaren Kenndaten. Die frueher mittige, immer offene Tabelle nahm die halbe
     * Dialoghoehe ein, obwohl das meiste davon nur im Fehlerfall interessiert - deshalb steht
     * jetzt oben, was man haeufig braucht (Version, Build, Branch), und der Rest eine Zeile
     * tiefer hinter "Details".
     *
     * Bewusst Swing statt HTML: So laedt der [IconLoader] das Bild in der zur
     * Bildschirmaufloesung passenden Variante (`elefant.png` bzw. `elefant@2x.png`), waehrend
     * ein `<img>` im JEditorPane unabhaengig von der Skalierung immer dieselbe Pixelgroesse
     * zoege.
     */
    private fun createHeader(): JComponent = JPanel(BorderLayout()).apply {
        border = JBUI.Borders.empty(12, 12, 8, 12)

        logo()?.let { icon ->
            add(
                JLabel(IconUtil.downscaleIconToSize(icon, LOGO_SIZE, LOGO_SIZE)).apply {
                    verticalAlignment = SwingConstants.TOP
                    border = JBUI.Borders.emptyRight(14)
                },
                BorderLayout.WEST,
            )
        }
        add(createSummary(), BorderLayout.CENTER)
    }

    private fun createSummary(): JComponent = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)

        add(JLabel(GradleTasksBuildInfo.pluginName).leftAligned().apply {
            font = font.deriveFont(Font.BOLD, font.size + JBUI.scale(3).toFloat())
        })
        add(JLabel(headline()).leftAligned().apply { foreground = UIUtil.getContextHelpForeground() })
        add(Box.createVerticalStrut(JBUI.scale(6)))

        // Klappabschnitt ueber das UI-DSL der Plattform: `HideableDecorator` waere die
        // naheliegende Klasse, ist aber in beiden Ueberladungen abgekuendigt (ADR-008).
        add(
            panel {
                collapsibleGroup(GradleTasksViewBundle.message("configHelp.details"), indent = false) {
                    row { cell(createFactsTable()) }
                }.expanded = false
            }.leftAligned(),
        )
    }

    /** Die drei Angaben, nach denen man im Alltag fragt - alles Weitere steckt in "Details". */
    private fun headline(): String = listOf(
        GradleTasksViewBundle.message("configHelp.fact.version") + " " + GradleTasksBuildInfo.version,
        GradleTasksBuildInfo.gitCommit,
        branchDescription(),
    ).joinToString(" · ")

    private fun logo(): Icon? = IconLoader.findIcon(LOGO_RESOURCE, javaClass.classLoader)
        .also { if (it == null) LOG.warn("Logo-Ressource '$LOGO_RESOURCE' fehlt im Plugin-Klassenlader.") }

    /** Zweispaltig und linksbuendig, passend zum Text darunter. */
    private fun createFactsTable(): JComponent = JPanel(GridBagLayout()).apply {
        isOpaque = false
        facts().forEachIndexed { row, (label, value) ->
            add(
                JLabel("$label:").apply { foreground = UIUtil.getContextHelpForeground() },
                GridBagConstraints().apply {
                    gridx = 0; gridy = row; anchor = GridBagConstraints.LINE_START
                    insets = JBUI.insets(1, 0, 1, 8)
                },
            )
            add(
                value,
                GridBagConstraints().apply {
                    gridx = 1; gridy = row; anchor = GridBagConstraints.LINE_START
                    insets = JBUI.insets(1, 0)
                },
            )
        }
    }

    private fun facts(): List<Pair<String, JComponent>> = buildList {
        val application = ApplicationInfo.getInstance()
        add(message("version") to JLabel(GradleTasksBuildInfo.version))
        add(message("build") to JLabel(buildDescription()))
        add(message("branch") to JLabel(branchDescription()))
        add(message("pluginId") to JLabel(GradleTasksBuildInfo.pluginId))
        add(message("vendor") to JLabel(vendorDescription()))
        installationDirectory()?.let { directory ->
            add(message("installedAt") to installationLink(directory))
        }
        add(message("builtAgainst") to JLabel(GradleTasksBuildInfo.targetPlatform))
        add(
            message("requires") to
                JLabel(GradleTasksViewBundle.message("configHelp.fact.requires.value", GradleTasksBuildInfo.sinceBuild)),
        )
        add(message("runningIn") to JLabel("${application.fullApplicationName} (${application.build.asString()})"))
        add(message("jre") to JLabel(System.getProperty("java.runtime.version", "?")))
    }

    private fun message(fact: String): String = GradleTasksViewBundle.message("configHelp.fact.$fact")

    private fun buildDescription(): String =
        "${GradleTasksBuildInfo.gitCommit} (${GradleTasksBuildInfo.buildTime})"

    /** Der Zusatz macht sichtbar, dass ein Build nicht aus einem eingecheckten Stand stammt. */
    private fun branchDescription(): String = when {
        GradleTasksBuildInfo.gitDirty ->
            GradleTasksViewBundle.message("configHelp.fact.branch.dirty", GradleTasksBuildInfo.gitBranch)
        else -> GradleTasksBuildInfo.gitBranch
    }

    private fun vendorDescription(): String =
        listOf(GradleTasksBuildInfo.vendor, GradleTasksBuildInfo.vendorEmail, GradleTasksBuildInfo.vendorUrl)
            .filter { it.isNotBlank() }
            .joinToString(" · ")

    /**
     * Anklickbar und auf `~` gekuerzt: Der volle Sandbox- oder Installationspfad waere breiter
     * als der Dialog, steht aber im Tooltip.
     */
    private fun installationLink(directory: File): JComponent = HyperlinkLabel(
        FileUtil.getLocationRelativeToUserHome(directory.absolutePath),
    ).apply {
        toolTipText = directory.absolutePath
        addHyperlinkListener { openLink(directory.absolutePath) }
    }

    /**
     * Verzeichnis, aus dem dieses Plugin geladen wurde - hilfreich, wenn unklar ist, welche
     * Kopie gerade laeuft. Ermittelt ueber die Herkunft der eigenen Klasse (`<plugin>/lib/x.jar`
     * liegt zwei Ebenen unter dem Plugin-Verzeichnis); die Plattform-API dafuer waere
     * `@ApiStatus.Internal` (siehe [GradleTasksBuildInfo]).
     */
    private fun installationDirectory(): File? = runCatching {
        val source = javaClass.protectionDomain?.codeSource?.location?.toURI()?.let(::File) ?: return null
        if (source.isDirectory) source else source.parentFile?.takeIf { it.name == "lib" }?.parentFile ?: source.parentFile
    }.onFailure { LOG.warn("Installationsverzeichnis des Plugins nicht ermittelbar", it) }.getOrNull()

    private fun <T : JComponent> T.leftAligned(): T = apply { alignmentX = Component.LEFT_ALIGNMENT }

    /** Nur "Close" - der Dialog aendert nichts, ein OK/Cancel-Paar waere irrefuehrend. */
    override fun createActions(): Array<Action> = arrayOf(okAction)

    private fun openLink(target: String) {
        if (target == LICENSE_LINK) {
            showLicense()
            return
        }
        val file = File(target)
        if (!file.isAbsolute) return
        // Der Dialog ist modal: Wuerde er offen bleiben, laege die geoeffnete Datei
        // unerreichbar dahinter.
        close(OK_EXIT_CODE)
        openInEditor(project, file)
    }

    /**
     * Zeigt den mitgelieferten Lizenztext der Icons. Pflicht ist das nicht - die MIT-Lizenz
     * verlangt nur, dass der Text mitgeliefert wird, und das tut die Datei im Plugin-Jar
     * ohnehin. Sichtbar ist er trotzdem besser als vergraben (REQ-081).
     */
    private fun showLicense() {
        val text = page(LICENSE_RESOURCE).ifEmpty { return }
        PlainTextDialog(project, GradleTasksViewBundle.message("configHelp.license.title"), text).show()
    }

    /** Rohtext einer mitgelieferten Ressource; fehlt sie, bleibt der Bereich leer statt zu scheitern. */
    private fun page(resource: String): String =
        javaClass.getResourceAsStream(resource)?.use { it.readBytes().decodeToString() }
            ?: "".also { LOG.warn("Ressource '$resource' fehlt im Plugin-Klassenlader.") }

    /** Die Farben, die beide Seiten brauchen - der Rest ist je Seite verschieden. */
    private fun withThemeColors(html: String): String = html
        .replace("{{linkColor}}", hex(JBUI.CurrentTheme.Link.Foreground.ENABLED))
        .replace("{{textColor}}", hex(UIUtil.getLabelForeground()))
        .replace("{{mutedColor}}", hex(UIUtil.getContextHelpForeground()))

    private fun helpHtml(): String {
        val template = page(HELP_RESOURCE).ifEmpty {
            return GradleTasksViewBundle.message("configHelp.missingResource", HELP_RESOURCE)
        }

        return withThemeColors(template)
            // Beispiel im Farbschema des Editors, damit es zu dem passt, was beim Bearbeiten
            // der YAML danebensteht.
            .replace("{{codeBackground}}", hex(EditorColorsManager.getInstance().globalScheme.defaultBackground))
            .replace("{{example}}", YamlSnippetHighlighter.toHtml(page(EXAMPLE_RESOURCE), editorPalette()))
            .replace("{{defaultFile}}", escape(store.defaultConfigFile.absolutePath))
            .replace("{{customFile}}", escape(store.customConfigFile.absolutePath))
            .replace("{{iconsDir}}", escape(store.iconsDirectory.absolutePath))
            // Angezeigt wird der auf `~` gekuerzte Pfad: Der volle waere breiter als der
            // Dialog und zwaenge die Beschreibung in einen waagerechten Bildlauf.
            .replace("{{defaultFileShort}}", escape(shortPath(store.defaultConfigFile)))
            .replace("{{customFileShort}}", escape(shortPath(store.customConfigFile)))
            .replace("{{iconsDirShort}}", escape(shortPath(store.iconsDirectory)))
    }

    /** Die Farben des aktiven Editor-Schemas, damit das Beispiel aussieht wie die Datei daneben. */
    private fun editorPalette(): YamlHighlightPalette {
        val scheme = EditorColorsManager.getInstance().globalScheme
        fun foreground(key: TextAttributesKey): Color? = scheme.getAttributes(key)?.foregroundColor
        return YamlHighlightPalette(
            key = foreground(DefaultLanguageHighlighterColors.KEYWORD),
            string = foreground(DefaultLanguageHighlighterColors.STRING),
            number = foreground(DefaultLanguageHighlighterColors.NUMBER),
            comment = foreground(DefaultLanguageHighlighterColors.LINE_COMMENT),
        )
    }

    private fun shortPath(file: File): String = FileUtil.getLocationRelativeToUserHome(file.absolutePath)

    private fun hex(color: Color): String = "#%02x%02x%02x".format(color.red, color.green, color.blue)

    /** Projektpfade sind Fremdtext: Sonderzeichen duerfen das umgebende HTML nicht zerlegen. */
    private fun escape(path: String): String =
        path.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private companion object {
        private const val HELP_RESOURCE = "/help/configuration.html"
        private const val DESCRIPTION_RESOURCE = "/help/description.html"

        /** Trennmarken der drei Fragmente in `description.html`. */
        private const val SCREENSHOT_MARKER = "<!-- screenshots -->"
        private const val FOOTNOTE_MARKER = "<!-- footnote -->"
        private const val LICENSE_RESOURCE = "/icons/LICENSE-icons.txt"

        /** Kein Dateipfad, sondern ein eigener Verweis - siehe `openLink`. */
        private const val LICENSE_LINK = "license:icons"
        private const val LOGO_RESOURCE = "/images/elefant.png"

        /** Klein genug, dass Logo und Zusammenfassung zusammen kaum hoeher sind als drei Zeilen. */
        private const val LOGO_SIZE = 64
        private const val EXAMPLE_RESOURCE = "/help/example-config.yaml"
        private val LOG = logger<GradleTasksConfigHelpDialog>()
    }
}
