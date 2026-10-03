package de.berger.gradletasksview.icons

import com.intellij.icons.AllIcons
import icons.ExternalSystemIcons
import javax.swing.Icon

/**
 * Die eingebauten Baum-Icons - durchweg Icons der Plattform, dieselben wie in der originalen
 * Gradle-Ansicht der IDE (ADR-026). Das Plugin liefert dafuer keine eigenen Dateien mit, also
 * auch keine fremde Lizenz; Hell/Dunkel und New UI waehlt die Plattform selbst.
 *
 * Ein in `default.yaml`/`custom.yaml` konfiguriertes Icon hat Vorrang; diese hier sind nur der
 * Rueckfall.
 */
object GradleTasksIcons {

    /** Task-Blatt (Default/Sort/Nested). */
    val Task: Icon = ExternalSystemIcons.Task

    /**
     * Gruppenzeile in `Default`/`Sort` und virtueller Ordner in `Nested`. Ein Icon fuer beide
     * Zustaende: Eine aufgeklappte Variante gibt es in der Plattform nicht mehr, und auch
     * Projekt- und Gradle-Ansicht zeigen aufgeklappt dasselbe Symbol.
     */
    val Folder: Icon = AllIcons.Nodes.ConfigFolder

    /**
     * Modulknoten bei eingeschalteter Modulgliederung. Bewusst ein anderes Symbol als
     * [Folder]: Ein Modul ist keine erfundene Gliederung, sondern ein echtes Gradle-Projekt.
     */
    val Module: Icon = AllIcons.Nodes.Module

    /**
     * Der Strukturknoten `Tasks` unterhalb eines Moduls im `Default`-Modus (REQ-077) - wie in
     * der originalen Gradle-Ansicht dasselbe Symbol wie eine Gruppe. Anders als [Folder] laesst
     * er sich nicht konfigurieren, kann also nie durch ein eigenes Icon ersetzt werden.
     */
    val TaskList: Icon = AllIcons.Nodes.ConfigFolder
}
