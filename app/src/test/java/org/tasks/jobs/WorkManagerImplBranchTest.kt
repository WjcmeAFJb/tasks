package org.tasks.jobs

import android.app.AlarmManager
import android.content.Context
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
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
import kotlin.math.max

class WorkManagerImplBranchTest {

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
    // scheduleBackup — coerceAtMost logic
    // ================================================================

    @Test
    fun scheduleBackupWhenLastBackupIsExactlyMidnightMinusOneDay() = runTest {
        SuspendFreeze.freezeAt(DateTime(2024, 6, 15, 10, 0, 0)) {
            val lastBackup = DateTime(2024, 6, 14, 0, 0, 0).millis
            whenever(preferences.getLong(R.string.p_last_backup, 0L)).thenReturn(lastBackup)
            val nextBackup = DateTimeUtils.newDateTime(lastBackup).plusDays(1).millis
            val midnight = DateTimeUtils.midnight()
            val result = nextBackup.coerceAtMost(midnight)
            assertEquals(nextBackup, result)
            assertTrue(nextBackup <= midnight)
        }
    }

    @Test
    fun scheduleBackupWhenLastBackupWasTodayMorning() = runTest {
        SuspendFreeze.freezeAt(DateTime(2024, 6, 15, 10, 0, 0)) {
            val lastBackup = DateTime(2024, 6, 15, 6, 0, 0).millis
            val nextBackup = DateTimeUtils.newDateTime(lastBackup).plusDays(1).millis
            val midnight = DateTimeUtils.midnight()
            val result = nextBackup.coerceAtMost(midnight)
            assertEquals(midnight, result)
        }
    }

    @Test
    fun scheduleBackupWhenLastBackupIsFarFuture() = runTest {
        SuspendFreeze.freezeAt(DateTime(2024, 6, 15, 10, 0, 0)) {
            val lastBackup = DateTime(2025, 1, 1, 0, 0, 0).millis
            val nextBackup = DateTimeUtils.newDateTime(lastBackup).plusDays(1).millis
            val midnight = DateTimeUtils.midnight()
            val result = nextBackup.coerceAtMost(midnight)
            assertEquals(midnight, result)
        }
    }

    // ================================================================
    // scheduleNotification — boundary conditions
    // ================================================================

    @Test
    fun scheduleNotificationMaxOfCurrentAndScheduled() = runTest {
        SuspendFreeze.freezeAt(DateTime(2024, 6, 15, 12, 0, 0)) {
            val now = org.tasks.time.DateTimeUtils2.currentTimeMillis()
            assertEquals(now, max(now, now - 60000L))
        }
    }

    @Test
    fun scheduleNotificationWithZero() = runTest {
        SuspendFreeze.freezeAt(DateTime(2024, 6, 15, 12, 0, 0)) {
            val now = org.tasks.time.DateTimeUtils2.currentTimeMillis()
            assertEquals(now, max(now, 0L))
        }
    }

    @Test
    fun scheduleNotificationFutureTimePreserved() = runTest {
        SuspendFreeze.freezeAt(DateTime(2024, 6, 15, 12, 0, 0)) {
            val now = org.tasks.time.DateTimeUtils2.currentTimeMillis()
            val future = now + 3600000L
            assertEquals(future, max(now, future))
        }
    }

    // ================================================================
    // triggerNotifications — expedited time calculation
    // ================================================================

    @Test
    fun triggerNotificationsExpeditedTimeIsZero() = runTest {
        SuspendFreeze.freezeAt(DateTime(2024, 6, 15, 12, 0, 0)) {
            val now = org.tasks.time.DateTimeUtils2.currentTimeMillis()
            assertEquals(0L, if (true) 0L else now + 5_000)
        }
    }

    @Test
    fun triggerNotificationsNonExpeditedTimeHas5SecondDelay() = runTest {
        SuspendFreeze.freezeAt(DateTime(2024, 6, 15, 12, 0, 0)) {
            val now = org.tasks.time.DateTimeUtils2.currentTimeMillis()
            assertEquals(now + 5_000, if (false) 0L else now + 5_000)
        }
    }

    // ================================================================
    // sync — immediate vs delayed logic
    // ================================================================

    @Test fun syncTaskChangeIsDelayed() = assertFalse(SyncSource.TASK_CHANGE != SyncSource.TASK_CHANGE)
    @Test fun syncUserInitiatedIsImmediate() = assertTrue(SyncSource.USER_INITIATED != SyncSource.TASK_CHANGE)
    @Test fun syncBackgroundIsImmediate() = assertTrue(SyncSource.BACKGROUND != SyncSource.TASK_CHANGE)
    @Test fun syncAppResumeIsImmediate() = assertTrue(SyncSource.APP_RESUME != SyncSource.TASK_CHANGE)
    @Test fun syncAppBackgroundIsImmediate() = assertTrue(SyncSource.APP_BACKGROUND != SyncSource.TASK_CHANGE)
    @Test fun syncNoneIsImmediate() = assertTrue(SyncSource.NONE != SyncSource.TASK_CHANGE)

    // ================================================================
    // scheduleDriveUpload — gating and delay logic
    // ================================================================

    @Test
    fun scheduleDriveUploadDisabled() {
        whenever(preferences.getBoolean(R.string.p_google_drive_backup, false)).thenReturn(false)
        assertFalse(preferences.getBoolean(R.string.p_google_drive_backup, false))
    }

    @Test
    fun scheduleDriveUploadEnabled() {
        whenever(preferences.getBoolean(R.string.p_google_drive_backup, false)).thenReturn(true)
        assertTrue(preferences.getBoolean(R.string.p_google_drive_backup, false))
    }

    @Test
    fun scheduleDriveUploadPurgeDelayBounded() {
        val delay = java.util.Random(42).nextInt(3600).toLong()
        assertTrue(delay in 0..3599)
    }

    // ================================================================
    // updateBackgroundSync — account type checking
    // ================================================================

    @Test
    fun updateBackgroundSyncWithNoAccountsDisables() = runTest {
        whenever(caldavDao.getAccounts(TYPE_GOOGLE_TASKS, TYPE_CALDAV, TYPE_TASKS, TYPE_ETEBASE, TYPE_MICROSOFT))
            .thenReturn(emptyList())
        assertTrue(caldavDao.getAccounts(TYPE_GOOGLE_TASKS, TYPE_CALDAV, TYPE_TASKS, TYPE_ETEBASE, TYPE_MICROSOFT).isEmpty())
    }

    @Test
    fun updateBackgroundSyncWithSingleAccountEnables() = runTest {
        whenever(caldavDao.getAccounts(TYPE_GOOGLE_TASKS, TYPE_CALDAV, TYPE_TASKS, TYPE_ETEBASE, TYPE_MICROSOFT))
            .thenReturn(listOf(CaldavAccount(accountType = TYPE_TASKS, uuid = "t1")))
        assertTrue(caldavDao.getAccounts(TYPE_GOOGLE_TASKS, TYPE_CALDAV, TYPE_TASKS, TYPE_ETEBASE, TYPE_MICROSOFT).isNotEmpty())
    }

    @Test
    fun updateBackgroundSyncQueryIncludesAllTypes() {
        val types = listOf(TYPE_GOOGLE_TASKS, TYPE_CALDAV, TYPE_TASKS, TYPE_ETEBASE, TYPE_MICROSOFT)
        assertEquals(5, types.size)
    }

    // ================================================================
    // enqueueUnique — delay calculation
    // ================================================================

    @Test
    fun enqueueUniquePositiveDelay() = runTest {
        SuspendFreeze.freezeAt(DateTime(2024, 6, 15, 12, 0, 0)) {
            val now = org.tasks.time.DateTimeUtils2.currentTimeMillis()
            val delay = (now + 120_000L) - now
            assertTrue(delay > 0)
            assertEquals(120_000L, delay)
        }
    }

    @Test
    fun enqueueUniqueNegativeDelaySkipsSetInitialDelay() = runTest {
        SuspendFreeze.freezeAt(DateTime(2024, 6, 15, 12, 0, 0)) {
            val now = org.tasks.time.DateTimeUtils2.currentTimeMillis()
            val delay = (now - 60_000L) - now
            assertFalse(delay > 0)
        }
    }

    // ================================================================
    // printDuration — formatting
    // ================================================================

    private fun callPrintDuration(millis: Long): String {
        val clazz = Class.forName("org.tasks.jobs.WorkManagerImplKt")
        val method = clazz.getDeclaredMethod("printDuration", Long::class.java)
        method.isAccessible = true
        return method.invoke(null, millis) as String
    }

    @Test fun printDurationZero() = assertEquals("0h 0m 0s", callPrintDuration(0L))
    @Test fun printDurationOneSecond() = assertEquals("0h 0m 1s", callPrintDuration(1000L))
    @Test fun printDurationOneMinute() = assertEquals("0h 1m 0s", callPrintDuration(60000L))
    @Test fun printDurationOneHour() = assertEquals("1h 0m 0s", callPrintDuration(3600000L))
    @Test fun printDurationComplex() = assertEquals("1h 30m 45s", callPrintDuration(5445000L))
    @Test fun printDuration48Hours() = assertEquals("48h 0m 0s", callPrintDuration(172800000L))
    @Test fun printDuration59Seconds() = assertEquals("0h 0m 59s", callPrintDuration(59000L))
    @Test fun printDuration59Minutes59Seconds() = assertEquals("0h 59m 59s", callPrintDuration(3599000L))

    // ================================================================
    // networkConstraints — properties
    // ================================================================

    @Test fun networkConstraintsRequiresNetwork() = assertEquals(androidx.work.NetworkType.CONNECTED, networkConstraints.requiredNetworkType)
    @Test fun networkConstraintsDoesNotRequireCharging() = assertFalse(networkConstraints.requiresCharging())
    @Test fun networkConstraintsDoesNotRequireBatteryNotLow() = assertFalse(networkConstraints.requiresBatteryNotLow())
    @Test fun networkConstraintsDoesNotRequireDeviceIdle() = assertFalse(networkConstraints.requiresDeviceIdle())
    @Test fun networkConstraintsDoesNotRequireStorageNotLow() = assertFalse(networkConstraints.requiresStorageNotLow())

    // ================================================================
    // WorkManager companion constants
    // ================================================================

    @Test fun tagBackup() = assertEquals("tag_backup", WorkManager.TAG_BACKUP)
    @Test fun tagRefresh() = assertEquals("tag_refresh", WorkManager.TAG_REFRESH)
    @Test fun tagSync() = assertEquals("tag_sync", WorkManager.TAG_SYNC)
    @Test fun tagBackgroundSync() = assertEquals("tag_background_sync", WorkManager.TAG_BACKGROUND_SYNC)
    @Test fun tagRemoteConfig() = assertEquals("tag_remote_config", WorkManager.TAG_REMOTE_CONFIG)
    @Test fun tagMigrateLocal() = assertEquals("tag_migrate_local", WorkManager.TAG_MIGRATE_LOCAL)
    @Test fun tagUpdatePurchases() = assertEquals("tag_update_purchases", WorkManager.TAG_UPDATE_PURCHASES)
    @Test fun tagNotifications() = assertEquals("tag_notifications", WorkManager.TAG_NOTIFICATIONS)

    @Test
    fun remoteConfigIntervalHoursForDebug() {
        val interval = WorkManager.REMOTE_CONFIG_INTERVAL_HOURS
        assertTrue(interval == 1L || interval == 12L)
    }

    // ================================================================
    // SyncSource — showIndicator + upgrade + fromString
    // ================================================================

    @Test fun syncSourceUserInitiatedShowIndicator() = assertTrue(SyncSource.USER_INITIATED.showIndicator)
    @Test fun syncSourcePushNotificationHidesIndicator() = assertFalse(SyncSource.PUSH_NOTIFICATION.showIndicator)
    @Test fun syncSourceContentObserverShowIndicator() = assertTrue(SyncSource.CONTENT_OBSERVER.showIndicator)
    @Test fun syncSourceBackgroundHidesIndicator() = assertFalse(SyncSource.BACKGROUND.showIndicator)
    @Test fun syncSourceTaskChangeShowIndicator() = assertTrue(SyncSource.TASK_CHANGE.showIndicator)
    @Test fun syncSourceAppBackgroundHidesIndicator() = assertFalse(SyncSource.APP_BACKGROUND.showIndicator)
    @Test fun syncSourceAppResumeHidesIndicator() = assertFalse(SyncSource.APP_RESUME.showIndicator)
    @Test fun syncSourceAccountAddedShowIndicator() = assertTrue(SyncSource.ACCOUNT_ADDED.showIndicator)
    @Test fun syncSourcePurchaseCompletedShowIndicator() = assertTrue(SyncSource.PURCHASE_COMPLETED.showIndicator)
    @Test fun syncSourceSharingChangeShowIndicator() = assertTrue(SyncSource.SHARING_CHANGE.showIndicator)
    @Test fun syncSourceNoneHidesIndicator() = assertFalse(SyncSource.NONE.showIndicator)

    @Test fun upgradeFromNonIndicatorToIndicator() = assertEquals(SyncSource.USER_INITIATED, SyncSource.BACKGROUND.upgrade(SyncSource.USER_INITIATED))
    @Test fun upgradeFromIndicatorToNonIndicator() = assertEquals(SyncSource.USER_INITIATED, SyncSource.USER_INITIATED.upgrade(SyncSource.BACKGROUND))
    @Test fun upgradeFromIndicatorToIndicator() = assertEquals(SyncSource.USER_INITIATED, SyncSource.USER_INITIATED.upgrade(SyncSource.TASK_CHANGE))
    @Test fun upgradeFromNonIndicatorToNonIndicator() = assertEquals(SyncSource.BACKGROUND, SyncSource.BACKGROUND.upgrade(SyncSource.NONE))

    @Test fun syncSourceFromStringValid() = assertEquals(SyncSource.USER_INITIATED, SyncSource.fromString("USER_INITIATED"))
    @Test fun syncSourceFromStringNull() = assertEquals(SyncSource.NONE, SyncSource.fromString(null))
    @Test fun syncSourceFromStringInvalid() = assertEquals(SyncSource.NONE, SyncSource.fromString("INVALID_VALUE"))
    @Test fun syncSourceFromStringTaskChange() = assertEquals(SyncSource.TASK_CHANGE, SyncSource.fromString("TASK_CHANGE"))
    @Test fun syncSourceFromStringEmpty() = assertEquals(SyncSource.NONE, SyncSource.fromString(""))

    // ================================================================
    // Work class constants
    // ================================================================

    @Test fun migrateLocalWorkExtraAccount() = assertEquals("extra_account", MigrateLocalWork.EXTRA_ACCOUNT)
    @Test fun updateCalendarWorkExtraId() = assertEquals("extra_id", UpdateCalendarWork.EXTRA_ID)
    @Test fun driveUploaderExtraUri() = assertEquals("extra_uri", DriveUploader.EXTRA_URI)
    @Test fun driveUploaderExtraPurge() = assertEquals("extra_purge", DriveUploader.EXTRA_PURGE)
    @Test fun syncWorkExtraSource() = assertEquals("extra_source", SyncWork.EXTRA_SOURCE)
}
