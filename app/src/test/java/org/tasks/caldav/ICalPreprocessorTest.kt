package org.tasks.caldav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.caldav.Task.Companion.tasksFromReader
import java.io.StringReader

class ICalPreprocessorTest {

    // ---- FixInvalidUtcOffsetPreprocessor ----

    @Test
    fun fixInvalidUtcOffsetFrom() {
        val input = "TZOFFSETFROM:+1800"
        val result = FixInvalidUtcOffsetPreprocessor.fixString(input)
        assertEquals("TZOFFSETFROM:+001800", result)
    }

    @Test
    fun fixInvalidUtcOffsetTo() {
        val input = "TZOFFSETTO:-1900"
        val result = FixInvalidUtcOffsetPreprocessor.fixString(input)
        assertEquals("TZOFFSETTO:-001900", result)
    }

    @Test
    fun fixInvalidUtcOffsetMultipleOccurrences() {
        val input = """
            TZOFFSETFROM:+1800
            TZOFFSETTO:-2000
        """.trimIndent()
        val result = FixInvalidUtcOffsetPreprocessor.fixString(input)
        assertTrue(result.contains("TZOFFSETFROM:+001800"))
        assertTrue(result.contains("TZOFFSETTO:-002000"))
    }

    @Test
    fun validUtcOffsetLeftAlone() {
        val input = "TZOFFSETFROM:+0100"
        val result = FixInvalidUtcOffsetPreprocessor.fixString(input)
        assertEquals("TZOFFSETFROM:+0100", result)
    }

    @Test
    fun validFourDigitOffsetLeftAlone() {
        val input = "TZOFFSETTO:-0530"
        val result = FixInvalidUtcOffsetPreprocessor.fixString(input)
        assertEquals("TZOFFSETTO:-0530", result)
    }

    @Test
    fun regexpForUtcOffsetMatchesBadOffset() {
        val regex = FixInvalidUtcOffsetPreprocessor.regexpForProblem()
        assertNotNull(regex)
        assertTrue(regex!!.containsMatchIn("TZOFFSETFROM:+1800"))
    }

    @Test
    fun regexpForUtcOffsetDoesNotMatchValidOffset() {
        val regex = FixInvalidUtcOffsetPreprocessor.regexpForProblem()
        assertNotNull(regex)
        // +0100 is a valid 4-digit offset and should NOT match the bad-offset regex
        // The regex requires 18xx, 19xx, 2xxx-6xxx patterns which are invalid hour values
        val matches = regex!!.containsMatchIn("TZOFFSETFROM:+0100")
        // 0100 doesn't match the pattern (18|19|[2-6]\d)\d\d
        assertTrue(!matches)
    }

    // ---- FixInvalidDayOffsetPreprocessor ----

    @Test
    fun fixDurationWithTBeforeDay() {
        // PT2D should be P2D (T belongs before time components, not days)
        val input = "DURATION:PT2D"
        val result = FixInvalidDayOffsetPreprocessor.fixString(input)
        assertEquals("DURATION:P2D", result)
    }

    @Test
    fun fixDurationWithDayAndTSuffix() {
        // P3DT should be P3D (trailing T is invalid)
        val input = "TRIGGER:-P3DT"
        val result = FixInvalidDayOffsetPreprocessor.fixString(input)
        assertEquals("TRIGGER:-P3D", result)
    }

    @Test
    fun fixNegativeDurationWithTBeforeDay() {
        val input = "DURATION:PT-5D"
        val result = FixInvalidDayOffsetPreprocessor.fixString(input)
        assertEquals("DURATION:P-5D", result)
    }

    @Test
    fun fixTriggerWithTBeforeDay() {
        val input = "TRIGGER:-PT7D"
        val result = FixInvalidDayOffsetPreprocessor.fixString(input)
        assertEquals("TRIGGER:-P7D", result)
    }

    @Test
    fun validDurationLeftAlone() {
        val input = "DURATION:P1D"
        val result = FixInvalidDayOffsetPreprocessor.fixString(input)
        assertEquals("DURATION:P1D", result)
    }

    @Test
    fun validTimeDurationLeftAlone() {
        val input = "DURATION:PT1H30M"
        val result = FixInvalidDayOffsetPreprocessor.fixString(input)
        assertEquals("DURATION:PT1H30M", result)
    }

    @Test
    fun regexpForDayOffsetNotNull() {
        assertNotNull(FixInvalidDayOffsetPreprocessor.regexpForProblem())
    }

    // ---- ICalPreprocessor.preprocessStream ----

    @Test
    fun preprocessStreamFixesBadUtcOffset() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTIMEZONE
            TZID:Custom
            BEGIN:STANDARD
            DTSTART:19700101T000000
            TZOFFSETFROM:+1800
            TZOFFSETTO:+0100
            END:STANDARD
            END:VTIMEZONE
            BEGIN:VTODO
            UID:test-1
            SUMMARY:Test
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val result = ICalPreprocessor.preprocessStream(StringReader(ics)).readText()
        assertTrue(result.contains("TZOFFSETFROM:+001800"))
    }

    @Test
    fun preprocessStreamPassesThroughCleanIcs() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            UID:test-1
            SUMMARY:Clean Task
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val result = ICalPreprocessor.preprocessStream(StringReader(ics)).readText()
        assertTrue(result.contains("UID:test-1"))
        assertTrue(result.contains("SUMMARY:Clean Task"))
    }

    // ---- Integration: parsing ICS with preprocessor ----

    @Test
    fun parseVTodoThroughPreprocessor() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            UID:preprocessed-uid
            SUMMARY:Preprocessed Task
            PRIORITY:5
            STATUS:NEEDS-ACTION
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals(1, tasks.size)
        assertEquals("preprocessed-uid", tasks[0].uid)
        assertEquals("Preprocessed Task", tasks[0].summary)
        assertEquals(5, tasks[0].priority)
    }

    @Test
    fun parseVTodoWithCompletedStatus() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            UID:completed-uid
            SUMMARY:Done Task
            STATUS:COMPLETED
            COMPLETED:20240101T120000Z
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals(1, tasks.size)
        assertEquals(net.fortuna.ical4j.model.property.Status.VTODO_COMPLETED, tasks[0].status)
        assertNotNull(tasks[0].completedAt)
    }

    @Test
    fun parseVTodoWithDescription() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            UID:desc-uid
            SUMMARY:Task With Desc
            DESCRIPTION:This is the description
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals(1, tasks.size)
        assertEquals("This is the description", tasks[0].description)
    }

    @Test
    fun parseVTodoWithRecurrence() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            UID:rrule-uid
            SUMMARY:Recurring Task
            RRULE:FREQ=WEEKLY;BYDAY=MO
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals(1, tasks.size)
        assertNotNull(tasks[0].rRule)
    }

    @Test
    fun parseMultipleVTodosExtractsAll() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            UID:uid-a
            SUMMARY:First
            END:VTODO
            BEGIN:VTODO
            UID:uid-b
            SUMMARY:Second
            END:VTODO
            BEGIN:VTODO
            UID:uid-c
            SUMMARY:Third
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals(3, tasks.size)
    }

    @Test
    fun parseEmptyCalendarReturnsNoTasks() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals(0, tasks.size)
    }

    @Test
    fun parseVTodoWithoutUidGeneratesOne() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            SUMMARY:No UID Task
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals(1, tasks.size)
        assertNotNull(tasks[0].uid)
        assertTrue(tasks[0].uid!!.isNotEmpty())
    }
}
