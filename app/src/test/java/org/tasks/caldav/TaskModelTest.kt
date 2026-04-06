package org.tasks.caldav

import net.fortuna.ical4j.model.property.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.StringReader

class TaskModelTest {

    // ===== Task creation and defaults =====

    @Test
    fun newTaskHasNullUid() {
        val task = Task()
        assertNull(task.uid)
    }

    @Test
    fun generateUidSetsNonNullUid() {
        val task = Task()
        task.generateUID()
        assertNotNull(task.uid)
    }

    @Test
    fun generateUidSetsNonBlankUid() {
        val task = Task()
        task.generateUID()
        assertTrue(task.uid!!.isNotBlank())
    }

    @Test
    fun generateUidSetsUniqueUids() {
        val task1 = Task()
        val task2 = Task()
        task1.generateUID()
        task2.generateUID()
        assertFalse(task1.uid == task2.uid)
    }

    @Test
    fun defaultPriorityIsUndefined() {
        val task = Task()
        assertEquals(0, task.priority) // Priority.UNDEFINED is 0
    }

    @Test
    fun defaultSequenceIsNull() {
        val task = Task()
        assertNull(task.sequence)
    }

    @Test
    fun defaultStatusIsNull() {
        val task = Task()
        assertNull(task.status)
    }

    @Test
    fun defaultCategoriesIsEmpty() {
        val task = Task()
        assertTrue(task.categories.isEmpty())
    }

    @Test
    fun defaultAlarmsIsEmpty() {
        val task = Task()
        assertTrue(task.alarms.isEmpty())
    }

    @Test
    fun defaultRelatedToIsEmpty() {
        val task = Task()
        assertTrue(task.relatedTo.isEmpty())
    }

    // ===== Task write/read round trip =====

    @Test
    fun writeProducesICalendarOutput() {
        val task = Task(uid = "test-uid-123")
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("BEGIN:VCALENDAR"))
        assertTrue(output.contains("END:VCALENDAR"))
        assertTrue(output.contains("BEGIN:VTODO"))
        assertTrue(output.contains("END:VTODO"))
    }

    @Test
    fun writeContainsUid() {
        val task = Task(uid = "my-uid-456")
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("my-uid-456"))
    }

    @Test
    fun writeContainsSummary() {
        val task = Task(uid = "uid1", summary = "My Task Title")
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("My Task Title"))
    }

    @Test
    fun writeContainsDescription() {
        val task = Task(uid = "uid1", description = "Task description here")
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("Task description here"))
    }

    @Test
    fun writeContainsPriority() {
        val task = Task(uid = "uid1", priority = 5)
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("PRIORITY:5"))
    }

    @Test
    fun writeOmitsPriorityWhenUndefined() {
        val task = Task(uid = "uid1", priority = 0)
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertFalse(output.contains("PRIORITY"))
    }

    @Test
    fun writeContainsStatus() {
        val task = Task(uid = "uid1", status = Status.VTODO_COMPLETED)
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("STATUS:COMPLETED"))
    }

    @Test
    fun writeOmitsSequenceWhenZero() {
        val task = Task(uid = "uid1", sequence = 0)
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertFalse(output.contains("SEQUENCE"))
    }

    @Test
    fun writeContainsSequenceWhenNonZero() {
        val task = Task(uid = "uid1", sequence = 3)
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("SEQUENCE:3"))
    }

    @Test
    fun writeContainsVersion20() {
        val task = Task(uid = "uid1")
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("VERSION:2.0"))
    }

    @Test
    fun writeContainsCategories() {
        val task = Task(uid = "uid1")
        task.categories.add("Work")
        task.categories.add("Important")
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("Work"))
        assertTrue(output.contains("Important"))
    }

    @Test
    fun writeContainsComment() {
        val task = Task(uid = "uid1", comment = "This is a comment")
        val os = ByteArrayOutputStream()
        task.write(os)
        val output = os.toString("UTF-8")
        assertTrue(output.contains("This is a comment"))
    }

    // ===== tasksFromReader =====

    @Test
    fun parseSimpleVtodo() {
        val vtodo = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Test//Test//EN
BEGIN:VTODO
UID:test-uid-123
SUMMARY:Test Task
END:VTODO
END:VCALENDAR"""
        val tasks = Task.tasksFromReader(StringReader(vtodo))
        assertEquals(1, tasks.size)
        assertEquals("test-uid-123", tasks[0].uid)
        assertEquals("Test Task", tasks[0].summary)
    }

    @Test
    fun parseVtodoWithPriority() {
        val vtodo = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Test//Test//EN
BEGIN:VTODO
UID:test-uid
PRIORITY:5
END:VTODO
END:VCALENDAR"""
        val tasks = Task.tasksFromReader(StringReader(vtodo))
        assertEquals(5, tasks[0].priority)
    }

    @Test
    fun parseVtodoWithStatus() {
        val vtodo = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Test//Test//EN
BEGIN:VTODO
UID:test-uid
STATUS:COMPLETED
END:VTODO
END:VCALENDAR"""
        val tasks = Task.tasksFromReader(StringReader(vtodo))
        assertEquals(Status.VTODO_COMPLETED, tasks[0].status)
    }

    @Test
    fun parseVtodoWithDescription() {
        val vtodo = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Test//Test//EN
BEGIN:VTODO
UID:test-uid
DESCRIPTION:This is the description
END:VTODO
END:VCALENDAR"""
        val tasks = Task.tasksFromReader(StringReader(vtodo))
        assertEquals("This is the description", tasks[0].description)
    }

    @Test
    fun parseVtodoWithCategories() {
        val vtodo = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Test//Test//EN
BEGIN:VTODO
UID:test-uid
CATEGORIES:Work,Personal
END:VTODO
END:VCALENDAR"""
        val tasks = Task.tasksFromReader(StringReader(vtodo))
        assertTrue(tasks[0].categories.contains("Work"))
        assertTrue(tasks[0].categories.contains("Personal"))
    }

    @Test
    fun parseVtodoWithRelatedTo() {
        val vtodo = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Test//Test//EN
BEGIN:VTODO
UID:child-uid
RELATED-TO:parent-uid
END:VTODO
END:VCALENDAR"""
        val tasks = Task.tasksFromReader(StringReader(vtodo))
        assertEquals(1, tasks[0].relatedTo.size)
        assertEquals("parent-uid", tasks[0].relatedTo[0].value)
    }

    @Test
    fun parseVtodoWithPercentComplete() {
        val vtodo = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Test//Test//EN
BEGIN:VTODO
UID:test-uid
PERCENT-COMPLETE:75
END:VTODO
END:VCALENDAR"""
        val tasks = Task.tasksFromReader(StringReader(vtodo))
        assertEquals(75, tasks[0].percentComplete)
    }

    @Test
    fun parseVtodoWithComment() {
        val vtodo = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Test//Test//EN
BEGIN:VTODO
UID:test-uid
COMMENT:My comment
END:VTODO
END:VCALENDAR"""
        val tasks = Task.tasksFromReader(StringReader(vtodo))
        assertEquals("My comment", tasks[0].comment)
    }

    @Test
    fun parseVtodoWithLocation() {
        val vtodo = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Test//Test//EN
BEGIN:VTODO
UID:test-uid
LOCATION:Office
END:VTODO
END:VCALENDAR"""
        val tasks = Task.tasksFromReader(StringReader(vtodo))
        assertEquals("Office", tasks[0].location)
    }

    @Test
    fun parseVtodoSetsSequenceToZero() {
        val vtodo = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Test//Test//EN
BEGIN:VTODO
UID:test-uid
END:VTODO
END:VCALENDAR"""
        val tasks = Task.tasksFromReader(StringReader(vtodo))
        assertEquals(0, tasks[0].sequence)
    }

    @Test
    fun parseVtodoWithExplicitSequence() {
        val vtodo = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Test//Test//EN
BEGIN:VTODO
UID:test-uid
SEQUENCE:5
END:VTODO
END:VCALENDAR"""
        val tasks = Task.tasksFromReader(StringReader(vtodo))
        assertEquals(5, tasks[0].sequence)
    }

    @Test
    fun parseVtodoWithoutUidGeneratesOne() {
        val vtodo = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Test//Test//EN
BEGIN:VTODO
SUMMARY:No UID Task
END:VTODO
END:VCALENDAR"""
        val tasks = Task.tasksFromReader(StringReader(vtodo))
        assertNotNull(tasks[0].uid)
        assertTrue(tasks[0].uid!!.isNotBlank())
    }

    @Test
    fun parseMultipleVtodos() {
        val vtodo = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Test//Test//EN
BEGIN:VTODO
UID:uid1
SUMMARY:Task 1
END:VTODO
BEGIN:VTODO
UID:uid2
SUMMARY:Task 2
END:VTODO
END:VCALENDAR"""
        val tasks = Task.tasksFromReader(StringReader(vtodo))
        assertEquals(2, tasks.size)
        assertEquals("uid1", tasks[0].uid)
        assertEquals("uid2", tasks[1].uid)
    }

    @Test
    fun parseVtodoWithInProgressStatus() {
        val vtodo = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Test//Test//EN
BEGIN:VTODO
UID:test-uid
STATUS:IN-PROCESS
END:VTODO
END:VCALENDAR"""
        val tasks = Task.tasksFromReader(StringReader(vtodo))
        assertEquals(Status.VTODO_IN_PROCESS, tasks[0].status)
    }

    @Test
    fun parseVtodoWithUrl() {
        val vtodo = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Test//Test//EN
BEGIN:VTODO
UID:test-uid
URL:https://example.com/task
END:VTODO
END:VCALENDAR"""
        val tasks = Task.tasksFromReader(StringReader(vtodo))
        assertEquals("https://example.com/task", tasks[0].url)
    }

    @Test
    fun parseVtodoWithGeo() {
        val vtodo = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Test//Test//EN
BEGIN:VTODO
UID:test-uid
GEO:37.386013;-122.082932
END:VTODO
END:VCALENDAR"""
        val tasks = Task.tasksFromReader(StringReader(vtodo))
        assertNotNull(tasks[0].geoPosition)
    }

    @Test
    fun parseVtodoWithDtStartAndDue() {
        val vtodo = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Test//Test//EN
BEGIN:VTODO
UID:test-uid
DTSTART;VALUE=DATE:20240315
DUE;VALUE=DATE:20240320
END:VTODO
END:VCALENDAR"""
        val tasks = Task.tasksFromReader(StringReader(vtodo))
        assertNotNull(tasks[0].dtStart)
        assertNotNull(tasks[0].due)
    }

    @Test
    fun parseVtodoWithDueDateBeforeDtStartDropsDtStart() {
        val vtodo = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Test//Test//EN
BEGIN:VTODO
UID:test-uid
DTSTART;VALUE=DATE:20240320
DUE;VALUE=DATE:20240315
END:VTODO
END:VCALENDAR"""
        val tasks = Task.tasksFromReader(StringReader(vtodo))
        // DUE < DTSTART so DTSTART should be dropped
        assertNull(tasks[0].dtStart)
        assertNotNull(tasks[0].due)
    }

    @Test
    fun parseVtodoWithDurationWithoutDtStartDropsDuration() {
        val vtodo = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Test//Test//EN
BEGIN:VTODO
UID:test-uid
DURATION:PT1H
END:VTODO
END:VCALENDAR"""
        val tasks = Task.tasksFromReader(StringReader(vtodo))
        // DURATION without DTSTART should be dropped
        assertNull(tasks[0].duration)
    }

    @Test
    fun parseVtodoWithDurationAndDtStart() {
        val vtodo = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Test//Test//EN
BEGIN:VTODO
UID:test-uid
DTSTART;VALUE=DATE:20240315
DURATION:P1D
END:VTODO
END:VCALENDAR"""
        val tasks = Task.tasksFromReader(StringReader(vtodo))
        assertNotNull(tasks[0].dtStart)
        assertNotNull(tasks[0].duration)
    }

    @Test
    fun parseVtodoWithCreatedAndLastModified() {
        val vtodo = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Test//Test//EN
BEGIN:VTODO
UID:test-uid
CREATED:20240315T100000Z
LAST-MODIFIED:20240315T110000Z
END:VTODO
END:VCALENDAR"""
        val tasks = Task.tasksFromReader(StringReader(vtodo))
        assertNotNull(tasks[0].createdAt)
        assertNotNull(tasks[0].lastModified)
    }

    @Test
    fun parseVtodoWithCancelledStatus() {
        val vtodo = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Test//Test//EN
BEGIN:VTODO
UID:test-uid
STATUS:CANCELLED
END:VTODO
END:VCALENDAR"""
        val tasks = Task.tasksFromReader(StringReader(vtodo))
        assertEquals(Status.VTODO_CANCELLED, tasks[0].status)
    }

    // ===== round-trip: write then parse =====

    @Test
    fun roundTripPreservesUid() {
        val original = Task(uid = "round-trip-uid")
        val os = ByteArrayOutputStream()
        original.write(os)
        val parsed = Task.tasksFromReader(StringReader(os.toString("UTF-8")))
        assertEquals(1, parsed.size)
        assertEquals("round-trip-uid", parsed[0].uid)
    }

    @Test
    fun roundTripPreservesSummary() {
        val original = Task(uid = "uid1", summary = "Round Trip Title")
        val os = ByteArrayOutputStream()
        original.write(os)
        val parsed = Task.tasksFromReader(StringReader(os.toString("UTF-8")))
        assertEquals("Round Trip Title", parsed[0].summary)
    }

    @Test
    fun roundTripPreservesPriority() {
        val original = Task(uid = "uid1", priority = 7)
        val os = ByteArrayOutputStream()
        original.write(os)
        val parsed = Task.tasksFromReader(StringReader(os.toString("UTF-8")))
        assertEquals(7, parsed[0].priority)
    }

    @Test
    fun roundTripPreservesDescription() {
        val original = Task(uid = "uid1", description = "A detailed description")
        val os = ByteArrayOutputStream()
        original.write(os)
        val parsed = Task.tasksFromReader(StringReader(os.toString("UTF-8")))
        assertEquals("A detailed description", parsed[0].description)
    }

    @Test
    fun roundTripPreservesCategories() {
        val original = Task(uid = "uid1")
        original.categories.add("Tag1")
        original.categories.add("Tag2")
        val os = ByteArrayOutputStream()
        original.write(os)
        val parsed = Task.tasksFromReader(StringReader(os.toString("UTF-8")))
        assertTrue(parsed[0].categories.contains("Tag1"))
        assertTrue(parsed[0].categories.contains("Tag2"))
    }

    @Test
    fun roundTripPreservesStatus() {
        val original = Task(uid = "uid1", status = Status.VTODO_IN_PROCESS)
        val os = ByteArrayOutputStream()
        original.write(os)
        val parsed = Task.tasksFromReader(StringReader(os.toString("UTF-8")))
        assertEquals(Status.VTODO_IN_PROCESS, parsed[0].status)
    }

    @Test
    fun roundTripPreservesPercentComplete() {
        val original = Task(uid = "uid1", percentComplete = 50)
        val os = ByteArrayOutputStream()
        original.write(os)
        val parsed = Task.tasksFromReader(StringReader(os.toString("UTF-8")))
        assertEquals(50, parsed[0].percentComplete)
    }

    // ===== Data class tests =====

    @Test
    fun taskCopy() {
        val original = Task(uid = "uid1", summary = "Original")
        val copy = original.copy(summary = "Copied")
        assertEquals("uid1", copy.uid)
        assertEquals("Copied", copy.summary)
    }

    @Test
    fun taskEquals() {
        val task1 = Task(uid = "uid1", summary = "Test")
        val task2 = Task(uid = "uid1", summary = "Test")
        assertEquals(task1, task2)
    }
}
