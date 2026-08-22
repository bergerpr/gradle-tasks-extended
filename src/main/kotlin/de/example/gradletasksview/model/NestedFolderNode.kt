package de.example.gradletasksview.model

/**
 * Virtueller Ordnerknoten im Nested-Modus. Wird ausschliesslich im Speicher
 * aus [GradleTaskDescriptor.groupName]-Pfaden aufgebaut und nicht persistiert -
 * persistiert werden nur die Expand/Collapse-Zustaende (siehe persistence-Paket).
 */
data class NestedFolderNode(
    val displayName: String,
    val fullPath: String,
    val childrenTasks: MutableList<GradleTaskDescriptor> = mutableListOf(),
    val childrenFolders: MutableMap<String, NestedFolderNode> = linkedMapOf()
)
