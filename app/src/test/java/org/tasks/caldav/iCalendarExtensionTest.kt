@file:Suppress("ClassName")

package org.tasks.caldav

import net.fortuna.ical4j.model.Date
import net.fortuna.ical4j.model.DateTime
import net.fortuna.ical4j.model.property.DtStart
import net.fortuna.ical4j.model.property.Due
import net.fortuna.ical4j.model.property.RelatedTo
import net.fortuna.ical4j.model.property.XProperty
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.tasks.TestUtilities.icalendarFromFile
import org.tasks.TestUtilities.readFile
import org.tasks.caldav.iCalendar.Companion.collapsed
import org.tasks.caldav.iCalendar.Companion.lastAck
import org.tasks.caldav.iCalendar.Companion.order
import org.tasks.caldav.iCalendar.Companion.parent
import org.tasks.caldav.iCalendar.Companion.prodId
import org.tasks.caldav.iCalendar.Companion.snooze
import org.tasks.caldav.iCalendar.Companion.supportsReminders
import org.tasks.caldav.iCalendar.Companion.toMillis
import org.tasks.data.entity.Task.Companion.URGENCY_SPECIFIC_DAY
import org.tasks.data.entity.Task.Companion.URGENCY_SPECIFIC_DAY_TIME
import org.tasks.data.createDueDate
import java.util.TimeZone

class iCalendarExtensionTest {
    private val defaultTimeZone = TimeZone.getDefault()

    @Before
    fun before() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"))
    }

    @After
    fun after() {
        TimeZone.setDefault(defaultTimeZone)
    }

    // ---- supportsReminders() ----

    @Test
    fun tasksOrgSupportsReminders() {
        assertTrue("-//tasks.org//android//EN".supportsReminders())
    }

    @Test
    fun mozillaSupportsReminders() {
        assertTrue("-//Mozilla.org/NONSGML Mozilla Calendar V1.1//EN".supportsReminders())
    }

    @Test
    fun appleSupportsReminders() {
        assertTrue("-//Apple Inc.//Mac OS X 10.13.4//EN".supportsReminders())
    }

    @Test
    fun nextcloudDoesNotSupportReminders() {
        assertFalse("-//Nextcloud Tasks v0.9.5".supportsReminders())
    }

    @Test
    fun unknownClientDoesNotSupportReminders() {
        assertFalse("-//SomeUnknownClient//v1.0//EN".supportsReminders())
    }

    @Test
    fun emptyStringDoesNotSupportReminders() {
        assertFalse("".supportsReminders())
    }

    @Test
    fun supportsRemindersIsCaseSensitive() {
        assertFalse("-//TASKS.ORG//EN".supportsReminders())
    }

    @Test
    fun supportsRemindersFromThunderbirdFile() {
        assertTrue(readFile("thunderbird/basic_due_date.txt").supportsReminders())
    }

    @Test
    fun supportsRemindersFromNextcloudFile() {
        assertFalse(readFile("nextcloud/basic_due_date.txt").supportsReminders())
    }

    // ---- prodId() ----

    @Test
    fun parseMozillaProdId() {
        assertEquals(
            "-//Mozilla.org/NONSGML Mozilla Calendar V1.1//EN",
            readFile("thunderbird/basic_due_date.txt").prodId()
        )
    }

    @Test
    fun parseNextcloudProdId() {
        assertEquals(
            "-//Nextcloud Tasks v0.9.5",
            readFile("nextcloud/basic_due_date.txt").prodId()
        )
    }

    @Test
    fun parseAppleProdId() {
        assertEquals(
            "-//Apple Inc.//Mac OS X 10.13.4//EN",
            readFile("apple/basic_due_date.txt").prodId()
        )
    }

    @Test
    fun prodIdReturnsNullForNoProdId() {
        val noProdId = "BEGIN:VCALENDAR\nVERSION:2.0\nEND:VCALENDAR\n"
        assertNull(noProdId.prodId())
    }

    @Test
    fun prodIdFromInlineString() {
        val ics = "BEGIN:VCALENDAR\nPRODID:-//tasks.org//android//EN\nVERSION:2.0\nEND:VCALENDAR\n"
        assertEquals("-//tasks.org//android//EN", ics.prodId())
    }

    // ---- Due.toMillis() ----

    @Test
    fun nullDueReturnsZero() {
        val due: Due? = null
        assertEquals(0L, due.toMillis())
    }

    @Test
    fun allDayDueReturnsSpecificDay() {
        // DATE-only (all day): 20210201
        val due = Due(Date("20210201"))
        val millis = due.toMillis()
        assertTrue("All-day due should produce nonzero millis", millis > 0)
        // Verify it uses URGENCY_SPECIFIC_DAY format
        val expected = createDueDate(
            URGENCY_SPECIFIC_DAY,
            iCalendar.getLocal(due)
        )
        assertEquals(expected, millis)
    }

    @Test
    fun dateTimeDueReturnsSpecificDayTime() {
        // DATE-TIME: 20180417T190000Z
        val due = Due(DateTime("20180417T190000Z"))
        val millis = due.toMillis()
        assertTrue("DateTime due should produce nonzero millis", millis > 0)
        val expected = createDueDate(
            URGENCY_SPECIFIC_DAY_TIME,
            iCalendar.getLocal(due)
        )
        assertEquals(expected, millis)
    }

    @Test
    fun allDayDueFromFile() {
        val remote = icalendarFromFile("nextcloud/all_day_task.txt")
        val millis = remote.due.toMillis()
        assertTrue("Due from all-day file should be nonzero", millis > 0)
    }

    @Test
    fun dateTimeDueFromFile() {
        val remote = icalendarFromFile("thunderbird/basic_due_date.txt")
        val millis = remote.due.toMillis()
        assertTrue("Due from datetime file should be nonzero", millis > 0)
        // Thunderbird due is 20180417T140000 America/Chicago
        val expected = createDueDate(
            URGENCY_SPECIFIC_DAY_TIME,
            iCalendar.getLocal(remote.due!!)
        )
        assertEquals(expected, millis)
    }

    // ---- DtStart.toMillis() ----

    @Test
    fun nullDtStartReturnsZero() {
        val dtStart: DtStart? = null
        val task = org.tasks.data.entity.Task()
        assertEquals(0L, dtStart.toMillis(task))
    }

    @Test
    fun dtStartDateTimeFromFile() {
        val remote = icalendarFromFile("apple/basic_due_date.txt")
        val task = org.tasks.data.entity.Task()
        val millis = remote.dtStart.toMillis(task)
        assertTrue("DtStart from apple file should be nonzero", millis > 0)
    }

    // ---- Due.apply() ----

    @Test
    fun applyNullDueClearsDueDate() {
        val task = org.tasks.data.entity.Task(dueDate = 12345L)
        val due: Due? = null
        with(iCalendar.Companion) {
            due.apply(task)
        }
        assertEquals(0L, task.dueDate)
    }

    @Test
    fun applyAllDayDueSetsDate() {
        val task = org.tasks.data.entity.Task()
        val due = Due(Date("20210201"))
        with(iCalendar.Companion) {
            due.apply(task)
        }
        assertTrue("Task dueDate should be set", task.dueDate > 0)
    }

    @Test
    fun applyDateTimeDueSetsDateTime() {
        val task = org.tasks.data.entity.Task()
        val due = Due(DateTime("20180417T190000Z"))
        with(iCalendar.Companion) {
            due.apply(task)
        }
        assertTrue("Task dueDate should be set", task.dueDate > 0)
    }

    // ---- VTodoTask.parent ----

    @Test
    fun parentDefaultsToNull() {
        val task = Task()
        assertNull(task.parent)
    }

    @Test
    fun setAndGetParent() {
        val task = Task()
        task.parent = "parent-uid-123"
        assertEquals("parent-uid-123", task.parent)
    }

    @Test
    fun clearParent() {
        val task = Task()
        task.parent = "parent-uid-123"
        task.parent = null
        assertNull(task.parent)
    }

    @Test
    fun clearParentWithBlankString() {
        val task = Task()
        task.parent = "parent-uid-123"
        task.parent = ""
        assertNull(task.parent)
    }

    @Test
    fun overwriteParent() {
        val task = Task()
        task.parent = "parent-uid-1"
        task.parent = "parent-uid-2"
        assertEquals("parent-uid-2", task.parent)
        // Should have exactly one RELATED-TO
        assertEquals(1, task.relatedTo.size)
    }

    @Test
    fun parentFromFile() {
        val ics = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//Test//EN
            BEGIN:VTODO
            UID:child-uid
            SUMMARY:Child Task
            RELATED-TO:parent-uid-abc
            END:VTODO
            END:VCALENDAR
        """.trimIndent()
        val tasks = Task.tasksFromReader(java.io.StringReader(ics))
        assertEquals(1, tasks.size)
        assertEquals("parent-uid-abc", tasks[0].parent)
    }

    // ---- VTodoTask.order ----

    @Test
    fun orderDefaultsToNull() {
        val task = Task()
        assertNull(task.order)
    }

    @Test
    fun setAndGetOrder() {
        val task = Task()
        task.order = 42L
        assertEquals(42L, task.order)
    }

    @Test
    fun clearOrder() {
        val task = Task()
        task.order = 42L
        task.order = null
        assertNull(task.order)
    }

    @Test
    fun overwriteOrder() {
        val task = Task()
        task.order = 10L
        task.order = 20L
        assertEquals(20L, task.order)
    }

    @Test
    fun orderFromXProperty() {
        val task = Task()
        task.unknownProperties.add(XProperty("X-APPLE-SORT-ORDER", "99"))
        assertEquals(99L, task.order)
    }

    // ---- VTodoTask.collapsed ----

    @Test
    fun collapsedDefaultsToFalse() {
        val task = Task()
        assertFalse(task.collapsed)
    }

    @Test
    fun setCollapsed() {
        val task = Task()
        task.collapsed = true
        assertTrue(task.collapsed)
    }

    @Test
    fun unsetCollapsed() {
        val task = Task()
        task.collapsed = true
        task.collapsed = false
        assertFalse(task.collapsed)
    }

    @Test
    fun collapsedFromXProperty() {
        val task = Task()
        task.unknownProperties.add(XProperty("X-OC-HIDESUBTASKS", "1"))
        assertTrue(task.collapsed)
    }

    @Test
    fun notCollapsedWhenValueIsZero() {
        val task = Task()
        task.unknownProperties.add(XProperty("X-OC-HIDESUBTASKS", "0"))
        assertFalse(task.collapsed)
    }

    // ---- VTodoTask.lastAck ----

    @Test
    fun lastAckDefaultsToNull() {
        val task = Task()
        assertNull(task.lastAck)
    }

    @Test
    fun setAndGetLastAck() {
        val task = Task()
        val timestamp = org.tasks.time.DateTime(2021, 6, 15, 10, 30, 0).millis
        task.lastAck = timestamp
        val result = task.lastAck
        // Round-trip through UTC conversion may shift slightly, but should be within a second
        assertTrue(
            "lastAck round-trip should be close to original",
            result != null && kotlin.math.abs(result - timestamp) < 2000
        )
    }

    @Test
    fun lastAckFromXProperty() {
        val task = Task()
        task.unknownProperties.add(XProperty("X-MOZ-LASTACK", "20210615T153000Z"))
        val result = task.lastAck
        assertTrue("lastAck from X-MOZ-LASTACK should be nonzero", result != null && result > 0)
    }

    // ---- VTodoTask.snooze ----

    @Test
    fun snoozeDefaultsToNull() {
        val task = Task()
        assertNull(task.snooze)
    }

    @Test
    fun snoozeFromXProperty() {
        val task = Task()
        // Use a far-future date so it is "after now"
        task.unknownProperties.add(XProperty("X-MOZ-SNOOZE-TIME", "20990101T120000Z"))
        val result = task.snooze
        assertTrue("snooze from X-MOZ-SNOOZE-TIME should be nonzero", result != null && result > 0)
    }

    @Test
    fun settingSnoozeToPastClearsIt() {
        val task = Task()
        task.unknownProperties.add(XProperty("X-MOZ-SNOOZE-TIME", "20990101T120000Z"))
        // Setting snooze to a past time should remove the property
        task.snooze = org.tasks.time.DateTime(2000, 1, 1, 0, 0, 0).millis
        assertNull(task.snooze)
    }

    @Test
    fun clearSnoozeWithNull() {
        val task = Task()
        task.unknownProperties.add(XProperty("X-MOZ-SNOOZE-TIME", "20990101T120000Z"))
        task.snooze = null
        assertNull(task.snooze)
    }

    // ---- DtStart.apply() ----

    @Test
    fun applyNullDtStartClearsHideUntil() {
        val task = org.tasks.data.entity.Task(hideUntil = 12345L)
        val dtStart: DtStart? = null
        with(iCalendar.Companion) {
            dtStart.apply(task)
        }
        assertEquals(0L, task.hideUntil)
    }

    // ---- Additional parent edge cases ----

    @Test
    fun parentIgnoresNonParentRelType() {
        val task = Task()
        val related = RelatedTo("sibling-uid")
        related.parameters.add(net.fortuna.ical4j.model.parameter.RelType.SIBLING)
        task.relatedTo.add(related)
        // Non-parent RELTYPE should not be returned
        assertNull(task.parent)
    }

    @Test
    fun parentWithMultipleRelatedToKeepsFirst() {
        val task = Task()
        task.relatedTo.add(RelatedTo("parent-1"))
        task.relatedTo.add(RelatedTo("parent-2"))
        // Should find the first one
        assertEquals("parent-1", task.parent)
    }

    @Test
    fun setParentConsolidatesMultipleRelatedTo() {
        val task = Task()
        task.relatedTo.add(RelatedTo("parent-1"))
        task.relatedTo.add(RelatedTo("parent-2"))
        task.parent = "parent-3"
        assertEquals("parent-3", task.parent)
        // The setter should consolidate to one parent
        val parentRelations = task.relatedTo.filter { rel ->
            val relType = rel.parameters.getParameter<net.fortuna.ical4j.model.parameter.RelType>(
                net.fortuna.ical4j.model.Parameter.RELTYPE
            )
            relType == null || relType === net.fortuna.ical4j.model.parameter.RelType.PARENT || relType.value.isNullOrBlank()
        }
        assertEquals(1, parentRelations.size)
    }
}
