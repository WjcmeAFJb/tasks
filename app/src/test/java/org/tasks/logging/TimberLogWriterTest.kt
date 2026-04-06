package org.tasks.logging

import co.touchlab.kermit.Severity
import org.junit.Assert.assertEquals
import org.junit.Test

class TimberLogWriterTest {

    private val writer = TimberLogWriter()

    // Test the severity-to-priority mapping via reflection since toTimberPriority is private
    @Test
    fun verboseSeverityMapsToVerbosePriority() {
        val priority = callToTimberPriority(Severity.Verbose)
        assertEquals(android.util.Log.VERBOSE, priority)
    }

    @Test
    fun debugSeverityMapsToDebugPriority() {
        val priority = callToTimberPriority(Severity.Debug)
        assertEquals(android.util.Log.DEBUG, priority)
    }

    @Test
    fun infoSeverityMapsToInfoPriority() {
        val priority = callToTimberPriority(Severity.Info)
        assertEquals(android.util.Log.INFO, priority)
    }

    @Test
    fun warnSeverityMapsToWarnPriority() {
        val priority = callToTimberPriority(Severity.Warn)
        assertEquals(android.util.Log.WARN, priority)
    }

    @Test
    fun errorSeverityMapsToErrorPriority() {
        val priority = callToTimberPriority(Severity.Error)
        assertEquals(android.util.Log.ERROR, priority)
    }

    @Test
    fun assertSeverityMapsToAssertPriority() {
        val priority = callToTimberPriority(Severity.Assert)
        assertEquals(android.util.Log.ASSERT, priority)
    }

    @Test
    fun allSeveritiesMapToDistinctPriorities() {
        val priorities = Severity.entries.map { callToTimberPriority(it) }.toSet()
        assertEquals(Severity.entries.size, priorities.size)
    }

    @Test
    fun allPrioritiesArePositive() {
        Severity.entries.forEach { severity ->
            val priority = callToTimberPriority(severity)
            assert(priority > 0) { "$severity mapped to non-positive priority $priority" }
        }
    }

    private fun callToTimberPriority(severity: Severity): Int {
        val method = TimberLogWriter::class.java.getDeclaredMethod(
            "toTimberPriority", Severity::class.java
        )
        method.isAccessible = true
        return method.invoke(writer, severity) as Int
    }
}
