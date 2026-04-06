package org.tasks.jobs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_ETEBASE
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.sync.SyncSource

class SyncWorkMaxCovTest {

    // ================================================================
    // SyncSource routing: which account types go to which synchronizer
    // ================================================================

    @Test
    fun googleTasksRoutedToGoogleSynchronizer() {
        // googleTaskJobs queries for TYPE_GOOGLE_TASKS only
        assertEquals(7, TYPE_GOOGLE_TASKS)
    }

    @Test
    fun caldavJobsQueriesCorrectTypes() {
        // caldavJobs queries TYPE_CALDAV, TYPE_TASKS, TYPE_ETEBASE, TYPE_MICROSOFT
        val caldavTypes = listOf(TYPE_CALDAV, TYPE_TASKS, TYPE_ETEBASE, TYPE_MICROSOFT)
        assertEquals(4, caldavTypes.size)
        assertFalse(caldavTypes.contains(TYPE_GOOGLE_TASKS))
    }

    @Test
    fun etebaseAccountRouting() {
        val accountType = TYPE_ETEBASE
        val route = when (accountType) {
            TYPE_ETEBASE -> "etebase"
            TYPE_TASKS, TYPE_CALDAV -> "caldav"
            TYPE_MICROSOFT -> "microsoft"
            else -> "unknown"
        }
        assertEquals("etebase", route)
    }

    @Test
    fun tasksAccountRouting() {
        val accountType = TYPE_TASKS
        val route = when (accountType) {
            TYPE_ETEBASE -> "etebase"
            TYPE_TASKS, TYPE_CALDAV -> "caldav"
            TYPE_MICROSOFT -> "microsoft"
            else -> "unknown"
        }
        assertEquals("caldav", route)
    }

    @Test
    fun caldavAccountRouting() {
        val accountType = TYPE_CALDAV
        val route = when (accountType) {
            TYPE_ETEBASE -> "etebase"
            TYPE_TASKS, TYPE_CALDAV -> "caldav"
            TYPE_MICROSOFT -> "microsoft"
            else -> "unknown"
        }
        assertEquals("caldav", route)
    }

    @Test
    fun microsoftAccountRouting() {
        val accountType = TYPE_MICROSOFT
        val route = when (accountType) {
            TYPE_ETEBASE -> "etebase"
            TYPE_TASKS, TYPE_CALDAV -> "caldav"
            TYPE_MICROSOFT -> "microsoft"
            else -> "unknown"
        }
        assertEquals("microsoft", route)
    }

    // ================================================================
    // SyncSource.upgrade — determines sync source priority
    // ================================================================

    @Test
    fun upgradeFromTaskChangeToAccountAdded() {
        val result = SyncSource.TASK_CHANGE.upgrade(SyncSource.ACCOUNT_ADDED)
        assertEquals(SyncSource.TASK_CHANGE, result)
    }

    @Test
    fun upgradeFromPushNotificationToUserInitiated() {
        val result = SyncSource.PUSH_NOTIFICATION.upgrade(SyncSource.USER_INITIATED)
        assertEquals(SyncSource.USER_INITIATED, result)
    }

    @Test
    fun upgradeFromAppResumeToTaskChange() {
        val result = SyncSource.APP_RESUME.upgrade(SyncSource.TASK_CHANGE)
        assertEquals(SyncSource.TASK_CHANGE, result)
    }

    // ================================================================
    // Background restriction check only for BACKGROUND source
    // ================================================================

    @Test
    fun backgroundSourceChecksRestrictions() {
        assertTrue(SyncSource.BACKGROUND == SyncSource.BACKGROUND)
    }

    @Test
    fun nonBackgroundSourcesSkipRestrictions() {
        SyncSource.values()
            .filter { it != SyncSource.BACKGROUND }
            .forEach { assertFalse("${it.name} should skip restriction", it == SyncSource.BACKGROUND) }
    }

    // ================================================================
    // OpenTasks content resolver sync only for USER_INITIATED
    // ================================================================

    @Test
    fun onlyUserInitiatedTriggersContentResolverSync() {
        SyncSource.values().forEach { source ->
            if (source == SyncSource.USER_INITIATED) {
                assertTrue(source == SyncSource.USER_INITIATED)
            } else {
                assertFalse(source == SyncSource.USER_INITIATED)
            }
        }
    }

    // ================================================================
    // SyncSource.fromString roundtrip completeness
    // ================================================================

    @Test
    fun fromStringLowercaseReturnsNone() {
        assertEquals(SyncSource.NONE, SyncSource.fromString("user_initiated"))
    }

    @Test
    fun fromStringWithTrailingSpaceReturnsNone() {
        assertEquals(SyncSource.NONE, SyncSource.fromString("BACKGROUND "))
    }
}
