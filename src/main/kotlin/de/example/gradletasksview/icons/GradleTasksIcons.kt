package de.example.gradletasksview.icons

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.IconLoader
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon

/**
 * Eigene, im Plugin mitgelieferte Icons (`src/main/resources/icons/`) statt Plattform-Icons
 * aus `AllIcons`/`ExternalSystemIcons`.
 *
 * Hintergrund: Die frueher verwendeten Plattform-Icons wurden im Baum nicht sichtbar
 * gezeichnet (siehe ADR-005). Eigene Ressourcen haben zwei Vorteile:
 * Sie liegen im Klassenlader des Plugins selbst (kein Zugriff ueber Modulgrenzen hinweg,
 * die sich zwischen Plattformversionen verschieben koennen), und ein fehlendes bzw. nicht
 * aufloesbares Icon laesst sich hier eindeutig von einem korrekt geladenen unterscheiden -
 * `IconLoader.findIcon` liefert dann `null`, waehrend `IconLoader.getIcon` still ein leeres,
 * transparentes Platzhalter-Icon zurueckgibt, das im Log wie ein gueltiges 16x16-Icon aussieht.
 *
 * Quelle/Lizenz der SVGs: siehe `src/main/resources/icons/LICENSE-icons.txt` (MIT).
 * Zu jedem Icon existiert eine `_dark`-Variante, die die Plattform bei dunklen Themes
 * automatisch anstelle der Light-Variante laedt.
 */
object GradleTasksIcons {

    private val LOG = logger<GradleTasksIcons>()

    /** Task-Blatt (Default/Sort/Nested). */
    val Task: Icon = load("/icons/gradleTask.svg")

    /**
     * Zugeklappter Container: Gruppenzeile in `Default`/`Sort` und virtueller Ordner in
     * `Nested` verwenden bewusst dasselbe Icon.
     */
    val Folder: Icon = load("/icons/taskFolder.svg")

    /** Aufgeklappter Container, siehe [Folder]. */
    val FolderOpen: Icon = load("/icons/taskFolderOpen.svg")

    /**
     * Laedt ein Icon aus den Plugin-Ressourcen. Bewusst `findIcon` (nullable) statt
     * `getIcon`: So faellt ein Tippfehler im Pfad oder eine nicht mitgebuendelte Datei
     * sofort im Log auf, statt sich als unsichtbares Icon zu tarnen.
     */
    private fun load(path: String): Icon =
        IconLoader.findIcon(path, GradleTasksIcons::class.java.classLoader)
            ?: MissingIcon.also {
                LOG.warn("Icon-Ressource '$path' wurde im Plugin-Klassenlader nicht gefunden - " +
                    "es wird ein auffaelliges Ersatz-Icon gezeichnet.")
            }

    /**
     * Absichtlich haesslicher, rein programmatisch gezeichneter Notnagel: ein magentafarbenes
     * Quadrat. Es ist auf jedem Theme-Hintergrund sichtbar, macht ein Ressourcenproblem also
     * unuebersehbar, statt es hinter einer leeren Flaeche zu verstecken.
     */
    private object MissingIcon : Icon {
        override fun getIconWidth(): Int = 16
        override fun getIconHeight(): Int = 16
        override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
            val old = g.color
            g.color = java.awt.Color(255, 0, 255)
            g.fillRect(x + 2, y + 2, 12, 12)
            g.color = old
        }
    }
}
