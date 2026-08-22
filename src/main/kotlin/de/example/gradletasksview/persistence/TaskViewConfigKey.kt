package de.example.gradletasksview.persistence

/**
 * Adressiert einen konfigurierbaren Baumknoten in `default.yaml`/`custom.yaml`.
 *
 * Es gibt genau drei Formen:
 * - [module] - der Modulknoten selbst (`":app"`), konfiguriert ueber `expanded`/`icon`
 *   innerhalb seines Blocks.
 * - [scopedNode] - ein Gruppen-/Ordnerpfad oder Taskname innerhalb eines bestimmten Moduls;
 *   steht in der YAML-Datei unter dem Modulblock.
 * - [scopedNode] mit `modulePath = null` - derselbe Schluessel ohne Modulbezug, auf oberster
 *   Ebene der Datei; er gilt fuer alle Module und ist die Form, die es schon vor der
 *   Modulgliederung gab. Diesen Fall trifft man automatisch, wenn die Modulgliederung
 *   ausgeschaltet ist.
 *
 * Die benannten Fabrikmethoden sind der vorgesehene Weg: Sie machen an der Aufrufstelle
 * sichtbar, welche Form gemeint ist, was zwei nebeneinanderstehende nullable Strings nicht
 * leisten.
 */
data class TaskViewConfigKey(val modulePath: String?, val nodeKey: String?) {

    companion object {
        fun module(modulePath: String) = TaskViewConfigKey(modulePath, null)

        /**
         * [modulePath] ist `null`, wenn die Modulgliederung ausgeschaltet ist; dann greift der
         * modulunabhaengige Schluessel auf oberster Ebene der Datei.
         */
        fun scopedNode(nodeKey: String, modulePath: String?) = TaskViewConfigKey(modulePath, nodeKey)
    }
}
