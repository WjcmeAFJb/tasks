package org.tasks.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationManagerTest {

    @Test
    fun maxNotificationsIs21() {
        assertEquals(21, NotificationManager.MAX_NOTIFICATIONS)
    }

    @Test
    fun summaryNotificationIdIsZero() {
        assertEquals(0, NotificationManager.SUMMARY_NOTIFICATION_ID)
    }

    @Test
    fun defaultChannelId() {
        assertEquals("notifications", NotificationManager.NOTIFICATION_CHANNEL_DEFAULT)
    }

    @Test
    fun taskerChannelId() {
        assertEquals("notifications_tasker", NotificationManager.NOTIFICATION_CHANNEL_TASKER)
    }

    @Test
    fun timersChannelId() {
        assertEquals("notifications_timers", NotificationManager.NOTIFICATION_CHANNEL_TIMERS)
    }

    @Test
    fun miscellaneousChannelId() {
        assertEquals(
            "notifications_miscellaneous",
            NotificationManager.NOTIFICATION_CHANNEL_MISCELLANEOUS,
        )
    }

    @Test
    fun extraNotificationIdKey() {
        assertEquals("extra_notification_id", NotificationManager.EXTRA_NOTIFICATION_ID)
    }

    @Test
    fun channelIdsAreDistinct() {
        val channels = setOf(
            NotificationManager.NOTIFICATION_CHANNEL_DEFAULT,
            NotificationManager.NOTIFICATION_CHANNEL_TASKER,
            NotificationManager.NOTIFICATION_CHANNEL_TIMERS,
            NotificationManager.NOTIFICATION_CHANNEL_MISCELLANEOUS,
        )
        assertEquals(4, channels.size)
    }
}
