package org.tasks.jobs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.lang.reflect.Method

/**
 * Tests for the printDuration function in WorkManagerImpl.kt.
 * Since it's a file-level private function, we access it via reflection.
 */
class PrintDurationTest {

    private fun callPrintDuration(millis: Long): String {
        // printDuration is compiled as a static method in WorkManagerImplKt class
        val clazz = Class.forName("org.tasks.jobs.WorkManagerImplKt")
        val method = clazz.getDeclaredMethod("printDuration", Long::class.java)
        method.isAccessible = true
        return method.invoke(null, millis) as String
    }

    @Test
    fun zeroDuration() {
        val result = callPrintDuration(0L)
        assertNotNull(result)
        // In DEBUG mode: "0h 0m 0s"
        assertEquals("0h 0m 0s", result)
    }

    @Test
    fun oneSecond() {
        val result = callPrintDuration(1000L)
        assertEquals("0h 0m 1s", result)
    }

    @Test
    fun oneMinute() {
        val result = callPrintDuration(60_000L)
        assertEquals("0h 1m 0s", result)
    }

    @Test
    fun oneHour() {
        val result = callPrintDuration(3_600_000L)
        assertEquals("1h 0m 0s", result)
    }

    @Test
    fun oneHourThirtyMinutesTenSeconds() {
        val millis = 3_600_000L + 30 * 60_000L + 10 * 1000L
        val result = callPrintDuration(millis)
        assertEquals("1h 30m 10s", result)
    }

    @Test
    fun twentyThreeHoursFiftyNineMinutesFiftyNineSeconds() {
        val millis = 23 * 3_600_000L + 59 * 60_000L + 59 * 1000L
        val result = callPrintDuration(millis)
        assertEquals("23h 59m 59s", result)
    }

    @Test
    fun negativeDuration() {
        // Negative millis: should still format (may have negative values)
        val result = callPrintDuration(-1000L)
        assertNotNull(result)
    }

    @Test
    fun largeValue() {
        val millis = 100 * 3_600_000L // 100 hours
        val result = callPrintDuration(millis)
        assertEquals("100h 0m 0s", result)
    }

    @Test
    fun subSecondDurationShowsZero() {
        val result = callPrintDuration(500L)
        assertEquals("0h 0m 0s", result)
    }
}
