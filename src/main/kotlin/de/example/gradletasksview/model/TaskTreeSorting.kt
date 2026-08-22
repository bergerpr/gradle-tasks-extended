package de.example.gradletasksview.model

/**
 * Case-insensitive Sortierung mit stabiler Zweitsortierung ueber den
 * Originalwert. Verhindert Sortierflackern bei Gross-/Kleinschreibungs-
 * Duplikaten (z. B. "Build" und "build" bekommen sonst bei jedem Rebuild
 * potenziell eine andere relative Reihenfolge).
 */
val caseInsensitiveStableComparator: Comparator<String> =
    Comparator<String> { a, b -> String.CASE_INSENSITIVE_ORDER.compare(a, b) }.thenBy { it }

/**
 * Ordner mit dem Namen [UNNAMED_GROUP_DISPLAY_NAME] werden immer als letztes
 * einsortiert, unabhaengig von der alphabetischen Reihenfolge der uebrigen
 * Eintraege (siehe REQ-022 und REQ-023).
 */
private fun otherLastThen(comparator: Comparator<String>): Comparator<String> =
    Comparator { a, b ->
        val aIsOther = a == UNNAMED_GROUP_DISPLAY_NAME
        val bIsOther = b == UNNAMED_GROUP_DISPLAY_NAME
        when {
            aIsOther && bIsOther -> 0
            aIsOther -> 1
            bIsOther -> -1
            else -> comparator.compare(a, b)
        }
    }

/** Sortierung von Gruppenpfaden in der flachen Gruppenliste des Sort-Modus. */
val groupPathComparator: Comparator<String> = otherLastThen(caseInsensitiveStableComparator)

/** Sortierung von Ordner-Geschwisterknoten im Nested-Modus. */
val folderComparator: Comparator<NestedFolderNode> =
    Comparator { a, b -> otherLastThen(caseInsensitiveStableComparator).compare(a.displayName, b.displayName) }

/** Sortierung von Tasks innerhalb einer Gruppe bzw. eines Ordners. */
val taskComparator: Comparator<GradleTaskDescriptor> =
    Comparator { a, b -> caseInsensitiveStableComparator.compare(a.taskName, b.taskName) }
