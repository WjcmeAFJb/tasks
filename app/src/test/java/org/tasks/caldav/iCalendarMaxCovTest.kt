@file:Suppress("ClassName")

package org.tasks.caldav

import net.fortuna.ical4j.model.property.Due
import net.fortuna.ical4j.model.property.DtStart
import net.fortuna.ical4j.model.property.RelatedTo
import net.fortuna.ical4j.model.property.Status
import net.fortuna.ical4j.model.property.XProperty
import net.fortuna.ical4j.model.DateTime as ICalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.caldav.iCalendar.Companion.IS_APPLE_SORT_ORDER
import org.tasks.caldav.iCalendar.Companion.apply
import org.tasks.caldav.iCalendar.Companion.applyLocal
import org.tasks.caldav.iCalendar.Companion.collapsed
import org.tasks.caldav.iCalendar.Companion.fromVtodo
import org.tasks.caldav.iCalendar.Companion.getDateTime
import org.tasks.caldav.iCalendar.Companion.getLocal
import org.tasks.caldav.iCalendar.Companion.order
import org.tasks.caldav.iCalendar.Companion.parent
import org.tasks.caldav.iCalendar.Companion.toMillis
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task as AppTask
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.time.startOfDay
import java.util.Date

class iCalendarMaxCovTest {

    // ===== Due.apply =====

    @Test
    fun dueApplyNullSetsZero() {
        val task = AppTask()
        val due: Due? = null
        due.apply(task)
        assertEquals(0L, task.dueDate)
    }

    @Test
    fun dueApplyWithDateTimeSetsSpecificDayTime() {
        val task = AppTask()
        val dateTime = ICalDateTime(System.currentTimeMillis())
        val due = Due(dateTime)
        due.apply(task)
        assertTrue(task.dueDate > 0)
        assertTrue(task.hasDueTime())
    }

    @Test
    fun dueApplyWithDateOnlySetsSpecificDay() {
        val task = AppTask()
        val date = Date(System.currentTimeMillis())
        val due = Due(net.fortuna.ical4j.model.Date(date))
        due.apply(task)
        assertTrue(task.dueDate > 0)
        assertFalse(task.hasDueTime())
    }

    // ===== DtStart.apply =====

    @Test
    fun dtStartApplyNullSetsZero() {
        val task = AppTask()
        val dtStart: DtStart? = null
        dtStart.apply(task)
        assertEquals(0L, task.hideUntil)
    }

    @Test
    fun dtStartApplyWithDateTimeSetsSpecificDayTime() {
        val task = AppTask()
        val dateTime = ICalDateTime(System.currentTimeMillis())
        val dtStart = DtStart(dateTime)
        dtStart.apply(task)
        assertTrue(task.hideUntil > 0)
    }

    @Test
    fun dtStartApplyWithDateOnlySetsSpecificDay() {
        val task = AppTask()
        val date = Date(System.currentTimeMillis())
        val dtStart = DtStart(net.fortuna.ical4j.model.Date(date))
        dtStart.apply(task)
        assertTrue(task.hideUntil > 0)
    }

    // ===== Due.toMillis =====

    @Test
    fun dueToMillisNull() {
        val due: Due? = null
        assertEquals(0L, due.toMillis())
    }

    @Test
    fun dueToMillisDateTime() {
        val due = Due(ICalDateTime(System.currentTimeMillis()))
        assertTrue(due.toMillis() > 0)
    }

    @Test
    fun dueToMillisDateOnly() {
        val due = Due(net.fortuna.ical4j.model.Date(System.currentTimeMillis()))
        assertTrue(due.toMillis() > 0)
    }

    // ===== DtStart.toMillis =====

    @Test
    fun dtStartToMillisNull() {
        val dtStart: DtStart? = null
        val task = AppTask()
        assertEquals(0L, dtStart.toMillis(task))
    }

    @Test
    fun dtStartToMillisDateTime() {
        val dtStart = DtStart(ICalDateTime(System.currentTimeMillis()))
        val task = AppTask()
        assertTrue(dtStart.toMillis(task) > 0)
    }

    @Test
    fun dtStartToMillisDateOnly() {
        val dtStart = DtStart(net.fortuna.ical4j.model.Date(System.currentTimeMillis()))
        val task = AppTask()
        assertTrue(dtStart.toMillis(task) > 0)
    }

    // ===== applyLocal =====

    private fun doApplyLocal(model: Task, ct: CaldavTask, task: AppTask) {
        with(iCalendar) { model.applyLocal(ct, task) }
    }

    @Test
    fun applyLocalSetsBasicFields() {
        val remoteModel = Task()
        val ct = CaldavTask(task = 1L, calendar = "cal", remoteId = "remote-123")
        val task = AppTask(id = 1L, title = "Test Task", notes = "Notes", creationDate = 1000L, modificationDate = 2000L, priority = AppTask.Priority.HIGH)
        doApplyLocal(remoteModel, ct, task)
        assertEquals("Test Task", remoteModel.summary)
        assertEquals("Notes", remoteModel.description)
    }

    @Test
    fun applyLocalCompletedTask() {
        val remoteModel = Task()
        val ct = CaldavTask(task = 1L, calendar = "cal", remoteId = "r")
        val task = AppTask(id = 1L, completionDate = System.currentTimeMillis(), modificationDate = 2000L)
        doApplyLocal(remoteModel, ct, task)
        assertNotNull(remoteModel.completedAt)
        assertEquals(Status.VTODO_COMPLETED, remoteModel.status)
        assertEquals(100, remoteModel.percentComplete!!)
    }

    @Test
    fun applyLocalIncompletedTaskClearsCompletion() {
        val remoteModel = Task()
        remoteModel.completedAt = net.fortuna.ical4j.model.property.Completed(ICalDateTime(System.currentTimeMillis()))
        remoteModel.status = Status.VTODO_COMPLETED
        remoteModel.percentComplete = 100
        val ct = CaldavTask(task = 1L, calendar = "cal", remoteId = "r")
        val task = AppTask(id = 1L, completionDate = 0L, modificationDate = 2000L)
        doApplyLocal(remoteModel, ct, task)
        assertNull(remoteModel.completedAt)
        assertNull(remoteModel.status)
        assertNull(remoteModel.percentComplete)
    }

    @Test
    fun applyLocalSetsNoDueDate() {
        val remoteModel = Task()
        val ct = CaldavTask(task = 1L, calendar = "cal", remoteId = "r")
        val task = AppTask(id = 1L, dueDate = 0L, modificationDate = 2000L)
        doApplyLocal(remoteModel, ct, task)
        assertNull(remoteModel.due)
    }

    @Test
    fun applyLocalSetsNoStartDate() {
        val remoteModel = Task()
        val ct = CaldavTask(task = 1L, calendar = "cal", remoteId = "r")
        val task = AppTask(id = 1L, hideUntil = 0L, modificationDate = 2000L)
        doApplyLocal(remoteModel, ct, task)
        assertNull(remoteModel.dtStart)
    }

    @Test
    fun applyLocalPriorityNone() {
        val remoteModel = Task()
        val ct = CaldavTask(task = 1L, calendar = "cal", remoteId = "r")
        val task = AppTask(id = 1L, priority = AppTask.Priority.NONE, modificationDate = 2000L)
        doApplyLocal(remoteModel, ct, task)
        assertEquals(0, remoteModel.priority)
    }

    @Test
    fun applyLocalPriorityMedium() {
        val remoteModel = Task()
        val ct = CaldavTask(task = 1L, calendar = "cal", remoteId = "r")
        val task = AppTask(id = 1L, priority = AppTask.Priority.MEDIUM, modificationDate = 2000L)
        doApplyLocal(remoteModel, ct, task)
        assertEquals(5, remoteModel.priority)
    }

    @Test
    fun applyLocalPriorityHighFromLow() {
        val remoteModel = Task()
        remoteModel.priority = 3
        val ct = CaldavTask(task = 1L, calendar = "cal", remoteId = "r")
        val task = AppTask(id = 1L, priority = AppTask.Priority.HIGH, modificationDate = 2000L)
        doApplyLocal(remoteModel, ct, task)
        assertEquals(3, remoteModel.priority)
    }

    @Test
    fun applyLocalPriorityHighFromAboveFive() {
        val remoteModel = Task()
        remoteModel.priority = 7
        val ct = CaldavTask(task = 1L, calendar = "cal", remoteId = "r")
        val task = AppTask(id = 1L, priority = AppTask.Priority.HIGH, modificationDate = 2000L)
        doApplyLocal(remoteModel, ct, task)
        assertEquals(1, remoteModel.priority)
    }

    @Test
    fun applyLocalPriorityLowFromAboveFive() {
        val remoteModel = Task()
        remoteModel.priority = 7
        val ct = CaldavTask(task = 1L, calendar = "cal", remoteId = "r")
        val task = AppTask(id = 1L, priority = AppTask.Priority.LOW, modificationDate = 2000L)
        doApplyLocal(remoteModel, ct, task)
        assertEquals(7, remoteModel.priority)
    }

    @Test
    fun applyLocalPriorityLowFromBelowFive() {
        val remoteModel = Task()
        remoteModel.priority = 3
        val ct = CaldavTask(task = 1L, calendar = "cal", remoteId = "r")
        val task = AppTask(id = 1L, priority = AppTask.Priority.LOW, modificationDate = 2000L)
        doApplyLocal(remoteModel, ct, task)
        assertEquals(9, remoteModel.priority)
    }

    @Test
    fun applyLocalParentWithNoParent() {
        val remoteModel = Task()
        val ct = CaldavTask(task = 1L, calendar = "cal", remoteId = "r")
        val task = AppTask(id = 1L, parent = 0L, modificationDate = 2000L)
        doApplyLocal(remoteModel, ct, task)
        assertNull(remoteModel.parent)
    }

    @Test
    fun applyLocalParentWithParent() {
        val remoteModel = Task()
        val ct = CaldavTask(task = 1L, calendar = "cal", remoteId = "r", remoteParent = "parent-uid")
        val task = AppTask(id = 1L, parent = 99L, modificationDate = 2000L)
        doApplyLocal(remoteModel, ct, task)
        assertEquals("parent-uid", remoteModel.parent)
    }

    @Test
    fun applyLocalSetsOrder() {
        val remoteModel = Task()
        val ct = CaldavTask(task = 1L, calendar = "cal", remoteId = "r")
        val task = AppTask(id = 1L, order = 42L, modificationDate = 2000L)
        doApplyLocal(remoteModel, ct, task)
        assertEquals(42L, remoteModel.order)
    }

    @Test
    fun applyLocalSetsCollapsed() {
        val remoteModel = Task()
        val ct = CaldavTask(task = 1L, calendar = "cal", remoteId = "r")
        val task = AppTask(id = 1L, isCollapsed = true, modificationDate = 2000L)
        doApplyLocal(remoteModel, ct, task)
        assertTrue(remoteModel.collapsed)
    }

    @Test
    fun applyLocalSetsNotCollapsed() {
        val remoteModel = Task()
        val ct = CaldavTask(task = 1L, calendar = "cal", remoteId = "r")
        val task = AppTask(id = 1L, isCollapsed = false, modificationDate = 2000L)
        doApplyLocal(remoteModel, ct, task)
        assertFalse(remoteModel.collapsed)
    }

    @Test
    fun applyLocalWithRecurrence() {
        val remoteModel = Task()
        val ct = CaldavTask(task = 1L, calendar = "cal", remoteId = "r")
        val task = AppTask(id = 1L, recurrence = "FREQ=DAILY", modificationDate = 2000L)
        doApplyLocal(remoteModel, ct, task)
        assertNotNull(remoteModel.rRule)
    }

    @Test
    fun applyLocalWithNullRecurrence() {
        val remoteModel = Task()
        val ct = CaldavTask(task = 1L, calendar = "cal", remoteId = "r")
        val task = AppTask(id = 1L, recurrence = null, modificationDate = 2000L)
        doApplyLocal(remoteModel, ct, task)
        assertNull(remoteModel.rRule)
    }

    @Test
    fun applyLocalWithDueDate() {
        val remoteModel = Task()
        val ct = CaldavTask(task = 1L, calendar = "cal", remoteId = "r")
        // Task with due time (dueDate % 60000 > 0)
        val dueMs = (System.currentTimeMillis() / 60000) * 60000 + 1000
        val task = AppTask(id = 1L, dueDate = dueMs, modificationDate = 2000L)
        doApplyLocal(remoteModel, ct, task)
        assertNotNull(remoteModel.due)
    }

    @Test
    fun applyLocalWithDueDateAllDay() {
        val remoteModel = Task()
        val ct = CaldavTask(task = 1L, calendar = "cal", remoteId = "r")
        // All-day due (dueDate % 60000 == 0)
        val dueMs = (System.currentTimeMillis() / 60000) * 60000
        val task = AppTask(id = 1L, dueDate = dueMs, modificationDate = 2000L)
        doApplyLocal(remoteModel, ct, task)
        assertNotNull(remoteModel.due)
    }

    @Test
    fun applyLocalStartDateClampedToDueDate() {
        val remoteModel = Task()
        val ct = CaldavTask(task = 1L, calendar = "cal", remoteId = "r")
        val now = System.currentTimeMillis()
        val dueMs = (now / 60000) * 60000
        val hideMs = ((now + 86400000L) / 60000) * 60000
        val task = AppTask(id = 1L, dueDate = dueMs, hideUntil = hideMs, modificationDate = 2000L)
        doApplyLocal(remoteModel, ct, task)
        assertNotNull(remoteModel.due)
        assertNotNull(remoteModel.dtStart)
    }

    // ===== parent setter with multiple parents =====

    @Test
    fun setParentWithMultipleExistingParents() {
        val task = Task(uid = "child")
        task.relatedTo.add(RelatedTo("parent-1"))
        task.relatedTo.add(RelatedTo("parent-2"))
        task.parent = "new-parent"
        assertEquals("new-parent", task.parent)
        assertEquals(1, task.relatedTo.size)
    }

    // ===== order setter updates =====

    @Test
    fun orderSetToNewValueOnExisting() {
        val task = Task(uid = "uid")
        task.unknownProperties.add(XProperty("X-APPLE-SORT-ORDER", "10"))
        task.order = 20L
        assertEquals(20L, task.order)
        assertEquals(1, task.unknownProperties.count(IS_APPLE_SORT_ORDER))
    }

    // ===== collapsed setter updates =====

    @Test
    fun collapsedSetTrueUpdatesExisting() {
        val task = Task(uid = "uid")
        task.unknownProperties.add(XProperty("X-OC-HIDESUBTASKS", "0"))
        task.collapsed = true
        assertTrue(task.collapsed)
    }

    @Test
    fun collapsedSetFalseRemovesProperty() {
        val task = Task(uid = "uid")
        task.collapsed = true
        assertTrue(task.collapsed)
        task.collapsed = false
        assertFalse(task.collapsed)
    }

    // ===== getLocal =====

    @Test
    fun getLocalReturnsMillis() {
        val now = newDateTime().millis
        val dateTime = ICalDateTime(now)
        val due = Due(dateTime)
        val result = getLocal(due)
        assertTrue(result > 0)
    }

    // ===== getDateTime =====

    @Test
    fun getDateTimeCreatesValidDateTime() {
        val now = newDateTime().millis
        val result = getDateTime(now)
        assertNotNull(result)
        assertTrue(result is ICalDateTime)
    }

    // ===== fromVtodo parsing =====

    @Test
    fun fromVtodoWithSummary() {
        val vtodo = "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//Test//Test//EN\nBEGIN:VTODO\nUID:test-123\nSUMMARY:Buy groceries\nPRIORITY:5\nEND:VTODO\nEND:VCALENDAR"
        val result = fromVtodo(vtodo)
        assertNotNull(result)
        assertEquals("test-123", result!!.uid)
        assertEquals("Buy groceries", result.summary)
        assertEquals(5, result.priority)
    }

    @Test
    fun fromVtodoWithGeo() {
        val vtodo = "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//Test//Test//EN\nBEGIN:VTODO\nUID:geo-test\nSUMMARY:Test\nGEO:37.386013;-122.082932\nEND:VTODO\nEND:VCALENDAR"
        val result = fromVtodo(vtodo)
        assertNotNull(result)
        assertNotNull(result!!.geoPosition)
    }

    @Test
    fun fromVtodoWithCategories() {
        val vtodo = "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//Test//Test//EN\nBEGIN:VTODO\nUID:cat-test\nSUMMARY:Task\nCATEGORIES:Work,Personal\nEND:VTODO\nEND:VCALENDAR"
        val result = fromVtodo(vtodo)
        assertNotNull(result)
        assertEquals(2, result!!.categories.size)
    }

    @Test
    fun fromVtodoWithRecurrence() {
        val vtodo = "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//Test//Test//EN\nBEGIN:VTODO\nUID:recur-test\nSUMMARY:Task\nRRULE:FREQ=WEEKLY;BYDAY=MO,WE,FR\nEND:VTODO\nEND:VCALENDAR"
        val result = fromVtodo(vtodo)
        assertNotNull(result)
        assertNotNull(result!!.rRule)
    }

    @Test
    fun fromVtodoWithDescription() {
        val vtodo = "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//Test//Test//EN\nBEGIN:VTODO\nUID:desc-test\nSUMMARY:Test\nDESCRIPTION:Detailed description\nEND:VTODO\nEND:VCALENDAR"
        val result = fromVtodo(vtodo)
        assertNotNull(result)
        assertEquals("Detailed description", result!!.description)
    }

    @Test
    fun fromVtodoWithCompletedStatus() {
        val vtodo = "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//Test//Test//EN\nBEGIN:VTODO\nUID:done\nSUMMARY:Done\nSTATUS:COMPLETED\nCOMPLETED:20230101T120000Z\nEND:VTODO\nEND:VCALENDAR"
        val result = fromVtodo(vtodo)
        assertNotNull(result)
        assertEquals(Status.VTODO_COMPLETED, result!!.status)
        assertNotNull(result.completedAt)
    }

    @Test
    fun fromVtodoWithRelatedTo() {
        val vtodo = "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//Test//Test//EN\nBEGIN:VTODO\nUID:child-test\nSUMMARY:Sub-task\nRELATED-TO:parent-uid\nEND:VTODO\nEND:VCALENDAR"
        val result = fromVtodo(vtodo)
        assertNotNull(result)
        assertEquals("parent-uid", result!!.parent)
    }

    @Test
    fun fromVtodoWithAppleSortOrder() {
        val vtodo = "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//Test//Test//EN\nBEGIN:VTODO\nUID:order-test\nSUMMARY:Ordered\nX-APPLE-SORT-ORDER:42\nEND:VTODO\nEND:VCALENDAR"
        val result = fromVtodo(vtodo)
        assertNotNull(result)
        assertEquals(42L, result!!.order)
    }

    @Test
    fun fromVtodoWithHideSubtasks() {
        val vtodo = "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//Test//Test//EN\nBEGIN:VTODO\nUID:collapsed-test\nSUMMARY:Collapsed\nX-OC-HIDESUBTASKS:1\nEND:VTODO\nEND:VCALENDAR"
        val result = fromVtodo(vtodo)
        assertNotNull(result)
        assertTrue(result!!.collapsed)
    }

    // ===== prodId / supportsReminders =====

    @Test
    fun prodIdExtractedFromVtodo() {
        val vtodo = "BEGIN:VCALENDAR\nPRODID:-//Apple Inc.//Mac OS X 13//EN\nVERSION:2.0\nEND:VCALENDAR\n"
        val prodId = with(iCalendar.Companion) { vtodo.prodId() }
        assertEquals("-//Apple Inc.//Mac OS X 13//EN", prodId)
    }

    @Test
    fun prodIdNullWhenNoProdId() {
        val vtodo = "BEGIN:VCALENDAR\nVERSION:2.0\nEND:VCALENDAR\n"
        val prodId = with(iCalendar.Companion) { vtodo.prodId() }
        assertNull(prodId)
    }

    @Test
    fun supportsRemindersTasksOrg() {
        assertTrue(with(iCalendar.Companion) { "tasks.org/product".supportsReminders() })
    }

    @Test
    fun supportsRemindersMozilla() {
        assertTrue(with(iCalendar.Companion) { "Mozilla.org/Calendar".supportsReminders() })
    }

    @Test
    fun supportsRemindersApple() {
        assertTrue(with(iCalendar.Companion) { "Apple Inc./iCal".supportsReminders() })
    }

    @Test
    fun doesNotSupportRemindersUnknown() {
        assertFalse(with(iCalendar.Companion) { "Unknown Client".supportsReminders() })
    }

    @Test
    fun doesNotSupportRemindersEmpty() {
        assertFalse(with(iCalendar.Companion) { "".supportsReminders() })
    }

    // ===== filtered alarms =====

    @Test
    fun filteredRemovesNonDisplayNonAudio() {
        val vtodo = "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//Test//Test//EN\nBEGIN:VTODO\nUID:alarm-test\nSUMMARY:Alarm test\nBEGIN:VALARM\nACTION:DISPLAY\nTRIGGER:-PT15M\nDESCRIPTION:Reminder\nEND:VALARM\nEND:VTODO\nEND:VCALENDAR"
        val result = fromVtodo(vtodo)
        assertNotNull(result)
        val filtered = with(iCalendar.Companion) { result!!.alarms.filtered }
        assertEquals(1, filtered.size)
    }
}
