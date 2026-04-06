package org.tasks.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationManagerConstantsTest {

    @Test
    fun defaultChannelName() {
        assertEquals("notifications", NotificationManager.NOTIFICATION_CHANNEL_DEFAULT)
    }

    @Test
    fun taskerChannelName() {
        assertEquals("notifications_tasker", NotificationManager.NOTIFICATION_CHANNEL_TASKER)
    }

    @Test
    fun timersChannelName() {
        assertEquals("notifications_timers", NotificationManager.NOTIFICATION_CHANNEL_TIMERS)
    }

    @Test
    fun miscellaneousChannelName() {
        assertEquals(
            "notifications_miscellaneous",
            NotificationManager.NOTIFICATION_CHANNEL_MISCELLANEOUS
        )
    }

    @Test
    fun maxNotifications() {
        assertEquals(21, NotificationManager.MAX_NOTIFICATIONS)
    }

    @Test
    fun extraNotificationIdKey() {
        assertEquals("extra_notification_id", NotificationManager.EXTRA_NOTIFICATION_ID)
    }

    @Test
    fun summaryNotificationIdIsZero() {
        assertEquals(0, NotificationManager.SUMMARY_NOTIFICATION_ID)
    }

    @Test
    fun channelNamesAreDistinct() {
        val channels = setOf(
            NotificationManager.NOTIFICATION_CHANNEL_DEFAULT,
            NotificationManager.NOTIFICATION_CHANNEL_TASKER,
            NotificationManager.NOTIFICATION_CHANNEL_TIMERS,
            NotificationManager.NOTIFICATION_CHANNEL_MISCELLANEOUS,
        )
        assertEquals(4, channels.size)
    }

    @Test
    fun allChannelsStartWithNotifications() {
        val channels = listOf(
            NotificationManager.NOTIFICATION_CHANNEL_DEFAULT,
            NotificationManager.NOTIFICATION_CHANNEL_TASKER,
            NotificationManager.NOTIFICATION_CHANNEL_TIMERS,
            NotificationManager.NOTIFICATION_CHANNEL_MISCELLANEOUS,
        )
        channels.forEach { channel ->
            assert(channel.startsWith("notifications")) {
                "Channel '$channel' does not start with 'notifications'"
            }
        }
    }
}
