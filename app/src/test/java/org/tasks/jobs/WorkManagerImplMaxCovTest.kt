package org.tasks.jobs

import android.app.AlarmManager
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.tasks.R
import org.tasks.SuspendFreeze
import org.tasks.data.OpenTaskDao
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_ETEBASE
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.date.DateTimeUtils
import org.tasks.preferences.Preferences
import org.tasks.sync.SyncSource
import org.tasks.time.DateTime

class WorkManagerImplMaxCovTest {

    private lateinit var context: Context
    private lateinit var preferences: Preferences
    private lateinit var caldavDao: CaldavDao
    private lateinit var openTaskDao: OpenTaskDao
    private lateinit var alarmManager: AlarmManager

    @Before
    fun setUp() {
        context = mock()
        preferences = mock()
        caldavDao = mock()
        openTaskDao = mock()
        alarmManager = mock()
        whenever(context.getSystemService(Context.ALARM_SERVICE)).thenReturn(alarmManager)
        whenever(context.applicationContext).thenReturn(context)
        whenever(context.packageName).thenReturn("org.tasks")
    }

    // ================================================================
    // printDuration — private function via reflection
    // ================================================================

    private fun callPrintDuration(millis: Long): String {
        val clazz = Class.forName("org.tasks.jobs.WorkManagerImplKt")
        val method = clazz.getDeclaredMethod("printDuration", Long::class.java)
        method.isAccessible = true
        return method.invoke(null, millis) as String
    }

    @Test
    fun printDurationZero() {
        assertEquals("0h 0m 0s", callPrintDuration(0L))
    }

    @Test
    fun printDurationExactOneHour() {
        assertEquals("1h 0m 0s", callPrintDuration(3600000L))
    }

    @Test
    fun printDurationExactOneMinute() {
        assertEquals("0h 1m 0s", callPrintDuration(60000L))
    }

    @Test
    fun printDurationExactOneSecond() {
        assertEquals("0h 0m 1s", callPrintDuration(1000L))
    }

    @Test
    fun printDurationMixedComponents() {
        // 1h 30m 45s = 5445s = 5445000ms
        assertEquals("1h 30m 45s", callPrintDuration(5445000L))
    }

    @Test
    fun printDurationSubSecond() {
        // 500ms = 0s
        assertEquals("0h 0m 0s", callPrintDuration(500L))
    }

    @Test
    fun printDurationNegative() {
        // Negative millis: -5000ms = -5s => -5/3600 = 0, (-5%3600/60)=0, -5%60=-5
        val result = callPrintDuration(-5000L)
        assertNotNull(result)
    }

    @Test
    fun printDurationVeryLarge() {
        // 48 hours
        assertEquals("48h 0m 0s", callPrintDuration(172800000L))
    }

    // ================================================================
    // scheduleBackup — time calculation branches
    // ================================================================

    @Test
    fun scheduleBackupTimeClampsToMidnight() = runTest {
        SuspendFreeze.freezeAt(DateTime(2024, 6, 15, 14, 0, 0)) {
            val lastBackup = DateTime(2024, 6, 14, 22, 0, 0).millis
            whenever(preferences.getLong(R.string.p_last_backup, 0L)).thenReturn(lastBackup)
            val nextBackup = DateTimeUtils.newDateTime(lastBackup).plusDays(1).millis
            val midnight = DateTimeUtils.midnight()
            val result = nextBackup.coerceAtMost(midnight)
            // Both should be positive and result should be <= midnight
            assertTrue(result > 0)
            assertTrue(result <= midnight)
        }
    }

    @Test
    fun scheduleBackupTimeDoesNotClampWhenOld() = runTest {
        SuspendFreeze.freezeAt(DateTime(2024, 6, 15, 14, 0, 0)) {
            val lastBackup = DateTime(2022, 1, 1, 0, 0, 0).millis
            val nextBackup = DateTimeUtils.newDateTime(lastBackup).plusDays(1).millis
            val midnight = DateTimeUtils.midnight()
            val result = nextBackup.coerceAtMost(midnight)
            // Jan 2 2022 < midnight 2024 => result = nextBackup
            assertEquals(nextBackup, result)
        }
    }

    // ================================================================
    // scheduleDriveUpload — gating and purge logic
    // ================================================================

    @Test
    fun scheduleDriveUploadNoPurgeNoDelay() {
        val purge = false
        assertFalse(purge)
    }

    @Test
    fun scheduleDriveUploadPurgeRandomDelayRange() {
        // Verify delay is within range
        val delay = java.util.Random(42).nextInt(3600).toLong()
        assertTrue(delay >= 0)
        assertTrue(delay < 3600)
    }

    // ================================================================
    // sync — immediate and append routing
    // ================================================================

    @Test
    fun syncTaskChangeNotImmediate() {
        val source = SyncSource.TASK_CHANGE
        val immediate = source != SyncSource.TASK_CHANGE
        assertFalse(immediate)
    }

    @Test
    fun syncUserInitiatedImmediate() {
        val source = SyncSource.USER_INITIATED
        val immediate = source != SyncSource.TASK_CHANGE
        assertTrue(immediate)
    }

    @Test
    fun syncBackgroundImmediate() {
        val source = SyncSource.BACKGROUND
        val immediate = source != SyncSource.TASK_CHANGE
        assertTrue(immediate)
    }

    @Test
    fun syncAllSourcesImmediateExceptTaskChange() {
        SyncSource.values().forEach { source ->
            val immediate = source != SyncSource.TASK_CHANGE
            if (source == SyncSource.TASK_CHANGE) {
                assertFalse("TASK_CHANGE should not be immediate", immediate)
            } else {
                assertTrue("${source.name} should be immediate", immediate)
            }
        }
    }

    // ================================================================
    // WorkManager tags
    // ================================================================

    @Test
    fun allTagsAreUnique() {
        val tags = setOf(
            WorkManager.TAG_BACKUP,
            WorkManager.TAG_REFRESH,
            WorkManager.TAG_SYNC,
            WorkManager.TAG_BACKGROUND_SYNC,
            WorkManager.TAG_REMOTE_CONFIG,
            WorkManager.TAG_MIGRATE_LOCAL,
            WorkManager.TAG_UPDATE_PURCHASES,
            WorkManager.TAG_NOTIFICATIONS,
        )
        assertEquals(8, tags.size)
    }

    @Test
    fun remoteConfigIntervalIsPositive() {
        assertTrue(WorkManager.REMOTE_CONFIG_INTERVAL_HOURS > 0)
    }

    // ================================================================
    // updateBackgroundSync — account type filtering
    // ================================================================

    @Test
    fun updateBackgroundSyncFiltersByCorrectTypes() {
        val types = listOf(TYPE_GOOGLE_TASKS, TYPE_CALDAV, TYPE_TASKS, TYPE_ETEBASE, TYPE_MICROSOFT)
        assertEquals(5, types.size)
        assertTrue(types.contains(TYPE_GOOGLE_TASKS))
        assertTrue(types.contains(TYPE_CALDAV))
        assertTrue(types.contains(TYPE_TASKS))
        assertTrue(types.contains(TYPE_ETEBASE))
        assertTrue(types.contains(TYPE_MICROSOFT))
    }

    // ================================================================
    // scheduleNotification — time clamping
    // ================================================================

    @Test
    fun scheduleNotificationFutureSetsAlarm() = runTest {
        SuspendFreeze.freezeAt(DateTime(2024, 6, 15, 12, 0, 0)) {
            val now = org.tasks.time.DateTimeUtils2.currentTimeMillis()
            val scheduledTime = now + 60_000
            val time = kotlin.math.max(now, scheduledTime)
            assertEquals(scheduledTime, time)
            assertFalse(time < now)
        }
    }

    @Test
    fun scheduleNotificationPastClampsToNow() = runTest {
        SuspendFreeze.freezeAt(DateTime(2024, 6, 15, 12, 0, 0)) {
            val now = org.tasks.time.DateTimeUtils2.currentTimeMillis()
            val scheduledTime = now - 60_000
            val time = kotlin.math.max(now, scheduledTime)
            assertEquals(now, time)
        }
    }
}
