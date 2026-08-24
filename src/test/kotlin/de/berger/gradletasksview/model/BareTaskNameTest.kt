package de.berger.gradletasksview.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `TaskData.name` der Plattform ist bei Untermodulen der qualifizierte Pfad und nur im
 * Wurzelprojekt der blanke Name (ADR-024). Die Umrechnung ist winzig, aber an ihr haengen
 * Beschriftung, Konfigurationsschluessel, Suche und Ausfuehrung gleichermassen - deshalb
 * bekommt sie einen eigenen Test.
 */
class BareTaskNameTest {

    @Test
    fun `a qualified name is reduced to the plain task name`() {
        assertEquals("printLibGui", bareTaskName(":lib:printLibGui"))
        assertEquals("printCoreReact", bareTaskName(":app:core:printCoreReact"))
    }

    @Test
    fun `a name without a module prefix stays as it is`() {
        assertEquals("wrapper", bareTaskName("wrapper"))
    }

    @Test
    fun `a leading colon alone is a root project task`() {
        assertEquals("assembleAll", bareTaskName(":assembleAll"))
    }
}
