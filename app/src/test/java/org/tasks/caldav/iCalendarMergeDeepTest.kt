@file:Suppress("ClassName")

package org.tasks.caldav

import com.natpryce.makeiteasy.MakeItEasy.with
import net.fortuna.ical4j.model.property.Due
import net.fortuna.ical4j.model.property.DtStart
import net.fortuna.ical4j.model.property.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.caldav.iCalendar.Companion.apply
import org.tasks.caldav.iCalendar.Companion.collapsed
import org.tasks.caldav.iCalendar.Companion.getDateTime
import org.tasks.caldav.iCalendar.Companion.order
import org.tasks.caldav.iCalendar.Companion.parent
import org.tasks.caldav.iCalendar.Companion.toMillis
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.data.entity.Task.Priority.Companion.HIGH
import org.tasks.data.entity.Task.Priority.Companion.LOW
import org.tasks.data.entity.Task.Priority.Companion.MEDIUM
import org.tasks.data.entity.Task.Priority.Companion.NONE
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.makers.CaldavTaskMaker.REMOTE_PARENT
import org.tasks.makers.CaldavTaskMaker.newCaldavTask
import org.tasks.makers.TaskMaker
import org.tasks.makers.TaskMaker.newTask
import org.tasks.makers.iCalMaker
import org.tasks.makers.iCalMaker.COLLAPSED
import org.tasks.makers.iCalMaker.COMPLETED_AT
import org.tasks.makers.iCalMaker.CREATED_AT
import org.tasks.makers.iCalMaker.DESCRIPTION
import org.tasks.makers.iCalMaker.DUE_DATE
import org.tasks.makers.iCalMaker.DUE_TIME
import org.tasks.makers.iCalMaker.ORDER
import org.tasks.makers.iCalMaker.PARENT
import org.tasks.makers.iCalMaker.PRIORITY
import org.tasks.makers.iCalMaker.RRULE
import org.tasks.makers.iCalMaker.START_DATE
import org.tasks.makers.iCalMaker.START_TIME
import org.tasks.makers.iCalMaker.STATUS
import org.tasks.makers.iCalMaker.TITLE
import org.tasks.makers.iCalMaker.newIcal
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils.toDate

/**
 * Deep tests for the iCalendarMerge functions: applyRemote for Task and CaldavTask,
 * as well as Due/DtStart apply extensions.
 */
class iCalendarMergeDeepTest {

    // ===== Task.applyRemote: completedAt edge cases =====

    @Test
    fun completedAtFromRemoteWithCompletedDate() {
        val completed = newDateTime()
        val task = newTask()
        task.applyRemote(
            remote = newIcal(with(COMPLETED_AT, completed.toUTC())),
            local = null
        )
        assertTrue(task.isCompleted)
        assertTrue(task.completionDate > 0)
    }

    @Test
    fun completedStatusWithoutCompletedAtSetsCurrentTime() {
        val task = newTask()
        task.applyRemote(
            remote = newIcal(with(STATUS, Status.VTODO_COMPLETED)),
            local = null
        )
        assertTrue(task.isCompleted)
        assertTrue(task.completionDate > 0)
    }

    @Test
    fun completedStatusPreservesExistingCompletionDate() {
        val now = newDateTime()
        val task = newTask(with(TaskMaker.COMPLETION_TIME, now))
        task.applyRemote(
            remote = newIcal(with(STATUS, Status.VTODO_COMPLETED)),
            local = null
        )
        assertTrue(task.isCompleted)
        assertEquals(now.millis, task.completionDate)
    }

    @Test
    fun noCompletionClearsCompletionDate() {
        val now = newDateTime()
        val task = newTask(with(TaskMaker.COMPLETION_TIME, now))
        task.applyRemote(
            remote = newIcal(), // no completion
            local = null
        )
        assertFalse(task.isCompleted)
        assertEquals(0L, task.completionDate)
    }

    @Test
    fun inProcessStatusClearsCompletion() {
        val now = newDateTime()
        val task = newTask(with(TaskMaker.COMPLETION_TIME, now))
        task.applyRemote(
            remote = newIcal(with(STATUS, Status.VTODO_IN_PROCESS)),
            local = null
        )
        assertFalse(task.isCompleted)
    }

    @Test
    fun cancelledStatusClearsCompletion() {
        val now = newDateTime()
        val task = newTask(with(TaskMaker.COMPLETION_TIME, now))
        task.applyRemote(
            remote = newIcal(with(STATUS, Status.VTODO_CANCELLED)),
            local = null
        )
        assertFalse(task.isCompleted)
    }

    @Test
    fun localCompletionWinsWhenLocalChanged() {
        val completedRemote = newDateTime().plusHours(1)
        val completedLocal = newDateTime().plusHours(2)
        val original = newDateTime()

        // Task has completion date that differs from local cache
        val task = newTask(with(TaskMaker.COMPLETION_TIME, completedLocal))
        task.applyRemote(
            remote = newIcal(with(COMPLETED_AT, completedRemote.toUTC())),
            local = newIcal(with(COMPLETED_AT, original.toUTC()))
        )
        // Since local completionDate != getLocal(local.completedAt), local wins
        assertEquals(completedLocal.millis, task.completionDate)
    }

    // ===== Task.applyRemote: title handling =====

    @Test
    fun titleAppliedFromRemoteNewTask() {
        val task = newTask()
        task.applyRemote(
            remote = newIcal(with(TITLE, "Remote Title")),
            local = null
        )
        assertEquals("Remote Title", task.title)
    }

    @Test
    fun titleAppliedFromRemoteWhenLocalUnchanged() {
        val task = newTask(with(TaskMaker.TITLE, "Original"))
        task.applyRemote(
            remote = newIcal(with(TITLE, "Updated")),
            local = newIcal(with(TITLE, "Original"))
        )
        assertEquals("Updated", task.title)
    }

    @Test
    fun titleLocalWinsWhenChanged() {
        val task = newTask(with(TaskMaker.TITLE, "Local Edit"))
        task.applyRemote(
            remote = newIcal(with(TITLE, "Remote Edit")),
            local = newIcal(with(TITLE, "Unmodified"))
        )
        assertEquals("Local Edit", task.title)
    }

    @Test
    fun nullTitleAppliedFromRemote() {
        val task = newTask()
        task.applyRemote(
            remote = newIcal(), // null title
            local = null
        )
        assertNull(task.title)
    }

    // ===== Task.applyRemote: description handling =====

    @Test
    fun descriptionAppliedFromRemoteNewTask() {
        val task = newTask()
        task.applyRemote(
            remote = newIcal(with(DESCRIPTION, "Remote Note")),
            local = null
        )
        assertEquals("Remote Note", task.notes)
    }

    @Test
    fun descriptionAppliedFromRemoteWhenLocalUnchanged() {
        val task = newTask(with(TaskMaker.DESCRIPTION, "Old Note"))
        task.applyRemote(
            remote = newIcal(with(DESCRIPTION, "New Note")),
            local = newIcal(with(DESCRIPTION, "Old Note"))
        )
        assertEquals("New Note", task.notes)
    }

    @Test
    fun descriptionLocalWinsWhenChanged() {
        val task = newTask(with(TaskMaker.DESCRIPTION, "My Edit"))
        task.applyRemote(
            remote = newIcal(with(DESCRIPTION, "Remote Edit")),
            local = newIcal(with(DESCRIPTION, "Original"))
        )
        assertEquals("My Edit", task.notes)
    }

    @Test
    fun nullDescriptionAppliedFromRemote() {
        val task = newTask()
        task.applyRemote(
            remote = newIcal(),
            local = null
        )
        assertNull(task.notes)
    }

    // ===== Task.applyRemote: priority edge cases =====

    @Test
    fun priorityAppliedFromRemoteNewTask() {
        val task = newTask()
        task.applyRemote(
            remote = newIcal(with(PRIORITY, 1)),
            local = null
        )
        assertEquals(HIGH, task.priority)
    }

    @Test
    fun priorityLocalWinsWhenChanged() {
        val task = newTask(with(TaskMaker.PRIORITY, LOW))
        task.applyRemote(
            remote = newIcal(with(PRIORITY, 1)),
            local = newIcal(with(PRIORITY, 5))
        )
        // local priority (LOW) != tasksPriority of local (MEDIUM) so local wins
        assertEquals(LOW, task.priority)
    }

    @Test
    fun priorityRemoteWinsWhenLocalUnchanged() {
        val task = newTask(with(TaskMaker.PRIORITY, MEDIUM))
        task.applyRemote(
            remote = newIcal(with(PRIORITY, 1)),
            local = newIcal(with(PRIORITY, 5))
        )
        assertEquals(HIGH, task.priority)
    }

    // ===== Task.applyRemote: recurrence =====

    @Test
    fun recurrenceAppliedFromRemoteNewTask() {
        val task = newTask()
        task.applyRemote(
            remote = newIcal(with(RRULE, "FREQ=MONTHLY")),
            local = null
        )
        assertNotNull(task.recurrence)
        assertTrue(task.recurrence!!.contains("MONTHLY"))
    }

    @Test
    fun nullRecurrenceAppliedFromRemote() {
        val task = newTask()
        task.applyRemote(
            remote = newIcal(),
            local = null
        )
        assertNull(task.recurrence)
    }

    @Test
    fun recurrenceRemoteWinsWhenLocalUnchanged() {
        val task = newTask(with(TaskMaker.RECUR, "FREQ=DAILY"))
        task.applyRemote(
            remote = newIcal(with(RRULE, "FREQ=WEEKLY")),
            local = newIcal(with(RRULE, "FREQ=DAILY"))
        )
        assertTrue(task.recurrence!!.contains("WEEKLY"))
    }

    @Test
    fun recurrenceLocalWinsWhenChanged() {
        val task = newTask(with(TaskMaker.RECUR, "FREQ=YEARLY"))
        task.applyRemote(
            remote = newIcal(with(RRULE, "FREQ=WEEKLY")),
            local = newIcal(with(RRULE, "FREQ=DAILY"))
        )
        assertTrue(task.recurrence!!.contains("YEARLY"))
    }

    // ===== Task.applyRemote: due date =====

    @Test
    fun dueDateAppliedFromRemoteNewTask() {
        val dueDate = newDateTime().plusDays(5)
        val task = newTask()
        task.applyRemote(
            remote = newIcal(with(DUE_DATE, DateTime(dueDate.year, dueDate.monthOfYear, dueDate.dayOfMonth))),
            local = null
        )
        assertTrue(task.dueDate > 0)
    }

    @Test
    fun dueDateNullAppliedFromRemote() {
        val task = newTask()
        task.applyRemote(
            remote = newIcal(),
            local = null
        )
        assertEquals(0L, task.dueDate)
    }

    // ===== Task.applyRemote: start date =====

    @Test
    fun startDateAppliedFromRemoteNewTask() {
        val startDate = newDateTime().plusDays(3)
        val task = newTask()
        task.applyRemote(
            remote = newIcal(with(START_DATE, DateTime(startDate.year, startDate.monthOfYear, startDate.dayOfMonth))),
            local = null
        )
        assertTrue(task.hideUntil > 0)
    }

    @Test
    fun startDateNullAppliedFromRemote() {
        val task = newTask()
        task.applyRemote(
            remote = newIcal(),
            local = null
        )
        assertEquals(0L, task.hideUntil)
    }

    // ===== Task.applyRemote: collapsed =====

    @Test
    fun collapsedAppliedFromRemoteNewTask() {
        val task = newTask()
        task.applyRemote(
            remote = newIcal(with(COLLAPSED, true)),
            local = null
        )
        assertTrue(task.isCollapsed)
    }

    @Test
    fun collapsedDefaultFalse() {
        val task = newTask()
        task.applyRemote(
            remote = newIcal(),
            local = null
        )
        assertFalse(task.isCollapsed)
    }

    @Test
    fun collapsedRemoteWinsWhenLocalUnchanged() {
        val task = newTask(with(TaskMaker.COLLAPSED, false))
        task.applyRemote(
            remote = newIcal(with(COLLAPSED, true)),
            local = newIcal(with(COLLAPSED, false))
        )
        assertTrue(task.isCollapsed)
    }

    @Test
    fun collapsedLocalWinsWhenChanged() {
        val task = newTask(with(TaskMaker.COLLAPSED, false))
        task.applyRemote(
            remote = newIcal(with(COLLAPSED, true)),
            local = newIcal(with(COLLAPSED, true))
        )
        // Local collapsed (false) != local cache collapsed (true), so local wins
        assertFalse(task.isCollapsed)
    }

    // ===== Task.applyRemote: order =====

    @Test
    fun orderAppliedFromRemoteNewTask() {
        val task = newTask()
        task.applyRemote(
            remote = newIcal(with(ORDER, 999L)),
            local = null
        )
        assertEquals(999L, task.order)
    }

    @Test
    fun orderNullFromRemoteNewTask() {
        val task = newTask()
        task.applyRemote(
            remote = newIcal(),
            local = null
        )
        assertNull(task.order)
    }

    @Test
    fun orderRemoteWinsWhenLocalUnchanged() {
        val task = newTask(with(TaskMaker.ORDER, 100L))
        task.applyRemote(
            remote = newIcal(with(ORDER, 200L)),
            local = newIcal(with(ORDER, 100L))
        )
        assertEquals(200L, task.order)
    }

    @Test
    fun orderLocalWinsWhenChanged() {
        val task = newTask(with(TaskMaker.ORDER, 300L))
        task.applyRemote(
            remote = newIcal(with(ORDER, 200L)),
            local = newIcal(with(ORDER, 100L))
        )
        assertEquals(300L, task.order)
    }

    // ===== CaldavTask.applyRemote: parent =====

    @Test
    fun parentAppliedFromRemoteNewCaldavTask() {
        val caldavTask = newCaldavTask()
        caldavTask.applyRemote(
            remote = newIcal(with(PARENT, "parent-uid")),
            local = null
        )
        assertEquals("parent-uid", caldavTask.remoteParent)
    }

    @Test
    fun parentNullFromRemoteNewCaldavTask() {
        val caldavTask = newCaldavTask()
        caldavTask.applyRemote(
            remote = newIcal(),
            local = null
        )
        assertNull(caldavTask.remoteParent)
    }

    @Test
    fun parentRemoteWinsWhenLocalUnchanged() {
        val caldavTask = newCaldavTask(with(REMOTE_PARENT, "old"))
        caldavTask.applyRemote(
            remote = newIcal(with(PARENT, "new")),
            local = newIcal(with(PARENT, "old"))
        )
        assertEquals("new", caldavTask.remoteParent)
    }

    @Test
    fun parentLocalWinsWhenChanged() {
        val caldavTask = newCaldavTask(with(REMOTE_PARENT, "local-change"))
        caldavTask.applyRemote(
            remote = newIcal(with(PARENT, "remote")),
            local = newIcal(with(PARENT, "original"))
        )
        assertEquals("local-change", caldavTask.remoteParent)
    }

    // ===== Due.apply / DtStart.apply =====

    @Test
    fun dueApplyNullSetsZero() {
        val task = Task(dueDate = 12345L)
        val due: Due? = null
        due.apply(task)
        assertEquals(0L, task.dueDate)
    }

    @Test
    fun dueApplyDateSetsNonZero() {
        val task = Task()
        val due = Due(newDateTime().plusDays(1).millis.toDate())
        due.apply(task)
        assertTrue(task.dueDate > 0)
    }

    @Test
    fun dueApplyDateTimeSetsNonZero() {
        val task = Task()
        val due = Due(getDateTime(newDateTime().plusDays(1).millis))
        due.apply(task)
        assertTrue(task.dueDate > 0)
    }

    @Test
    fun dtStartApplyNullSetsZero() {
        val task = Task(hideUntil = 12345L)
        val dtStart: DtStart? = null
        dtStart.apply(task)
        assertEquals(0L, task.hideUntil)
    }

    @Test
    fun dtStartApplyDateSetsNonZero() {
        val task = Task()
        val dtStart = DtStart(newDateTime().plusDays(1).millis.toDate())
        dtStart.apply(task)
        assertTrue(task.hideUntil > 0)
    }

    @Test
    fun dtStartApplyDateTimeSetsNonZero() {
        val task = Task()
        val dtStart = DtStart(getDateTime(newDateTime().plusDays(1).millis))
        dtStart.apply(task)
        assertTrue(task.hideUntil > 0)
    }

    // ===== Due/DtStart toMillis =====

    @Test
    fun nullDueToMillisReturnsZero() {
        val due: Due? = null
        assertEquals(0L, due.toMillis())
    }

    @Test
    fun nullDtStartToMillisReturnsZero() {
        val dtStart: DtStart? = null
        val task = Task()
        assertEquals(0L, with(iCalendar.Companion) { dtStart.toMillis(task) })
    }

    @Test
    fun dueDateToMillisReturnsNonZero() {
        val due = Due(newDateTime().plusDays(1).millis.toDate())
        assertTrue(due.toMillis() > 0)
    }

    @Test
    fun dueDateTimeToMillisReturnsNonZero() {
        val due = Due(getDateTime(newDateTime().plusDays(1).millis))
        assertTrue(due.toMillis() > 0)
    }

    @Test
    fun dtStartDateToMillisReturnsNonZero() {
        val dtStart = DtStart(newDateTime().plusDays(1).millis.toDate())
        val task = Task()
        assertTrue(with(iCalendar.Companion) { dtStart.toMillis(task) } > 0)
    }

    @Test
    fun dtStartDateTimeToMillisReturnsNonZero() {
        val dtStart = DtStart(getDateTime(newDateTime().plusDays(1).millis))
        val task = Task()
        assertTrue(with(iCalendar.Companion) { dtStart.toMillis(task) } > 0)
    }

    // ===== createdAt =====

    @Test
    fun createdAtAppliedFromRemoteWhenLocalNull() {
        val created = newDateTime()
        val task = newTask(with(TaskMaker.CREATION_TIME, created))
        task.applyRemote(
            remote = newIcal(with(CREATED_AT, created.plusHours(1).toUTC())),
            local = null
        )
        assertEquals(created.plusHours(1).millis, task.creationDate)
    }

    @Test
    fun createdAtLocalWins() {
        val created = newDateTime()
        val localChanged = created.plusHours(2)
        val task = newTask(with(TaskMaker.CREATION_TIME, localChanged))
        task.applyRemote(
            remote = newIcal(with(CREATED_AT, created.plusHours(1).toUTC())),
            local = newIcal(with(CREATED_AT, created.toUTC()))
        )
        assertEquals(localChanged.millis, task.creationDate)
    }

    @Test
    fun createdAtNotAppliedWhenRemoteNull() {
        val created = newDateTime()
        val task = newTask(with(TaskMaker.CREATION_TIME, created))
        task.applyRemote(
            remote = newIcal(),
            local = null
        )
        assertEquals(created.millis, task.creationDate)
    }

    // ===== full applyRemote chain =====

    @Test
    fun applyRemoteFullChainNewTask() {
        val created = newDateTime()
        val due = newDateTime().plusDays(5)
        val task = newTask()
        task.applyRemote(
            remote = newIcal(
                with(TITLE, "Full Task"),
                with(DESCRIPTION, "Full Description"),
                with(PRIORITY, 1),
                with(COLLAPSED, true),
                with(ORDER, 42L),
                with(RRULE, "FREQ=DAILY"),
                with(CREATED_AT, created.toUTC()),
                with(DUE_DATE, DateTime(due.year, due.monthOfYear, due.dayOfMonth)),
            ),
            local = null
        )
        assertEquals("Full Task", task.title)
        assertEquals("Full Description", task.notes)
        assertEquals(HIGH, task.priority)
        assertTrue(task.isCollapsed)
        assertEquals(42L, task.order)
        assertNotNull(task.recurrence)
        assertTrue(task.dueDate > 0)
        assertEquals(created.millis, task.creationDate)
    }

    @Test
    fun applyRemoteFullChainUpdateFromRemote() {
        val task = newTask(
            with(TaskMaker.TITLE, "Old Title"),
            with(TaskMaker.DESCRIPTION, "Old Notes"),
            with(TaskMaker.PRIORITY, MEDIUM),
            with(TaskMaker.COLLAPSED, false),
            with(TaskMaker.ORDER, 10L),
        )
        val local = newIcal(
            with(TITLE, "Old Title"),
            with(DESCRIPTION, "Old Notes"),
            with(PRIORITY, 5),
            with(COLLAPSED, false),
            with(ORDER, 10L),
        )
        task.applyRemote(
            remote = newIcal(
                with(TITLE, "New Title"),
                with(DESCRIPTION, "New Notes"),
                with(PRIORITY, 1),
                with(COLLAPSED, true),
                with(ORDER, 99L),
            ),
            local = local
        )
        assertEquals("New Title", task.title)
        assertEquals("New Notes", task.notes)
        assertEquals(HIGH, task.priority)
        assertTrue(task.isCollapsed)
        assertEquals(99L, task.order)
    }

    @Test
    fun applyRemoteFullChainLocalWinsForAll() {
        // Set local task values different from both remote and local cache
        val task = newTask(
            with(TaskMaker.TITLE, "My Local Title"),
            with(TaskMaker.DESCRIPTION, "My Local Notes"),
            with(TaskMaker.PRIORITY, LOW),
            with(TaskMaker.COLLAPSED, true),
            with(TaskMaker.ORDER, 555L),
        )
        val localCache = newIcal(
            with(TITLE, "Cache Title"),
            with(DESCRIPTION, "Cache Notes"),
            with(PRIORITY, 5),
            with(COLLAPSED, false),
            with(ORDER, 100L),
        )
        task.applyRemote(
            remote = newIcal(
                with(TITLE, "Remote Title"),
                with(DESCRIPTION, "Remote Notes"),
                with(PRIORITY, 1),
                with(COLLAPSED, false),
                with(ORDER, 200L),
            ),
            local = localCache
        )
        // All local changes should win
        assertEquals("My Local Title", task.title)
        assertEquals("My Local Notes", task.notes)
        assertEquals(LOW, task.priority)
        assertTrue(task.isCollapsed)
        assertEquals(555L, task.order)
    }
}
