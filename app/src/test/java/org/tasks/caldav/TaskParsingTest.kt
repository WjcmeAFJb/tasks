package org.tasks.caldav

import net.fortuna.ical4j.model.property.Completed
import net.fortuna.ical4j.model.property.DtStart
import net.fortuna.ical4j.model.property.Due
import net.fortuna.ical4j.model.property.Priority
import net.fortuna.ical4j.model.property.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.TestUtilities.icalendarFromFile
import org.tasks.caldav.Task.Companion.tasksFromReader
import java.io.ByteArrayOutputStream
import java.io.StringReader

class TaskParsingTest {

    // ---- tasksFromReader: basic parsing ----

    @Test
    fun parseMinimalVTodo() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            UID:test-uid-123
            SUMMARY:My Task
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals(1, tasks.size)
        assertEquals("test-uid-123", tasks[0].uid)
        assertEquals("My Task", tasks[0].summary)
    }

    @Test
    fun parseMultipleVTodos() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            UID:uid-1
            SUMMARY:Task One
            END:VTODO
            BEGIN:VTODO
            UID:uid-2
            SUMMARY:Task Two
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals(2, tasks.size)
        assertEquals("uid-1", tasks[0].uid)
        assertEquals("uid-2", tasks[1].uid)
    }

    @Test
    fun parseEmptyCalendarReturnsEmptyList() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertTrue(tasks.isEmpty())
    }

    @Test
    fun vtodoWithoutUidGeneratesOne() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            SUMMARY:No UID task
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals(1, tasks.size)
        assertNotNull(tasks[0].uid)
        assertTrue(tasks[0].uid!!.isNotEmpty())
    }

    // ---- Property parsing ----

    @Test
    fun parseSummary() {
        val task = icalendarFromFile("thunderbird/basic_no_due_date.txt")
        assertEquals("Test title", task.summary)
    }

    @Test
    fun parseDescription() {
        val task = icalendarFromFile("thunderbird/basic_no_due_date.txt")
        assertEquals("Test description", task.description)
    }

    @Test
    fun parseUid() {
        val task = icalendarFromFile("thunderbird/basic_no_due_date.txt")
        assertEquals("75deaea8-931b-b748-9a41-a7e8491c9aa9", task.uid)
    }

    @Test
    fun parseCreatedAt() {
        val task = icalendarFromFile("thunderbird/basic_no_due_date.txt")
        assertNotNull(task.createdAt)
    }

    @Test
    fun parseLastModified() {
        val task = icalendarFromFile("thunderbird/basic_no_due_date.txt")
        assertNotNull(task.lastModified)
    }

    @Test
    fun parseDueDate() {
        val task = icalendarFromFile("thunderbird/basic_due_date.txt")
        assertNotNull(task.due)
    }

    @Test
    fun parseCompletedStatus() {
        val task = icalendarFromFile("thunderbird/basic_completed.txt")
        assertEquals(Status.VTODO_COMPLETED, task.status)
    }

    @Test
    fun parseCompletedDate() {
        val task = icalendarFromFile("thunderbird/basic_completed.txt")
        assertNotNull(task.completedAt)
    }

    @Test
    fun parsePercentComplete() {
        val task = icalendarFromFile("thunderbird/basic_completed.txt")
        assertEquals(100, task.percentComplete)
    }

    @Test
    fun parseSequence() {
        val task = icalendarFromFile("thunderbird/basic_completed.txt")
        assertEquals(1, task.sequence)
    }

    @Test
    fun defaultSequenceIsZero() {
        val task = icalendarFromFile("thunderbird/basic_no_due_date.txt")
        assertEquals(0, task.sequence)
    }

    @Test
    fun parsePriorityHigh() {
        val task = icalendarFromFile("thunderbird/priority_high.txt")
        assertEquals(1, task.priority)
    }

    @Test
    fun parsePriorityUndefined() {
        val task = icalendarFromFile("thunderbird/basic_no_due_date.txt")
        assertEquals(Priority.UNDEFINED.level, task.priority)
    }

    @Test
    fun parseRecurrenceRule() {
        val task = icalendarFromFile("thunderbird/repeat_daily.txt")
        assertNotNull(task.rRule)
        assertEquals("FREQ=DAILY", task.rRule!!.value)
    }

    @Test
    fun parseDtStart() {
        val task = icalendarFromFile("thunderbird/start_date_time.txt")
        assertNotNull(task.dtStart)
    }

    @Test
    fun parseCategories() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:cat-uid
            SUMMARY:Task with categories
            CATEGORIES:Work,Personal,Urgent
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals(3, tasks[0].categories.size)
        assertTrue(tasks[0].categories.contains("Work"))
        assertTrue(tasks[0].categories.contains("Personal"))
        assertTrue(tasks[0].categories.contains("Urgent"))
    }

    @Test
    fun parseLocation() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:loc-uid
            SUMMARY:Task with location
            LOCATION:Conference Room B
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals("Conference Room B", tasks[0].location)
    }

    @Test
    fun parseUrl() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:url-uid
            SUMMARY:Task with URL
            URL:https://example.com/task
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals("https://example.com/task", tasks[0].url)
    }

    @Test
    fun parseComment() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:comment-uid
            SUMMARY:Task with comment
            COMMENT:This is a comment
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals("This is a comment", tasks[0].comment)
    }

    @Test
    fun parseRelatedTo() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:child-uid
            SUMMARY:Child Task
            RELATED-TO:parent-uid
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals(1, tasks[0].relatedTo.size)
        assertEquals("parent-uid", tasks[0].relatedTo[0].value)
    }

    @Test
    fun parseClassification() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:class-uid
            SUMMARY:Confidential Task
            CLASS:CONFIDENTIAL
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertNotNull(tasks[0].classification)
        assertEquals("CONFIDENTIAL", tasks[0].classification!!.value)
    }

    @Test
    fun parseDuration() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:dur-uid
            SUMMARY:Task with duration
            DTSTART:20210101T100000Z
            DURATION:PT1H30M
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertNotNull(tasks[0].duration)
        assertNotNull(tasks[0].dtStart)
    }

    @Test
    fun durationWithoutDtStartIsDropped() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:dur-no-start-uid
            SUMMARY:Duration without start
            DURATION:PT1H
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertNull(tasks[0].duration)
    }

    @Test
    fun unknownPropertiesArePreserved() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:unknown-uid
            SUMMARY:Task
            X-MOZ-GENERATION:3
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertTrue(tasks[0].unknownProperties.any { it.name == "X-MOZ-GENERATION" })
    }

    // ---- DTSTART / DUE mismatch correction ----

    @Test
    fun dueBeforeDtStartDropsDtStart() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:mismatch-uid
            SUMMARY:Mismatch
            DTSTART:20210201T100000Z
            DUE:20210101T100000Z
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertNull(tasks[0].dtStart)
        assertNotNull(tasks[0].due)
    }

    @Test
    fun dtStartDateAndDueDateTimeRewritesDtStart() {
        // When DTSTART is DATE and DUE is DATE-TIME, DTSTART should be rewritten to DATE-TIME
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:datetype-uid
            SUMMARY:Date mismatch
            DTSTART;VALUE=DATE:20210101
            DUE:20210115T100000Z
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertNotNull(tasks[0].dtStart)
        assertNotNull(tasks[0].due)
        // DTSTART should have been rewritten to DATE-TIME
        assertTrue(tasks[0].dtStart!!.date is net.fortuna.ical4j.model.DateTime)
    }

    @Test
    fun dtStartDateTimeAndDueDateRewritesDue() {
        // When DTSTART is DATE-TIME and DUE is DATE, DUE should be rewritten to DATE-TIME
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:datetype2-uid
            SUMMARY:Date mismatch 2
            DTSTART:20210101T100000Z
            DUE;VALUE=DATE:20210115
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertNotNull(tasks[0].due)
        assertTrue(tasks[0].due!!.date is net.fortuna.ical4j.model.DateTime)
    }

    // ---- generateUID ----

    @Test
    fun generateUidCreatesNonNullValue() {
        val task = Task()
        assertNull(task.uid)
        task.generateUID()
        assertNotNull(task.uid)
    }

    @Test
    fun generateUidCreatesUniqueValues() {
        val task1 = Task()
        val task2 = Task()
        task1.generateUID()
        task2.generateUID()
        assertNotEquals(task1.uid, task2.uid)
    }

    @Test
    fun generateUidFormatIsUuid() {
        val task = Task()
        task.generateUID()
        // UUID format: 8-4-4-4-12 hex chars
        val uuidRegex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
        assertTrue(task.uid!!.matches(uuidRegex))
    }

    // ---- write() method ----

    @Test
    fun writeProducesValidIcs() {
        val task = Task(uid = "write-test-uid", summary = "Write Test")
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("BEGIN:VCALENDAR"))
        assertTrue(output.contains("END:VCALENDAR"))
        assertTrue(output.contains("BEGIN:VTODO"))
        assertTrue(output.contains("END:VTODO"))
        assertTrue(output.contains("VERSION:2.0"))
    }

    @Test
    fun writeIncludesUid() {
        val task = Task(uid = "my-unique-uid")
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("UID:my-unique-uid"))
    }

    @Test
    fun writeIncludesSummary() {
        val task = Task(uid = "sum-uid", summary = "My Summary")
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("SUMMARY:My Summary"))
    }

    @Test
    fun writeIncludesDescription() {
        val task = Task(uid = "desc-uid", description = "A detailed description")
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("DESCRIPTION:A detailed description"))
    }

    @Test
    fun writeIncludesLocation() {
        val task = Task(uid = "loc-uid", location = "Office")
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("LOCATION:Office"))
    }

    @Test
    fun writeOmitsUndefinedPriority() {
        val task = Task(uid = "pri-uid", priority = Priority.UNDEFINED.level)
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertFalse(output.contains("PRIORITY:"))
    }

    @Test
    fun writeIncludesNonDefaultPriority() {
        val task = Task(uid = "pri-uid", priority = 1)
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("PRIORITY:1"))
    }

    @Test
    fun writeIncludesStatus() {
        val task = Task(uid = "stat-uid", status = Status.VTODO_COMPLETED)
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("STATUS:COMPLETED"))
    }

    @Test
    fun writeOmitsSequenceZero() {
        val task = Task(uid = "seq-uid", sequence = 0)
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertFalse(output.contains("SEQUENCE:"))
    }

    @Test
    fun writeIncludesNonZeroSequence() {
        val task = Task(uid = "seq-uid", sequence = 3)
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("SEQUENCE:3"))
    }

    @Test
    fun writeIncludesPercentComplete() {
        val task = Task(uid = "pct-uid", percentComplete = 50)
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("PERCENT-COMPLETE:50"))
    }

    @Test
    fun writeOmitsNullPercentComplete() {
        val task = Task(uid = "pct-uid", percentComplete = null)
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertFalse(output.contains("PERCENT-COMPLETE:"))
    }

    @Test
    fun writeIncludesCategories() {
        val task = Task(uid = "cat-uid")
        task.categories.addAll(listOf("Work", "Urgent"))
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("CATEGORIES:"))
        assertTrue(output.contains("Work"))
        assertTrue(output.contains("Urgent"))
    }

    @Test
    fun writeOmitsEmptyCategories() {
        val task = Task(uid = "cat-uid")
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertFalse(output.contains("CATEGORIES:"))
    }

    @Test
    fun writeIncludesComment() {
        val task = Task(uid = "com-uid", comment = "A note")
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("COMMENT:A note"))
    }

    @Test
    fun writeOmitsNullComment() {
        val task = Task(uid = "com-uid", comment = null)
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertFalse(output.contains("COMMENT:"))
    }

    @Test
    fun writeIncludesUrl() {
        val task = Task(uid = "url-uid", url = "https://example.com")
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("URL:https://example.com"))
    }

    @Test
    fun writeIgnoresInvalidUrl() {
        // An invalid URL should be silently skipped
        val task = Task(uid = "bad-url-uid", url = "not a valid: url with spaces")
        val os = ByteArrayOutputStream()
        task.write(os)
        // Should not throw, should still produce output
        val output = os.toString("UTF-8")
        assertTrue(output.contains("BEGIN:VCALENDAR"))
    }

    @Test
    fun writeOmitsNullSummary() {
        val task = Task(uid = "null-sum-uid", summary = null)
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertFalse(output.contains("SUMMARY:"))
    }

    @Test
    fun writeOmitsNullDescription() {
        val task = Task(uid = "null-desc-uid", description = null)
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertFalse(output.contains("DESCRIPTION:"))
    }

    @Test
    fun writeOmitsNullLocation() {
        val task = Task(uid = "null-loc-uid", location = null)
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertFalse(output.contains("LOCATION:"))
    }

    @Test
    fun writeOmitsNullUrl() {
        val task = Task(uid = "null-url-uid", url = null)
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertFalse(output.contains("URL:"))
    }

    @Test
    fun writeIncludesProdId() {
        val task = Task(uid = "prodid-uid")
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("PRODID:"))
    }

    @Test
    fun writeIncludesDtStamp() {
        val task = Task(uid = "dtstamp-uid")
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("DTSTAMP:"))
    }

    // ---- Roundtrip: parse then write ----

    @Test
    fun roundtripPreservesUid() {
        val task = icalendarFromFile("thunderbird/basic_no_due_date.txt")
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("UID:75deaea8-931b-b748-9a41-a7e8491c9aa9"))
    }

    @Test
    fun roundtripPreservesSummary() {
        val task = icalendarFromFile("thunderbird/basic_no_due_date.txt")
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("SUMMARY:Test title"))
    }

    @Test
    fun roundtripPreservesDescription() {
        val task = icalendarFromFile("thunderbird/basic_no_due_date.txt")
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("DESCRIPTION:Test description"))
    }

    @Test
    fun roundtripPreservesCompletedStatus() {
        val task = icalendarFromFile("thunderbird/basic_completed.txt")
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("STATUS:COMPLETED"))
        assertTrue(output.contains("COMPLETED:"))
        assertTrue(output.contains("PERCENT-COMPLETE:100"))
    }

    @Test
    fun roundtripPreservesPriority() {
        val task = icalendarFromFile("thunderbird/priority_high.txt")
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("PRIORITY:1"))
    }

    @Test
    fun roundtripPreservesRecurrence() {
        val task = icalendarFromFile("thunderbird/repeat_daily.txt")
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("RRULE:FREQ=DAILY"))
    }

    @Test
    fun roundtripPreservesUnknownProperties() {
        val task = icalendarFromFile("thunderbird/basic_completed.txt")
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("X-MOZ-GENERATION:"))
    }

    // ---- data class defaults ----

    @Test
    fun defaultTaskHasNullUid() {
        val task = Task()
        assertNull(task.uid)
    }

    @Test
    fun defaultTaskHasUndefinedPriority() {
        val task = Task()
        assertEquals(Priority.UNDEFINED.level, task.priority)
    }

    @Test
    fun defaultTaskHasNullSummary() {
        val task = Task()
        assertNull(task.summary)
    }

    @Test
    fun defaultTaskHasEmptyCategories() {
        val task = Task()
        assertTrue(task.categories.isEmpty())
    }

    @Test
    fun defaultTaskHasEmptyAlarms() {
        val task = Task()
        assertTrue(task.alarms.isEmpty())
    }

    @Test
    fun defaultTaskHasEmptyRelatedTo() {
        val task = Task()
        assertTrue(task.relatedTo.isEmpty())
    }

    @Test
    fun defaultTaskHasEmptyUnknownProperties() {
        val task = Task()
        assertTrue(task.unknownProperties.isEmpty())
    }

    @Test
    fun defaultTaskHasNullDue() {
        val task = Task()
        assertNull(task.due)
    }

    @Test
    fun defaultTaskHasNullDtStart() {
        val task = Task()
        assertNull(task.dtStart)
    }

    @Test
    fun defaultTaskHasNullCompletedAt() {
        val task = Task()
        assertNull(task.completedAt)
    }

    @Test
    fun defaultTaskHasNullStatus() {
        val task = Task()
        assertNull(task.status)
    }

    @Test
    fun defaultTaskHasNullSequence() {
        val task = Task()
        assertNull(task.sequence)
    }

    // ---- Property setters ----

    @Test
    fun setAndGetSummary() {
        val task = Task()
        task.summary = "New summary"
        assertEquals("New summary", task.summary)
    }

    @Test
    fun setAndGetDescription() {
        val task = Task()
        task.description = "New description"
        assertEquals("New description", task.description)
    }

    @Test
    fun setAndGetPriority() {
        val task = Task()
        task.priority = 5
        assertEquals(5, task.priority)
    }

    @Test
    fun setAndGetStatus() {
        val task = Task()
        task.status = Status.VTODO_IN_PROCESS
        assertEquals(Status.VTODO_IN_PROCESS, task.status)
    }

    @Test
    fun setAndGetPercentComplete() {
        val task = Task()
        task.percentComplete = 75
        assertEquals(75, task.percentComplete)
    }

    @Test
    fun setAndGetUrl() {
        val task = Task()
        task.url = "https://example.com"
        assertEquals("https://example.com", task.url)
    }

    @Test
    fun setAndGetLocation() {
        val task = Task()
        task.location = "New York"
        assertEquals("New York", task.location)
    }

    @Test
    fun setAndGetComment() {
        val task = Task()
        task.comment = "A comment"
        assertEquals("A comment", task.comment)
    }

    // ---- Parsing from file resources ----

    @Test
    fun parseSynologyCompletedWithoutDueDate() {
        val task = icalendarFromFile("synology/complete_no_due_date.txt")
        assertEquals(Status.VTODO_COMPLETED, task.status)
        assertNull(task.due)
    }

    @Test
    fun parseSynologyCompletedWithDate() {
        val task = icalendarFromFile("synology/complete_with_date.txt")
        assertEquals(Status.VTODO_COMPLETED, task.status)
    }

    @Test
    fun parseNextcloudAllDayDue() {
        val task = icalendarFromFile("nextcloud/all_day_task.txt")
        assertNotNull(task.due)
        // All day tasks use Date, not DateTime
        assertFalse(task.due!!.date is net.fortuna.ical4j.model.DateTime)
    }

    @Test
    fun parseNextcloudNoPriority() {
        val task = icalendarFromFile("nextcloud/priority_no_stars.txt")
        assertEquals(0, task.priority)
    }

    @Test
    fun parseNextcloudHighPriority() {
        val task = icalendarFromFile("nextcloud/priority_9_stars.txt")
        // Priority 1 = highest in iCalendar spec
        assertEquals(1, task.priority)
    }

    // ---- Edge cases ----

    @Test
    fun parseEmptyDescription() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:empty-desc-uid
            SUMMARY:Task
            DESCRIPTION:
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        // ical4j may treat empty DESCRIPTION as empty string or null
        val desc = tasks[0].description
        assertTrue(desc == null || desc.isEmpty())
    }

    @Test
    fun parseEmptySummary() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:empty-sum-uid
            SUMMARY:
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        val summary = tasks[0].summary
        assertTrue(summary == null || summary.isEmpty())
    }

    @Test
    fun parseMultipleCategories() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:multi-cat-uid
            SUMMARY:Task
            CATEGORIES:Work
            CATEGORIES:Home
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals(2, tasks[0].categories.size)
        assertTrue(tasks[0].categories.contains("Work"))
        assertTrue(tasks[0].categories.contains("Home"))
    }

    @Test
    fun parseMultipleRelatedTo() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:multi-rel-uid
            SUMMARY:Task
            RELATED-TO:parent-1
            RELATED-TO:parent-2
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals(2, tasks[0].relatedTo.size)
    }

    @Test
    fun writeWithDueDate() {
        val task = Task(uid = "due-write-uid")
        task.due = Due(net.fortuna.ical4j.model.DateTime("20210115T100000Z"))
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("DUE:"))
    }

    @Test
    fun writeWithDtStart() {
        val task = Task(uid = "start-write-uid")
        task.dtStart = DtStart(net.fortuna.ical4j.model.DateTime("20210101T090000Z"))
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("DTSTART:"))
    }

    @Test
    fun writeWithCompletedAt() {
        val task = Task(uid = "comp-write-uid")
        task.completedAt = Completed(net.fortuna.ical4j.model.DateTime("20210201T120000Z"))
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("COMPLETED:"))
    }

    @Test
    fun writeWithCreatedAt() {
        val task = Task(uid = "created-uid", createdAt = 1609459200000L)
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("CREATED:"))
    }

    @Test
    fun writeWithLastModified() {
        val task = Task(uid = "lastmod-uid", lastModified = 1609459200000L)
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("LAST-MODIFIED:"))
    }

    @Test
    fun writeWithRecurrenceRule() {
        val task = Task(uid = "rrule-uid")
        task.rRule = net.fortuna.ical4j.model.property.RRule("FREQ=WEEKLY;BYDAY=MO")
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("RRULE:FREQ=WEEKLY"))
    }

    @Test
    fun writeWithRelatedTo() {
        val task = Task(uid = "rel-write-uid")
        task.relatedTo.add(net.fortuna.ical4j.model.property.RelatedTo("parent-uid-123"))
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("RELATED-TO:parent-uid-123"))
    }

    @Test
    fun writeWithClassification() {
        val task = Task(uid = "class-write-uid", classification = net.fortuna.ical4j.model.property.Clazz.PRIVATE)
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("CLASS:PRIVATE"))
    }

    @Test
    fun writeMinimalTaskWithNoUid() {
        val task = Task()
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("BEGIN:VCALENDAR"))
        // UID should not be present since it's null
        assertFalse(output.contains("UID:"))
    }

    // ---- Full roundtrip: write then parse ----

    @Test
    fun fullRoundtripPreservesAllFields() {
        val original = Task(
            uid = "roundtrip-uid-full",
            summary = "Roundtrip Task",
            description = "Roundtrip description",
            location = "Roundtrip location",
            priority = 3,
            status = Status.VTODO_IN_PROCESS,
            percentComplete = 50,
            comment = "Roundtrip comment",
            sequence = 2,
            url = "https://roundtrip.example.com",
        )
        original.categories.addAll(listOf("Cat1", "Cat2"))

        val os = ByteArrayOutputStream()
        original.write(os)
        val icsContent = os.toString("UTF-8")

        val parsed = tasksFromReader(StringReader(icsContent))
        assertEquals(1, parsed.size)
        val result = parsed[0]

        assertEquals("roundtrip-uid-full", result.uid)
        assertEquals("Roundtrip Task", result.summary)
        assertEquals("Roundtrip description", result.description)
        assertEquals("Roundtrip location", result.location)
        assertEquals(3, result.priority)
        assertEquals(Status.VTODO_IN_PROCESS, result.status)
        assertEquals(50, result.percentComplete)
        assertEquals("Roundtrip comment", result.comment)
        assertEquals(2, result.sequence)
        assertEquals("https://roundtrip.example.com", result.url)
        assertEquals(2, result.categories.size)
        assertTrue(result.categories.contains("Cat1"))
        assertTrue(result.categories.contains("Cat2"))
    }

    // ---- minifyVTimeZone ----

    @Test
    fun minifyVTimeZoneWithNullStartReturnsOriginal() {
        val registry = net.fortuna.ical4j.model.TimeZoneRegistryFactory.getInstance().createRegistry()
        val tz = registry.getTimeZone("America/New_York")
        val original = tz.vTimeZone
        val result = Task.minifyVTimeZone(original, null)
        assertEquals(original, result)
    }

    @Test
    fun minifyVTimeZoneReturnsValidTimezone() {
        val registry = net.fortuna.ical4j.model.TimeZoneRegistryFactory.getInstance().createRegistry()
        val tz = registry.getTimeZone("America/New_York")
        val original = tz.vTimeZone
        val start = net.fortuna.ical4j.model.DateTime("20210601T000000Z")
        val result = Task.minifyVTimeZone(original, start)
        assertNotNull(result)
        assertNotNull(result.timeZoneId)
    }

    @Test
    fun minifyVTimeZonePreservesTimeZoneId() {
        val registry = net.fortuna.ical4j.model.TimeZoneRegistryFactory.getInstance().createRegistry()
        val tz = registry.getTimeZone("Europe/Berlin")
        val original = tz.vTimeZone
        val start = net.fortuna.ical4j.model.DateTime("20210601T000000Z")
        val result = Task.minifyVTimeZone(original, start)
        assertEquals(original.timeZoneId.value, result.timeZoneId.value)
    }

    @Test
    fun minifyVTimeZoneReducesObservances() {
        val registry = net.fortuna.ical4j.model.TimeZoneRegistryFactory.getInstance().createRegistry()
        val tz = registry.getTimeZone("America/New_York")
        val original = tz.vTimeZone
        val start = net.fortuna.ical4j.model.DateTime("20210601T000000Z")
        val minified = Task.minifyVTimeZone(original, start)
        // Minified should have no more observances than original
        assertTrue(minified.observances.size <= original.observances.size)
    }

    // ---- ExDate/RDate parsing ----

    @Test
    fun parseExDate() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:exdate-uid
            SUMMARY:Task with exdate
            DTSTART:20210101T100000Z
            RRULE:FREQ=DAILY
            EXDATE:20210105T100000Z
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals(1, tasks[0].exDates.size)
    }

    @Test
    fun parseRDate() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:rdate-uid
            SUMMARY:Task with rdate
            DTSTART:20210101T100000Z
            RDATE:20210110T100000Z
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals(1, tasks[0].rDates.size)
    }

    // ---- data class behavior ----

    @Test
    fun dataClassEquality() {
        val task1 = Task(uid = "eq-uid", summary = "Same", priority = 5)
        val task2 = Task(uid = "eq-uid", summary = "Same", priority = 5)
        assertEquals(task1, task2)
    }

    @Test
    fun dataClassCopy() {
        val original = Task(uid = "copy-uid", summary = "Original")
        val copy = original.copy(summary = "Modified")
        assertEquals("copy-uid", copy.uid)
        assertEquals("Modified", copy.summary)
        assertEquals("Original", original.summary)
    }

    // ---- Alarm parsing ----

    @Test
    fun parseAlarms() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:alarm-uid
            SUMMARY:Task with alarm
            BEGIN:VALARM
            ACTION:DISPLAY
            DESCRIPTION:Reminder
            TRIGGER:-PT15M
            END:VALARM
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals(1, tasks[0].alarms.size)
    }

    @Test
    fun parseMultipleAlarms() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:multi-alarm-uid
            SUMMARY:Task with multiple alarms
            BEGIN:VALARM
            ACTION:DISPLAY
            DESCRIPTION:Reminder 1
            TRIGGER:-PT15M
            END:VALARM
            BEGIN:VALARM
            ACTION:DISPLAY
            DESCRIPTION:Reminder 2
            TRIGGER:-PT30M
            END:VALARM
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals(2, tasks[0].alarms.size)
    }

    // ---- write with alarms ----

    @Test
    fun writeWithAlarms() {
        val task = Task(uid = "alarm-write-uid")
        val alarm = net.fortuna.ical4j.model.component.VAlarm(java.time.Duration.ofMinutes(-15))
        alarm.properties.add(net.fortuna.ical4j.model.property.Action.DISPLAY)
        alarm.properties.add(net.fortuna.ical4j.model.property.Description("Reminder"))
        task.alarms.add(alarm)
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("BEGIN:VALARM"))
        assertTrue(output.contains("END:VALARM"))
    }

    // ---- Priority levels ----

    @Test
    fun parsePriority1() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:p1-uid
            PRIORITY:1
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals(1, tasks[0].priority)
    }

    @Test
    fun parsePriority5() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:p5-uid
            PRIORITY:5
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals(5, tasks[0].priority)
    }

    @Test
    fun parsePriority9() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:p9-uid
            PRIORITY:9
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals(9, tasks[0].priority)
    }

    // ---- write with timezone ----

    @Test
    fun writeWithTimezonedDueIncludesVTimezone() {
        val registry = net.fortuna.ical4j.model.TimeZoneRegistryFactory.getInstance().createRegistry()
        val tz = registry.getTimeZone("America/New_York")
        val due = Due("20210115T100000", tz)
        val task = Task(uid = "tz-due-uid", due = due)
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("BEGIN:VTIMEZONE"))
        assertTrue(output.contains("TZID:America/New_York"))
    }

    @Test
    fun writeWithUtcDueOmitsVTimezone() {
        val task = Task(uid = "utc-due-uid")
        task.due = Due(net.fortuna.ical4j.model.DateTime("20210115T100000Z"))
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertFalse(output.contains("BEGIN:VTIMEZONE"))
    }

    // ---- Organizer ----

    @Test
    fun parseOrganizer() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:org-uid
            SUMMARY:Task with organizer
            ORGANIZER:mailto:user@example.com
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertNotNull(tasks[0].organizer)
    }

    @Test
    fun writeWithOrganizer() {
        val task = Task(
            uid = "org-write-uid",
            organizer = net.fortuna.ical4j.model.property.Organizer("mailto:user@example.com")
        )
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("ORGANIZER:mailto:user@example.com"))
    }

    // ---- Geo position ----

    @Test
    fun parseGeo() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:geo-uid
            SUMMARY:Task with geo
            GEO:40.712776;-74.005974
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertNotNull(tasks[0].geoPosition)
    }

    @Test
    fun writeWithGeoPosition() {
        val geo = net.fortuna.ical4j.model.property.Geo(
            java.math.BigDecimal("40.712776"),
            java.math.BigDecimal("-74.005974")
        )
        val task = Task(uid = "geo-write-uid", geoPosition = geo)
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("GEO:"))
    }

    // ---- Status values ----

    @Test
    fun parseStatusNeedsAction() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:na-uid
            STATUS:NEEDS-ACTION
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals(Status.VTODO_NEEDS_ACTION, tasks[0].status)
    }

    @Test
    fun parseStatusInProcess() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:ip-uid
            STATUS:IN-PROCESS
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals(Status.VTODO_IN_PROCESS, tasks[0].status)
    }

    @Test
    fun parseStatusCancelled() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:c-uid
            STATUS:CANCELLED
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        assertEquals(Status.VTODO_CANCELLED, tasks[0].status)
    }

    // ---- Special character handling ----

    @Test
    fun parseSummaryWithSpecialCharacters() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//EN
            BEGIN:VTODO
            UID:special-uid
            SUMMARY:Task with \, comma and \; semicolon
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = tasksFromReader(StringReader(ics))
        // ical4j should unescape the special characters
        assertTrue(tasks[0].summary!!.contains(","))
        assertTrue(tasks[0].summary!!.contains(";"))
    }

    @Test
    fun writeAndParseSpecialCharactersInSummary() {
        val task = Task(uid = "special-write-uid", summary = "Meeting, Review & Plan; Next steps")
        val os = ByteArrayOutputStream()
        task.write(os)
        val icsContent = os.toString("UTF-8")
        val parsed = tasksFromReader(StringReader(icsContent))
        assertEquals("Meeting, Review & Plan; Next steps", parsed[0].summary)
    }

    @Test
    fun writeAndParseMultilineDescription() {
        val task = Task(uid = "multiline-uid", description = "Line 1\nLine 2\nLine 3")
        val os = ByteArrayOutputStream()
        task.write(os)
        val icsContent = os.toString("UTF-8")
        val parsed = tasksFromReader(StringReader(icsContent))
        assertEquals("Line 1\nLine 2\nLine 3", parsed[0].description)
    }
}
