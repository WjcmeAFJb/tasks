package org.tasks.data.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CaldavTaskTest {
    @Test fun defaultIdIsZero() = assertEquals(0L, CaldavTask(calendar = "cal").id)
    @Test fun defaultDeletedIsZero() = assertEquals(0L, CaldavTask(calendar = "cal").deleted)
    @Test fun defaultLastSyncIsZero() = assertEquals(0L, CaldavTask(calendar = "cal").lastSync)
    @Test fun defaultIsMovedIsFalse() = assertFalse(CaldavTask(calendar = "cal").isMoved)
    @Test fun defaultRemoteOrderIsZero() = assertEquals(0L, CaldavTask(calendar = "cal").remoteOrder)
    @Test fun defaultRemoteParentIsNull() = assertEquals(null, CaldavTask(calendar = "cal").remoteParent)
    @Test fun defaultEtagIsNull() = assertEquals(null, CaldavTask(calendar = "cal").etag)

    @Test fun isDeletedWhenPositive() = assertTrue(CaldavTask(calendar = "c", deleted = 1000L).isDeleted())
    @Test fun isNotDeletedWhenZero() = assertFalse(CaldavTask(calendar = "c", deleted = 0L).isDeleted())
    @Test fun isNotDeletedWhenNegative() = assertFalse(CaldavTask(calendar = "c", deleted = -1L).isDeleted())

    @Test fun remoteIdGeneratedByDefault() {
        val task = CaldavTask(calendar = "cal")
        assertNotNull(task.remoteId)
        assertTrue(task.remoteId!!.isNotEmpty())
    }

    @Test fun objDerivedFromRemoteId() {
        val task = CaldavTask(calendar = "cal", remoteId = "abc-123")
        assertEquals("abc-123.ics", task.obj)
    }

    @Test fun objNullWhenRemoteIdNull() {
        val task = CaldavTask(calendar = "cal", remoteId = null)
        assertEquals(null, task.obj)
    }

    @Test fun calendarSetCorrectly() {
        val task = CaldavTask(calendar = "my-calendar")
        assertEquals("my-calendar", task.calendar)
    }

    @Test fun keyConstant() = assertEquals("caldav", CaldavTask.KEY)
    @Test fun tableNameConstant() = assertEquals("caldav_tasks", CaldavTask.TABLE.name())

    @Test fun copyChangesCalendar() {
        val original = CaldavTask(calendar = "old")
        val copy = original.copy(calendar = "new")
        assertEquals("new", copy.calendar)
    }

    @Test fun copyChangesDeleted() {
        val original = CaldavTask(calendar = "cal")
        val copy = original.copy(deleted = System.currentTimeMillis())
        assertTrue(copy.isDeleted())
    }

    @Test fun equalityOnSameValues() {
        val a = CaldavTask(calendar = "cal", remoteId = "abc", obj = "abc.ics")
        val b = CaldavTask(calendar = "cal", remoteId = "abc", obj = "abc.ics")
        assertEquals(a, b)
    }
}
