package org.tasks.data.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserActivityTest {

    // --- Default values ---

    @Test
    fun defaultIdIsNull() {
        assertNull(UserActivity().id)
    }

    @Test
    fun defaultRemoteIdIsNoUuid() {
        assertEquals(Task.NO_UUID, UserActivity().remoteId)
    }

    @Test
    fun defaultMessageIsEmpty() {
        assertEquals("", UserActivity().message)
    }

    @Test
    fun defaultPictureIsEmpty() {
        assertEquals("", UserActivity().picture)
    }

    @Test
    fun defaultTargetIdIsNoUuid() {
        assertEquals(Task.NO_UUID, UserActivity().targetId)
    }

    @Test
    fun defaultCreatedIsZero() {
        assertEquals(0L, UserActivity().created)
    }

    // --- Setters ---

    @Test
    fun setMessage() {
        val activity = UserActivity()
        activity.message = "Hello"
        assertEquals("Hello", activity.message)
    }

    @Test
    fun setPicture() {
        val activity = UserActivity()
        activity.picture = "https://example.com/img.png"
        assertEquals("https://example.com/img.png", activity.picture)
    }

    @Test
    fun setCreated() {
        val activity = UserActivity()
        activity.created = 1680537600000L
        assertEquals(1680537600000L, activity.created)
    }

    @Test
    fun setTargetId() {
        val activity = UserActivity()
        activity.targetId = "task-uuid-123"
        assertEquals("task-uuid-123", activity.targetId)
    }

    @Test
    fun setRemoteId() {
        val activity = UserActivity()
        activity.remoteId = "remote-456"
        assertEquals("remote-456", activity.remoteId)
    }

    @Test
    fun setId() {
        val activity = UserActivity()
        activity.id = 42L
        assertEquals(42L, activity.id)
    }

    // --- Table constants ---

    @Test
    fun tableNameIsUserActivity() {
        assertEquals("userActivity", UserActivity.TABLE.name())
    }

    // --- Equality ---

    @Test
    fun equalityOnSameValues() {
        val a = UserActivity(remoteId = "abc", message = "hi", picture = "pic", created = 100L)
        val b = UserActivity(remoteId = "abc", message = "hi", picture = "pic", created = 100L)
        assertEquals(a, b)
    }

    @Test
    fun hashCodeConsistentWithEquals() {
        val a = UserActivity(remoteId = "abc", message = "hi", created = 100L)
        val b = UserActivity(remoteId = "abc", message = "hi", created = 100L)
        assertEquals(a.hashCode(), b.hashCode())
    }

    // --- Copy ---

    @Test
    fun copyChangesMessage() {
        val original = UserActivity(message = "old")
        val copy = original.copy(message = "new")
        assertEquals("new", copy.message)
    }

    @Test
    fun copyPreservesRemoteId() {
        val original = UserActivity(remoteId = "r-123", message = "old")
        val copy = original.copy(message = "new")
        assertEquals("r-123", copy.remoteId)
    }

    @Test
    fun copyChangesCreated() {
        val original = UserActivity(created = 100L)
        val copy = original.copy(created = 200L)
        assertEquals(200L, copy.created)
    }

    @Test
    fun setNullMessage() {
        val activity = UserActivity()
        activity.message = null
        assertNull(activity.message)
    }

    @Test
    fun setNullPicture() {
        val activity = UserActivity()
        activity.picture = null
        assertNull(activity.picture)
    }
}
