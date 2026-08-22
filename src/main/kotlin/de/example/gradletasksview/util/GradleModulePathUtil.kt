package de.example.gradletasksview.util

/**
 * Gradle-Projektpfad des Wurzelprojekts. Gradle schreibt jeden Projektpfad mit einem
 * fuehrenden `:`; das Wurzelprojekt selbst heisst deshalb konsequent `":"`.
 */
const val ROOT_MODULE_PATH = ":"

/**
 * `true`, wenn [key] ein Gradle-Projektpfad (Modulblock) und kein Gruppenpfad/Taskname ist.
 *
 * Das Unterscheidungsmerkmal ist bewusst aus Gradle selbst abgeleitet statt erfunden:
 * Projektpfade beginnen immer mit `:`, Gruppenpfade nie. Damit ist `":app"` zweifelsfrei ein
 * Modul und `docker/gui/jsp` zweifelsfrei ein Gruppenpfad, ohne dass die Konfigurationsdatei
 * eine zusaetzliche Verschachtelungsebene braucht (siehe ADR-014).
 */
fun isModulePath(key: String): Boolean = key.startsWith(":")

/**
 * Zerlegt einen Gradle-Projektpfad in seine Segmente: `":app:core"` -> `["app", "core"]`,
 * `":"` -> `[]`. Leere Segmente (z. B. aus `"::"`) werden verworfen, damit ein von Hand
 * geschriebener Schluessel nicht an einem Tippfehler scheitert.
 */
fun moduleSegments(modulePath: String): List<String> =
    modulePath.split(":").filter { it.isNotBlank() }

/** Normalisiert einen von Hand geschriebenen Modulschluessel auf die kanonische Form (`":app:core"`, `":"`). */
fun normalizeModulePath(modulePath: String): String {
    val segments = moduleSegments(modulePath)
    return if (segments.isEmpty()) ROOT_MODULE_PATH else ":" + segments.joinToString(":")
}

/** Elternmodul eines Pfades, oder `null` fuer das Wurzelprojekt. */
fun parentModulePath(modulePath: String): String? {
    val segments = moduleSegments(modulePath)
    return when {
        segments.isEmpty() -> null
        segments.size == 1 -> ROOT_MODULE_PATH
        else -> ":" + segments.dropLast(1).joinToString(":")
    }
}

/**
 * `true`, wenn [modulePath] das Modul [ancestor] selbst ist oder darunter liegt.
 * Verglichen wird segmentweise, damit `":applet"` nicht faelschlich als Untermodul von
 * `":app"` gilt.
 */
fun isModuleWithin(modulePath: String, ancestor: String): Boolean {
    val own = moduleSegments(modulePath)
    val other = moduleSegments(ancestor)
    return own.size >= other.size && own.subList(0, other.size) == other
}

/**
 * Qualifizierter Taskpfad, wie ihn Gradle auf der Kommandozeile versteht:
 * `":app:core"` + `"test"` -> `":app:core:test"`, Wurzelprojekt -> `":test"`.
 */
fun qualifiedTaskPath(modulePath: String, taskName: String): String {
    val segments = moduleSegments(modulePath)
    return if (segments.isEmpty()) ":$taskName" else ":" + segments.joinToString(":") + ":$taskName"
}
