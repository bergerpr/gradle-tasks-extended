package de.example.gradletasksview.util

import de.example.gradletasksview.model.UNNAMED_GROUP_DISPLAY_NAME

/**
 * Zerlegt einen Gradle-Task-Gruppennamen in normalisierte Pfadsegmente.
 *
 * Regeln (siehe REQ-021 und REQ-022):
 * - `/` ist der einzige Separator.
 * - Fuehrende, folgende und doppelte `/` werden entfernt bzw. zusammengefasst,
 *   leere Segmente werden ignoriert.
 * - Segmente werden nur getrimmt, ansonsten unveraendert uebernommen (keine
 *   weitere Normalisierung von Sonderzeichen/Unicode, um Informationsverlust
 *   zu vermeiden).
 * - Tasks ohne (sinnvolle) Gruppe landen in der Gruppe [UNNAMED_GROUP_DISPLAY_NAME].
 */
fun normalizeGroupSegments(groupName: String?): List<String> {
    val raw = groupName?.trim()?.takeIf { it.isNotBlank() } ?: UNNAMED_GROUP_DISPLAY_NAME
    val segments = raw.split("/")
        .map { it.trim() }
        .filter { it.isNotBlank() }
    return segments.ifEmpty { listOf(UNNAMED_GROUP_DISPLAY_NAME) }
}

fun normalizeGroupPath(groupName: String?): String =
    normalizeGroupSegments(groupName).joinToString("/")
