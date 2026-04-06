package org.tasks.caldav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task.Companion.NO_ID

class CaldavTaskExtraTest {

    @Test
    fun isDeletedWhenDeletedGreaterThanZero() {
        assertTrue(CaldavTask(calendar = "cal", deleted = 1L).isDeleted())
    }

    @Test
    fun isNotDeletedWhenDeletedIsZero() {
        assertFalse(CaldavTask(calendar = "cal", deleted = 0L).isDeleted())
    }

    @Test
    fun defaultIdIsNoId() {
        assertEquals(NO_ID, CaldavTask(calendar = "cal").id)
    }

    @Test
    fun defaultTaskIsNoId() {
        assertEquals(NO_ID, CaldavTask(calendar = "cal").task)
    }

    @Test
    fun defaultDeletedIsZero() {
        assertEquals(0L, CaldavTask(calendar = "cal").deleted)
    }

    @Test
    fun defaultLastSyncIsZero() {
        assertEquals(0L, CaldavTask(calendar = "cal").lastSync)
    }

    @Test
    fun defaultEtagIsNull() {
        assertEquals(null, CaldavTask(calendar = "cal").etag)
    }

    @Test
    fun defaultRemoteParentIsNull() {
        assertEquals(null, CaldavTask(calendar = "cal").remoteParent)
    }

    @Test
    fun defaultIsMovedIsFalse() {
        assertFalse(CaldavTask(calendar = "cal").isMoved)
    }

    @Test
    fun defaultRemoteOrderIsZero() {
        assertEquals(0L, CaldavTask(calendar = "cal").remoteOrder)
    }

    @Test
    fun remoteIdIsGenerated() {
        val task = CaldavTask(calendar = "cal")
        assertNotNull(task.remoteId)
        assertTrue(task.remoteId!!.isNotBlank())
    }

    @Test
    fun objDerivedFromRemoteId() {
        val task = CaldavTask(calendar = "cal")
        assertNotNull(task.obj)
        assertTrue(task.obj!!.endsWith(".ics"))
        assertEquals("${task.remoteId}.ics", task.obj)
    }

    @Test
    fun objNullWhenRemoteIdNull() {
        val task = CaldavTask(calendar = "cal", remoteId = null)
        assertEquals(null, task.obj)
    }

    @Test
    fun copySetsFields() {
        val original = CaldavTask(
            calendar = "cal1",
            remoteId = "remote-id",
            etag = "etag-1",
            lastSync = 12345L,
        )
        val copy = original.copy(calendar = "cal2")
        assertEquals("cal2", copy.calendar)
        assertEquals("remote-id", copy.remoteId)
        assertEquals("etag-1", copy.etag)
        assertEquals(12345L, copy.lastSync)
    }
}
