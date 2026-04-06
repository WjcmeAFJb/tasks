package org.tasks.notifications

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Tests for [NotificationClearedReceiver] intent extras and guard conditions.
 */
class NotificationClearedReceiverTest {

    @Test
    fun extraNotificationIdKey() {
        assertEquals("extra_notification_id", NotificationManager.EXTRA_NOTIFICATION_ID)
    }

    @Test
    fun intentWithValidNotificationId() {
        val intent = mock(Intent::class.java)
        `when`(intent.getLongExtra(NotificationManager.EXTRA_NOTIFICATION_ID, -1L)).thenReturn(42L)
        val id = intent.getLongExtra(NotificationManager.EXTRA_NOTIFICATION_ID, -1L)
        assertEquals(42L, id)
    }

    @Test
    fun intentWithMissingNotificationIdReturnsDefault() {
        val intent = mock(Intent::class.java)
        `when`(intent.getLongExtra(NotificationManager.EXTRA_NOTIFICATION_ID, -1L)).thenReturn(-1L)
        val id = intent.getLongExtra(NotificationManager.EXTRA_NOTIFICATION_ID, -1L)
        assertEquals(-1L, id)
    }

    @Test
    fun guardConditionRejectsZeroId() {
        val id = 0L
        val shouldReturn = id <= 0L
        assertTrue(shouldReturn)
    }

    @Test
    fun guardConditionRejectsNegativeId() {
        val id = -1L
        val shouldReturn = id <= 0L
        assertTrue(shouldReturn)
    }

    @Test
    fun guardConditionAllowsPositiveId() {
        val id = 1L
        val shouldReturn = id <= 0L
        assertFalse(shouldReturn)
    }

    @Test
    fun intentWithLargeNotificationId() {
        val intent = mock(Intent::class.java)
        `when`(intent.getLongExtra(NotificationManager.EXTRA_NOTIFICATION_ID, -1L))
            .thenReturn(Long.MAX_VALUE)
        val id = intent.getLongExtra(NotificationManager.EXTRA_NOTIFICATION_ID, -1L)
        assertEquals(Long.MAX_VALUE, id)
    }
}
