@file:Suppress("ClassName")

package org.tasks.caldav

import com.natpryce.makeiteasy.MakeItEasy.with
import net.fortuna.ical4j.model.property.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.Task.Priority.Companion.HIGH
import org.tasks.data.entity.Task.Priority.Companion.LOW
import org.tasks.data.entity.Task.Priority.Companion.MEDIUM
import org.tasks.data.entity.Task.Priority.Companion.NONE
import org.tasks.makers.CaldavTaskMaker.REMOTE_PARENT
import org.tasks.makers.CaldavTaskMaker.newCaldavTask
import org.tasks.makers.TaskMaker
import org.tasks.makers.TaskMaker.COMPLETION_TIME
import org.tasks.makers.TaskMaker.CREATION_TIME
import org.tasks.makers.TaskMaker.newTask
import org.tasks.makers.iCalMaker
import org.tasks.makers.iCalMaker.COLLAPSED
import org.tasks.makers.iCalMaker.COMPLETED_AT
import org.tasks.makers.iCalMaker.CREATED_AT
import org.tasks.makers.iCalMaker.DESCRIPTION
import org.tasks.makers.iCalMaker.ORDER
import org.tasks.makers.iCalMaker.PARENT
import org.tasks.makers.iCalMaker.PRIORITY
import org.tasks.makers.iCalMaker.RRULE
import org.tasks.makers.iCalMaker.STATUS
import org.tasks.makers.iCalMaker.TITLE
import org.tasks.makers.iCalMaker.newIcal
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.time.DateTime

class iCalendarMergeExtrasTest {

    // ---- tasksPriority: RFC 5545 priority mapping ----

    @Test
    fun priorityZeroMapsToNone() =
        newTask()
            .applyRemote(
                remote = newIcal(with(PRIORITY, 0)),
                local = null
            )
            .let {
                assertEquals(NONE, it.priority)
            }

    @Test
    fun priority1MapsToHigh() =
        newTask()
            .applyRemote(
                remote = newIcal(with(PRIORITY, 1)),
                local = null
            )
            .let {
                assertEquals(HIGH, it.priority)
            }

    @Test
    fun priority2MapsToHigh() =
        newTask()
            .applyRemote(
                remote = newIcal(with(PRIORITY, 2)),
                local = null
            )
            .let {
                assertEquals(HIGH, it.priority)
            }

    @Test
    fun priority3MapsToHigh() =
        newTask()
            .applyRemote(
                remote = newIcal(with(PRIORITY, 3)),
                local = null
            )
            .let {
                assertEquals(HIGH, it.priority)
            }

    @Test
    fun priority4MapsToHigh() =
        newTask()
            .applyRemote(
                remote = newIcal(with(PRIORITY, 4)),
                local = null
            )
            .let {
                assertEquals(HIGH, it.priority)
            }

    @Test
    fun priority5MapsToMedium() =
        newTask()
            .applyRemote(
                remote = newIcal(with(PRIORITY, 5)),
                local = null
            )
            .let {
                assertEquals(MEDIUM, it.priority)
            }

    @Test
    fun priority6MapsToLow() =
        newTask()
            .applyRemote(
                remote = newIcal(with(PRIORITY, 6)),
                local = null
            )
            .let {
                assertEquals(LOW, it.priority)
            }

    @Test
    fun priority7MapsToLow() =
        newTask()
            .applyRemote(
                remote = newIcal(with(PRIORITY, 7)),
                local = null
            )
            .let {
                assertEquals(LOW, it.priority)
            }

    @Test
    fun priority8MapsToLow() =
        newTask()
            .applyRemote(
                remote = newIcal(with(PRIORITY, 8)),
                local = null
            )
            .let {
                assertEquals(LOW, it.priority)
            }

    @Test
    fun priority9MapsToLow() =
        newTask()
            .applyRemote(
                remote = newIcal(with(PRIORITY, 9)),
                local = null
            )
            .let {
                assertEquals(LOW, it.priority)
            }

    // ---- applyCreatedAt: remote updates creation date ----

    @Test
    fun remoteUpdatesCreationDate() {
        val created = newDateTime()
        val updated = created.plusHours(1)
        newTask(with(CREATION_TIME, created))
            .applyRemote(
                remote = newIcal(with(CREATED_AT, updated.toUTC())),
                local = newIcal(with(CREATED_AT, created.toUTC()))
            )
            .let {
                assertEquals(updated.millis, it.creationDate)
            }
    }

    @Test
    fun localBeatsRemoteCreationDate() {
        val created = newDateTime()
        val localUpdate = created.plusHours(2)
        newTask(with(CREATION_TIME, localUpdate))
            .applyRemote(
                remote = newIcal(with(CREATED_AT, created.plusHours(1).toUTC())),
                local = newIcal(with(CREATED_AT, created.toUTC()))
            )
            .let {
                assertEquals(localUpdate.millis, it.creationDate)
            }
    }

    @Test
    fun remoteCreationDateNotAppliedWhenNull() {
        val created = newDateTime()
        newTask(with(CREATION_TIME, created))
            .applyRemote(
                remote = newIcal(),
                local = null
            )
            .let {
                assertEquals(created.millis, it.creationDate)
            }
    }

    // ---- applyCompletedAt: edge cases ----

    @Test
    fun remoteStatusCompletedWithNoCompletedAtSetsCompletion() =
        newTask()
            .applyRemote(
                remote = newIcal(with(STATUS, Status.VTODO_COMPLETED)),
                local = null
            )
            .let {
                assertTrue(it.isCompleted)
                assertTrue(it.completionDate > 0)
            }

    @Test
    fun remoteStatusCompletedPreservesExistingCompletionDate() {
        val now = newDateTime()
        newTask(with(COMPLETION_TIME, now))
            .applyRemote(
                remote = newIcal(with(STATUS, Status.VTODO_COMPLETED)),
                local = null
            )
            .let {
                assertTrue(it.isCompleted)
                assertEquals(now.millis, it.completionDate)
            }
    }

    @Test
    fun remoteNoStatusOrCompletedAtClearsCompletion() {
        val now = newDateTime()
        newTask(with(COMPLETION_TIME, now))
            .applyRemote(
                remote = newIcal(),
                local = null
            )
            .let {
                assertFalse(it.isCompleted)
                assertEquals(0L, it.completionDate)
            }
    }

    @Test
    fun remoteInProcessStatusClearsCompletion() {
        val now = newDateTime()
        newTask(with(COMPLETION_TIME, now))
            .applyRemote(
                remote = newIcal(with(STATUS, Status.VTODO_IN_PROCESS)),
                local = null
            )
            .let {
                assertFalse(it.isCompleted)
                assertEquals(0L, it.completionDate)
            }
    }

    @Test
    fun remoteCancelledStatusClearsCompletion() {
        val now = newDateTime()
        newTask(with(COMPLETION_TIME, now))
            .applyRemote(
                remote = newIcal(with(STATUS, Status.VTODO_CANCELLED)),
                local = null
            )
            .let {
                assertFalse(it.isCompleted)
                assertEquals(0L, it.completionDate)
            }
    }

    // ---- applyTitle: null handling ----

    @Test
    fun applyNullTitleNewTask() =
        newTask()
            .applyRemote(
                remote = newIcal(),
                local = null
            )
            .let {
                assertNull(it.title)
            }

    @Test
    fun remoteUpdatedTitleToNull() =
        newTask(with(TaskMaker.TITLE, "Old Title"))
            .applyRemote(
                remote = newIcal(),
                local = newIcal(with(TITLE, "Old Title"))
            )
            .let {
                assertNull(it.title)
            }

    // ---- applyDescription: null handling ----

    @Test
    fun applyNullDescriptionNewTask() =
        newTask()
            .applyRemote(
                remote = newIcal(),
                local = null
            )
            .let {
                assertNull(it.notes)
            }

    // ---- applyRecurrence: null handling ----

    @Test
    fun applyNullRecurrenceNewTask() =
        newTask()
            .applyRemote(
                remote = newIcal(),
                local = null
            )
            .let {
                assertNull(it.recurrence)
            }

    // ---- applyCollapsed: new task defaults ----

    @Test
    fun applyCollapsedFalseNewTask() =
        newTask()
            .applyRemote(
                remote = newIcal(),
                local = null
            )
            .let {
                assertFalse(it.isCollapsed)
            }

    // ---- applyOrder: new task defaults ----

    @Test
    fun applyNullOrderNewTask() =
        newTask()
            .applyRemote(
                remote = newIcal(),
                local = null
            )
            .let {
                assertNull(it.order)
            }

    // ---- CaldavTask.applyRemote: new task defaults ----

    @Test
    fun applyNullParentNewCaldavTask() =
        newCaldavTask()
            .applyRemote(
                remote = newIcal(),
                local = null
            )
            .let {
                assertNull(it.remoteParent)
            }

    @Test
    fun remoteUpdatesParent() =
        newCaldavTask(with(REMOTE_PARENT, "old"))
            .applyRemote(
                remote = newIcal(with(PARENT, "new")),
                local = newIcal(with(PARENT, "old"))
            )
            .let {
                assertEquals("new", it.remoteParent)
            }

    // ---- applyOrder: remote updates ----

    @Test
    fun remoteUpdatesOrder() =
        newTask(with(TaskMaker.ORDER, 100L))
            .applyRemote(
                remote = newIcal(with(ORDER, 200L)),
                local = newIcal(with(ORDER, 100L))
            )
            .let {
                assertEquals(200L, it.order)
            }

    // ---- applyCollapsed: remote updates ----

    @Test
    fun remoteUpdatesCollapsed() {
        newTask()
            .applyRemote(
                remote = newIcal(with(COLLAPSED, true)),
                local = newIcal(with(COLLAPSED, false))
            )
            .let {
                assertTrue(it.isCollapsed)
            }
    }

    @Test
    fun localUpdatedCollapsedBeatsRemote() {
        newTask(with(TaskMaker.COLLAPSED, false))
            .applyRemote(
                remote = newIcal(with(COLLAPSED, true)),
                local = newIcal(with(COLLAPSED, true))
            )
            .let {
                assertFalse(it.isCollapsed)
            }
    }
}
