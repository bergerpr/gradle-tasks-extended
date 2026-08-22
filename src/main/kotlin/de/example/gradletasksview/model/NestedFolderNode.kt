package de.example.gradletasksview.model

/**
 * Virtueller Ordnerknoten im Nested-Modus. Wird ausschliesslich im Speicher
 * aus [GradleTaskDescriptor.groupName]-Pfaden aufgebaut und nicht persistiert -
 * persistiert werden nur die Expand/Collapse-Zustaende (siehe persistence-Paket).
 *
 * @property displayName Letztes Segment von [fullPath]. Immer genau ein Segment, auch bei
 *   zusammengefassten Knoten - fuer die Anzeige ist [compactedName] zustaendig.
 * @property fullPath Vollstaendiger Gruppenpfad ab der Wurzel (`docker/server/gui`). Dient als
 *   Konfigurationsschluessel und bleibt davon unberuehrt, ob Ebenen zusammengefasst wurden.
 * @property compactedName Text, der im Baum erscheint. Normalerweise gleich [displayName];
 *   bei zusammengefassten Ebenen die verschmolzenen Segmente (`server/gui`), siehe REQ-004.
 */
data class NestedFolderNode(
    val displayName: String,
    val fullPath: String,
    val childrenTasks: MutableList<GradleTaskDescriptor> = mutableListOf(),
    val childrenFolders: MutableMap<String, NestedFolderNode> = linkedMapOf(),
    val compactedName: String = displayName
)
