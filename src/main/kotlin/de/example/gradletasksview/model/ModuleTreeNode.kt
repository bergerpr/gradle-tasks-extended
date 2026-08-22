package de.example.gradletasksview.model

import de.example.gradletasksview.util.ROOT_MODULE_PATH
import de.example.gradletasksview.util.moduleSegments

/**
 * Knoten der Modulebene. Anders als [NestedFolderNode] ist das keine erfundene Gliederung,
 * sondern die Gradle-Projektstruktur selbst: `:app:core` haengt unter `:app`, nicht daneben.
 *
 * @property gradlePath Absoluter Gradle-Projektpfad (`":"`, `":app"`, `":app:core"`). Dient
 *   zugleich als Anzeigetext und als Konfigurationsschluessel.
 * @property moduleName Anzeigename aus dem IDE-Projektmodell. Leer bei Zwischenknoten, die
 *   nur aufgebaut werden, damit die Struktur nicht zerreisst.
 * @property tasks Tasks dieses Moduls; die Gliederung darunter (Gruppen bzw. Ordner) entsteht
 *   erst beim Aufbau des Swing-Modells, weil sie vom aktiven Ansichtsmodus abhaengt.
 */
data class ModuleTreeNode(
    val gradlePath: String,
    var moduleName: String = "",
    val tasks: MutableList<GradleTaskDescriptor> = mutableListOf(),
    val childrenModules: MutableMap<String, ModuleTreeNode> = linkedMapOf()
)

/**
 * Baut die Modulhierarchie aus den Tasks auf. Das Wurzelprojekt `":"` ist immer der oberste
 * Knoten - so wie in der originalen Gradle-Ansicht -, auch wenn es selbst keine Tasks hat;
 * ebenso werden Zwischenmodule ohne eigene Tasks angelegt, damit `:app:core` nicht
 * elternlos dasteht.
 *
 * Geschwistermodule werden alphabetisch sortiert, und zwar in **allen** Modi: Die Modulebene
 * bildet die Projektstruktur ab und ist keine von Gradle gelieferte Reihenfolge, die der
 * `Default`-Modus bewahren koennte (siehe REQ-074).
 */
fun buildModuleTree(tasks: List<GradleTaskDescriptor>): ModuleTreeNode {
    val root = ModuleTreeNode(ROOT_MODULE_PATH)
    val byPath = hashMapOf(ROOT_MODULE_PATH to root)

    fun nodeFor(modulePath: String, moduleName: String): ModuleTreeNode {
        byPath[modulePath]?.let { existing ->
            // Ein zuvor als Zwischenknoten angelegtes Modul bekommt seinen Anzeigenamen
            // nachtraeglich, sobald ein Task es tatsaechlich als sein Modul ausweist.
            if (existing.moduleName.isEmpty()) existing.moduleName = moduleName
            return existing
        }

        val segments = moduleSegments(modulePath)
        val parentPath = if (segments.size <= 1) ROOT_MODULE_PATH else ":" + segments.dropLast(1).joinToString(":")
        val node = ModuleTreeNode(modulePath, moduleName)
        byPath[modulePath] = node
        nodeFor(parentPath, "").childrenModules[modulePath] = node
        return node
    }

    tasks.forEach { task -> nodeFor(task.gradleProjectPath, task.moduleName).tasks += task }

    sortModulesRecursively(root)
    return root
}

private fun sortModulesRecursively(node: ModuleTreeNode) {
    val sorted = node.childrenModules.values.sortedWith(
        compareBy(caseInsensitiveStableComparator) { it.gradlePath }
    )
    node.childrenModules.clear()
    sorted.forEach { child ->
        node.childrenModules[child.gradlePath] = child
        sortModulesRecursively(child)
    }
}
