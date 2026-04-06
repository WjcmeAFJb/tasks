package org.tasks.data.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.Task.Companion.NO_ID

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

    // --- Task default ---

    @Test fun defaultTaskIsNoId() {
        assertEquals(NO_ID, CaldavTask(calendar = "cal").task)
    }

    @Test fun taskCanBeSet() {
        val ct = CaldavTask(calendar = "cal", task = 42)
        assertEquals(42L, ct.task)
    }

    // --- obj with explicit values ---

    @Test fun objCanBeOverridden() {
        val ct = CaldavTask(calendar = "cal", remoteId = "abc", obj = "custom.ics")
        assertEquals("custom.ics", ct.obj)
    }

    @Test fun objCanBeSetToNull() {
        val ct = CaldavTask(calendar = "cal", remoteId = "abc", obj = null)
        assertNull(ct.obj)
    }

    // --- isMoved ---

    @Test fun isMovedCanBeSetTrue() {
        val ct = CaldavTask(calendar = "cal", isMoved = true)
        assertTrue(ct.isMoved)
    }

    // --- remoteParent ---

    @Test fun remoteParentCanBeSet() {
        val ct = CaldavTask(calendar = "cal", remoteParent = "parent-id")
        assertEquals("parent-id", ct.remoteParent)
    }

    // --- etag ---

    @Test fun etagCanBeSet() {
        val ct = CaldavTask(calendar = "cal", etag = "\"abc123\"")
        assertEquals("\"abc123\"", ct.etag)
    }

    // --- lastSync ---

    @Test fun lastSyncCanBeSet() {
        val ct = CaldavTask(calendar = "cal", lastSync = 1234567890L)
        assertEquals(1234567890L, ct.lastSync)
    }

    // --- remoteOrder ---

    @Test fun remoteOrderCanBeSet() {
        val ct = CaldavTask(calendar = "cal", remoteOrder = 99)
        assertEquals(99L, ct.remoteOrder)
    }

    // --- isDeleted edge case ---

    @Test fun isDeletedForLargeValue() {
        assertTrue(CaldavTask(calendar = "c", deleted = Long.MAX_VALUE).isDeleted())
    }

    // --- Data class inequality ---

    @Test fun inequalityOnDifferentCalendar() {
        val a = CaldavTask(calendar = "cal1", remoteId = "abc", obj = "abc.ics")
        val b = CaldavTask(calendar = "cal2", remoteId = "abc", obj = "abc.ics")
        assertNotEquals(a, b)
    }

    @Test fun inequalityOnDifferentRemoteId() {
        val a = CaldavTask(calendar = "cal", remoteId = "abc", obj = "abc.ics")
        val b = CaldavTask(calendar = "cal", remoteId = "def", obj = "abc.ics")
        assertNotEquals(a, b)
    }

    @Test fun hashCodeConsistentWithEquals() {
        val a = CaldavTask(calendar = "cal", remoteId = "abc", obj = "abc.ics")
        val b = CaldavTask(calendar = "cal", remoteId = "abc", obj = "abc.ics")
        assertEquals(a.hashCode(), b.hashCode())
    }

    // --- copy preserves values ---

    @Test fun copyPreservesAllFields() {
        val original = CaldavTask(
            id = 1, task = 42, calendar = "cal", remoteId = "abc",
            obj = "abc.ics", etag = "\"etag\"", lastSync = 100,
            deleted = 0, remoteParent = "parent", isMoved = true, remoteOrder = 5
        )
        val copy = original.copy(calendar = "new-cal")
        assertEquals("new-cal", copy.calendar)
        assertEquals(1L, copy.id)
        assertEquals(42L, copy.task)
        assertEquals("abc", copy.remoteId)
        assertEquals("abc.ics", copy.obj)
        assertEquals("\"etag\"", copy.etag)
        assertEquals(100L, copy.lastSync)
        assertEquals(0L, copy.deleted)
        assertEquals("parent", copy.remoteParent)
        assertTrue(copy.isMoved)
        assertEquals(5L, copy.remoteOrder)
    }

    // --- TABLE column names ---

    @Test fun taskColumnName() {
        assertNotNull(CaldavTask.TASK)
    }

    @Test fun deletedColumnName() {
        assertNotNull(CaldavTask.DELETED)
    }

    @Test fun calendarColumnName() {
        assertNotNull(CaldavTask.CALENDAR)
    }

    @Test fun idColumnName() {
        assertNotNull(CaldavTask.ID)
    }

    // --- Explicit construction with all fields ---

    @Test fun explicitConstruction() {
        val ct = CaldavTask(
            id = 10,
            task = 20,
            calendar = "work",
            remoteId = "remote-1",
            obj = "remote-1.ics",
            etag = "etag-val",
            lastSync = 999,
            deleted = 0,
            remoteParent = "parent-1",
            isMoved = false,
            remoteOrder = 7
        )
        assertEquals(10L, ct.id)
        assertEquals(20L, ct.task)
        assertEquals("work", ct.calendar)
        assertEquals("remote-1", ct.remoteId)
        assertEquals("remote-1.ics", ct.obj)
        assertEquals("etag-val", ct.etag)
        assertEquals(999L, ct.lastSync)
        assertEquals(0L, ct.deleted)
        assertEquals("parent-1", ct.remoteParent)
        assertFalse(ct.isMoved)
        assertEquals(7L, ct.remoteOrder)
    }
}
