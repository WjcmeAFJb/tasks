package org.tasks.data.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationEntityTest {

    // --- Default values ---

    @Test
    fun defaultUidIsZero() {
        assertEquals(0L, Notification().uid)
    }

    @Test
    fun defaultTaskIdIsZero() {
        assertEquals(0L, Notification().taskId)
    }

    @Test
    fun defaultTimestampIsZero() {
        assertEquals(0L, Notification().timestamp)
    }

    @Test
    fun defaultTypeIsZero() {
        assertEquals(0, Notification().type)
    }

    @Test
    fun defaultLocationIsNull() {
        assertNull(Notification().location)
    }

    // --- TABLE_NAME constant ---

    @Test
    fun tableNameIsNotification() {
        assertEquals("notification", Notification.TABLE_NAME)
    }

    // --- TABLE ---

    @Test
    fun tableIsNotNull() {
        assertNotNull(Notification.TABLE)
    }

    // --- TASK column ---

    @Test
    fun taskColumnIsNotNull() {
        assertNotNull(Notification.TASK)
    }

    // --- Explicit construction ---

    @Test
    fun explicitConstruction() {
        val notification = Notification(
            uid = 42,
            taskId = 100,
            timestamp = 1680537600000L,
            type = 3,
            location = 7L
        )
        assertEquals(42L, notification.uid)
        assertEquals(100L, notification.taskId)
        assertEquals(1680537600000L, notification.timestamp)
        assertEquals(3, notification.type)
        assertEquals(7L, notification.location)
    }

    @Test
    fun constructionWithNullLocation() {
        val notification = Notification(uid = 1, taskId = 2, timestamp = 3, type = 4, location = null)
        assertNull(notification.location)
    }

    @Test
    fun constructionWithNonNullLocation() {
        val notification = Notification(location = 42L)
        assertEquals(42L, notification.location)
    }

    // --- Data class equality ---

    @Test
    fun equalityOnSameValues() {
        val a = Notification(uid = 1, taskId = 2, timestamp = 3, type = 4, location = 5)
        val b = Notification(uid = 1, taskId = 2, timestamp = 3, type = 4, location = 5)
        assertEquals(a, b)
    }

    @Test
    fun equalityWithNullLocation() {
        val a = Notification(uid = 1, taskId = 2, timestamp = 3, type = 4, location = null)
        val b = Notification(uid = 1, taskId = 2, timestamp = 3, type = 4, location = null)
        assertEquals(a, b)
    }

    @Test
    fun hashCodeConsistentWithEquals() {
        val a = Notification(uid = 1, taskId = 2, timestamp = 3, type = 4, location = 5)
        val b = Notification(uid = 1, taskId = 2, timestamp = 3, type = 4, location = 5)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun inequalityOnDifferentUid() {
        val a = Notification(uid = 1, taskId = 2, timestamp = 3, type = 4)
        val b = Notification(uid = 99, taskId = 2, timestamp = 3, type = 4)
        assertNotEquals(a, b)
    }

    @Test
    fun inequalityOnDifferentTaskId() {
        val a = Notification(uid = 1, taskId = 2, timestamp = 3, type = 4)
        val b = Notification(uid = 1, taskId = 99, timestamp = 3, type = 4)
        assertNotEquals(a, b)
    }

    @Test
    fun inequalityOnDifferentTimestamp() {
        val a = Notification(uid = 1, taskId = 2, timestamp = 3, type = 4)
        val b = Notification(uid = 1, taskId = 2, timestamp = 999, type = 4)
        assertNotEquals(a, b)
    }

    @Test
    fun inequalityOnDifferentType() {
        val a = Notification(uid = 1, taskId = 2, timestamp = 3, type = 4)
        val b = Notification(uid = 1, taskId = 2, timestamp = 3, type = 5)
        assertNotEquals(a, b)
    }

    @Test
    fun inequalityOnDifferentLocation() {
        val a = Notification(uid = 1, taskId = 2, timestamp = 3, type = 4, location = 5)
        val b = Notification(uid = 1, taskId = 2, timestamp = 3, type = 4, location = 6)
        assertNotEquals(a, b)
    }

    @Test
    fun inequalityNullVsNonNullLocation() {
        val a = Notification(uid = 1, taskId = 2, timestamp = 3, type = 4, location = null)
        val b = Notification(uid = 1, taskId = 2, timestamp = 3, type = 4, location = 5)
        assertNotEquals(a, b)
    }

    // --- Copy ---

    @Test
    fun copyChangesUid() {
        val original = Notification(uid = 1, taskId = 2, timestamp = 3, type = 4, location = 5)
        val copy = original.copy(uid = 99)
        assertEquals(99L, copy.uid)
        assertEquals(2L, copy.taskId)
        assertEquals(3L, copy.timestamp)
        assertEquals(4, copy.type)
        assertEquals(5L, copy.location)
    }

    @Test
    fun copyChangesTaskId() {
        val original = Notification(uid = 1, taskId = 2)
        val copy = original.copy(taskId = 99)
        assertEquals(99L, copy.taskId)
        assertEquals(1L, copy.uid)
    }

    @Test
    fun copyChangesTimestamp() {
        val original = Notification(timestamp = 100)
        val copy = original.copy(timestamp = 999)
        assertEquals(999L, copy.timestamp)
    }

    @Test
    fun copyChangesType() {
        val original = Notification(type = 1)
        val copy = original.copy(type = 5)
        assertEquals(5, copy.type)
    }

    @Test
    fun copyChangesLocation() {
        val original = Notification(location = 1)
        val copy = original.copy(location = null)
        assertNull(copy.location)
    }

    @Test
    fun copyPreservesAllFields() {
        val original = Notification(uid = 1, taskId = 2, timestamp = 3, type = 4, location = 5)
        val copy = original.copy(type = 99)
        assertEquals(1L, copy.uid)
        assertEquals(2L, copy.taskId)
        assertEquals(3L, copy.timestamp)
        assertEquals(99, copy.type)
        assertEquals(5L, copy.location)
    }

    // --- toString ---

    @Test
    fun toStringContainsClassName() {
        val notification = Notification(uid = 1, taskId = 2, timestamp = 3, type = 4)
        val str = notification.toString()
        assert(str.contains("Notification"))
    }

    @Test
    fun toStringContainsFieldValues() {
        val notification = Notification(uid = 42, taskId = 100, timestamp = 999, type = 7, location = 3)
        val str = notification.toString()
        assert(str.contains("42"))
        assert(str.contains("100"))
        assert(str.contains("999"))
        assert(str.contains("7"))
        assert(str.contains("3"))
    }
}
