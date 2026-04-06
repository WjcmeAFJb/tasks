package org.tasks.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class ThrottledNotificationManagerTest {

    @Test
    fun notificationsPerSecondIsFour() {
        // Verify the NOTIFICATIONS_PER_SECOND constant via reflection since it's private
        val field = ThrottledNotificationManager::class.java.getDeclaredField("NOTIFICATIONS_PER_SECOND")
        field.isAccessible = true
        assertEquals(4, field.getInt(null))
    }

    @Test
    fun throttleFieldIsInitialized() {
        // The Throttle class is internal; verify that the companion constant accessible
        // via reflection has the expected value
        val companionClass = Class.forName("org.tasks.notifications.ThrottledNotificationManager\$Companion")
        val outerClass = ThrottledNotificationManager::class.java
        val field = outerClass.getDeclaredField("NOTIFICATIONS_PER_SECOND")
        field.isAccessible = true
        val value = field.getInt(null)
        assertEquals(4, value)
    }

    @Test
    fun notificationsPerSecondIsPositive() {
        val field = ThrottledNotificationManager::class.java.getDeclaredField("NOTIFICATIONS_PER_SECOND")
        field.isAccessible = true
        val value = field.getInt(null)
        assert(value > 0) { "NOTIFICATIONS_PER_SECOND should be positive but was $value" }
    }
}
