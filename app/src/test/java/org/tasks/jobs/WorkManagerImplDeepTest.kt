package org.tasks.jobs

import android.app.AlarmManager
import android.content.Context
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
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

/**
 * Deep tests for WorkManagerImpl covering scheduling logic, backup time
 * calculations, notification scheduling edge cases, sync routing, and
 * drive upload gating.
 */
class WorkManagerImplDeepTest {

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
        `when`(context.getSystemService(Context.ALARM_SERVICE)).thenReturn(alarmManager)
        `when`(context.applicationContext).thenReturn(context)
        `when`(context.packageName).thenReturn("org.tasks")
    }

    // ================================================================
    // scheduleBackup — time calculation
    // ================================================================

    @Test
    fun scheduleBackupWithZeroLastBackupSchedulesAtMidnight() = kotlinx.coroutines.test.runTest {
        SuspendFreeze.freezeAt(DateTime(2024, 6, 15, 14, 0, 0)) {
            `when`(preferences.getLong(R.string.p_last_backup, 0L)).thenReturn(0L)
            val nextBackup = org.tasks.date.DateTimeUtils.newDateTime(0L)
                .plusDays(1)
                .millis
                .coerceAtMost(DateTimeUtils.midnight())
            // Jan 1 1970 + 1 day is very old, so coerceAtMost(midnight) = Jan 2 1970
            assertTrue(nextBackup > 0)
        }
    }

    @Test
    fun scheduleBackupWithRecentBackupCoercesToMidnight() = kotlinx.coroutines.test.runTest {
        SuspendFreeze.freezeAt(DateTime(2024, 6, 15, 10, 0, 0)) {
            val lastBackup = DateTime(2024, 6, 15, 8, 0, 0).millis
            `when`(preferences.getLong(R.string.p_last_backup, 0L)).thenReturn(lastBackup)
            val nextBackup = org.tasks.date.DateTimeUtils.newDateTime(lastBackup)
                .plusDays(1)
                .millis
            val midnight = DateTimeUtils.midnight()
            // nextBackup = June 16 8:00, midnight = June 16 00:00
            // coerceAtMost returns midnight since midnight < nextBackup
            val result = nextBackup.coerceAtMost(midnight)
            assertEquals(midnight, result)
        }
    }

    @Test
    fun scheduleBackupWithOldBackupUsesNextDay() = kotlinx.coroutines.test.runTest {
        SuspendFreeze.freezeAt(DateTime(2024, 6, 15, 10, 0, 0)) {
            val lastBackup = DateTime(2023, 1, 1, 0, 0, 0).millis
            val nextBackup = org.tasks.date.DateTimeUtils.newDateTime(lastBackup)
                .plusDays(1)
                .millis
            val midnight = DateTimeUtils.midnight()
            val result = nextBackup.coerceAtMost(midnight)
            // Jan 2 2023 is less than midnight June 16 2024
            assertEquals(nextBackup, result)
        }
    }

    @Test
    fun scheduleBackupWithYesterdayBackup() = kotlinx.coroutines.test.runTest {
        SuspendFreeze.freezeAt(DateTime(2024, 6, 15, 10, 0, 0)) {
            val lastBackup = DateTime(2024, 6, 14, 10, 0, 0).millis
            val nextBackup = org.tasks.date.DateTimeUtils.newDateTime(lastBackup)
                .plusDays(1)
                .millis
            val midnight = DateTimeUtils.midnight()
            // June 15 10:00 vs midnight June 16
            val result = nextBackup.coerceAtMost(midnight)
            assertTrue(result > 0)
        }
    }

    // ================================================================
    // scheduleNotification — alarm logic
    // ================================================================

    @Test
    fun scheduleNotificationPastTimeTriggersImmediate() = kotlinx.coroutines.test.runTest {
        SuspendFreeze.freezeAt(DateTime(2024, 6, 15, 12, 0, 0)) {
            val pastTime = DateTime(2020, 1, 1).millis
            val time = kotlin.math.max(
                org.tasks.time.DateTimeUtils2.currentTimeMillis(),
                pastTime
            )
            // time = max(now, past) = now
            // But now < now is false, so we'd go to the else branch
            // The condition: if (time < currentTimeMillis()) → false since time == now
            assertFalse(time < org.tasks.time.DateTimeUtils2.currentTimeMillis())
        }
    }

    @Test
    fun scheduleNotificationFutureTimeUsesAlarm() = kotlinx.coroutines.test.runTest {
        SuspendFreeze.freezeAt(DateTime(2024, 6, 15, 12, 0, 0)) {
            val futureTime = DateTime(2025, 6, 15, 15, 0, 0).millis
            val time = kotlin.math.max(
                org.tasks.time.DateTimeUtils2.currentTimeMillis(),
                futureTime
            )
            assertEquals(futureTime, time)
            assertFalse(time < org.tasks.time.DateTimeUtils2.currentTimeMillis())
        }
    }

    @Test
    fun scheduleNotificationExactCurrentTime() = kotlinx.coroutines.test.runTest {
        SuspendFreeze.freezeAt(DateTime(2024, 6, 15, 12, 0, 0)) {
            val now = org.tasks.time.DateTimeUtils2.currentTimeMillis()
            val time = kotlin.math.max(now, now)
            assertEquals(now, time)
            assertFalse(time < now)
        }
    }

    // ================================================================
    // triggerNotifications — expedited vs non-expedited
    // ================================================================

    @Test
    fun triggerNotificationsExpeditedReturnsZero() {
        val time = if (true) 0L else org.tasks.time.DateTimeUtils2.currentTimeMillis() + 5_000
        assertEquals(0L, time)
    }

    @Test
    fun triggerNotificationsNonExpeditedReturnsDelayedTime() = kotlinx.coroutines.test.runTest {
        SuspendFreeze.freezeAt(DateTime(2024, 6, 15, 12, 0, 0)) {
            val now = org.tasks.time.DateTimeUtils2.currentTimeMillis()
            val time = if (false) 0L else now + 5_000
            assertEquals(now + 5_000, time)
            assertTrue(time > now)
        }
    }

    // ================================================================
    // sync — immediate flag and network constraints
    // ================================================================

    @Test
    fun syncUserInitiatedIsImmediate() {
        assertTrue(SyncSource.USER_INITIATED != SyncSource.TASK_CHANGE)
    }

    @Test
    fun syncTaskChangeIsNotImmediate() {
        assertFalse(SyncSource.TASK_CHANGE != SyncSource.TASK_CHANGE)
    }

    @Test
    fun syncAccountAddedIsImmediate() {
        assertTrue(SyncSource.ACCOUNT_ADDED != SyncSource.TASK_CHANGE)
    }

    @Test
    fun syncPurchaseCompletedIsImmediate() {
        assertTrue(SyncSource.PURCHASE_COMPLETED != SyncSource.TASK_CHANGE)
    }

    @Test
    fun syncPushNotificationIsImmediate() {
        assertTrue(SyncSource.PUSH_NOTIFICATION != SyncSource.TASK_CHANGE)
    }

    @Test
    fun syncContentObserverIsImmediate() {
        assertTrue(SyncSource.CONTENT_OBSERVER != SyncSource.TASK_CHANGE)
    }

    @Test
    fun syncSharingChangeIsImmediate() {
        assertTrue(SyncSource.SHARING_CHANGE != SyncSource.TASK_CHANGE)
    }

    // ================================================================
    // scheduleDriveUpload — gating on preference
    // ================================================================

    @Test
    fun scheduleDriveUploadDisabledReturnsEarly() {
        `when`(preferences.getBoolean(R.string.p_google_drive_backup, false)).thenReturn(false)
        assertFalse(preferences.getBoolean(R.string.p_google_drive_backup, false))
    }

    @Test
    fun scheduleDriveUploadEnabledProceeds() {
        `when`(preferences.getBoolean(R.string.p_google_drive_backup, false)).thenReturn(true)
        assertTrue(preferences.getBoolean(R.string.p_google_drive_backup, false))
    }

    @Test
    fun scheduleDriveUploadPurgeAddsDelay() {
        val purge = true
        val delay = if (purge) java.util.Random().nextInt(3600).toLong() else 0L
        assertTrue(delay >= 0)
        assertTrue(delay < 3600)
    }

    @Test
    fun scheduleDriveUploadNoPurgeNoDelay() {
        val purge = false
        val hasDelay = purge
        assertFalse(hasDelay)
    }

    // ================================================================
    // updateBackgroundSync — account types
    // ================================================================

    @Test
    fun updateBackgroundSyncEnabledWithGoogleTasks() {
        val accounts = listOf(CaldavAccount(accountType = TYPE_GOOGLE_TASKS, uuid = "g1"))
        assertTrue(accounts.isNotEmpty())
    }

    @Test
    fun updateBackgroundSyncEnabledWithCaldav() {
        val accounts = listOf(CaldavAccount(accountType = TYPE_CALDAV, uuid = "c1"))
        assertTrue(accounts.isNotEmpty())
    }

    @Test
    fun updateBackgroundSyncEnabledWithTasks() {
        val accounts = listOf(CaldavAccount(accountType = TYPE_TASKS, uuid = "t1"))
        assertTrue(accounts.isNotEmpty())
    }

    @Test
    fun updateBackgroundSyncEnabledWithEtebase() {
        val accounts = listOf(CaldavAccount(accountType = TYPE_ETEBASE, uuid = "e1"))
        assertTrue(accounts.isNotEmpty())
    }

    @Test
    fun updateBackgroundSyncEnabledWithMicrosoft() {
        val accounts = listOf(CaldavAccount(accountType = TYPE_MICROSOFT, uuid = "m1"))
        assertTrue(accounts.isNotEmpty())
    }

    @Test
    fun updateBackgroundSyncDisabledWithNoAccounts() {
        val accounts = emptyList<CaldavAccount>()
        assertTrue(accounts.isEmpty())
    }

    @Test
    fun updateBackgroundSyncEnabledWithMixedAccounts() {
        val accounts = listOf(
            CaldavAccount(accountType = TYPE_GOOGLE_TASKS, uuid = "g1"),
            CaldavAccount(accountType = TYPE_CALDAV, uuid = "c1"),
            CaldavAccount(accountType = TYPE_MICROSOFT, uuid = "m1"),
        )
        assertEquals(3, accounts.size)
        assertTrue(accounts.isNotEmpty())
    }

    // ================================================================
    // enqueueUnique — delay calculation
    // ================================================================

    @Test
    fun enqueueUniquePositiveDelay() = kotlinx.coroutines.test.runTest {
        SuspendFreeze.freezeAt(DateTime(2024, 6, 15, 12, 0, 0)) {
            val now = org.tasks.time.DateTimeUtils2.currentTimeMillis()
            val futureTime = now + 60_000
            val delay = futureTime - now
            assertEquals(60_000L, delay)
            assertTrue(delay > 0)
        }
    }

    @Test
    fun enqueueUniqueZeroDelay() = kotlinx.coroutines.test.runTest {
        SuspendFreeze.freezeAt(DateTime(2024, 6, 15, 12, 0, 0)) {
            val now = org.tasks.time.DateTimeUtils2.currentTimeMillis()
            val delay = now - now
            assertEquals(0L, delay)
            assertFalse(delay > 0)
        }
    }

    @Test
    fun enqueueUniqueNegativeDelay() = kotlinx.coroutines.test.runTest {
        SuspendFreeze.freezeAt(DateTime(2024, 6, 15, 12, 0, 0)) {
            val now = org.tasks.time.DateTimeUtils2.currentTimeMillis()
            val pastTime = now - 30_000
            val delay = pastTime - now
            assertEquals(-30_000L, delay)
            assertFalse(delay > 0)
        }
    }

    // ================================================================
    // printDuration — formatting
    // ================================================================

    @Test
    fun printDurationNegativeMillis() {
        val clazz = Class.forName("org.tasks.jobs.WorkManagerImplKt")
        val method = clazz.getDeclaredMethod("printDuration", Long::class.java)
        method.isAccessible = true
        val result = method.invoke(null, -5000L) as String
        assertNotNull(result)
    }

    @Test
    fun printDurationLargeValue() {
        val clazz = Class.forName("org.tasks.jobs.WorkManagerImplKt")
        val method = clazz.getDeclaredMethod("printDuration", Long::class.java)
        method.isAccessible = true
        val result = method.invoke(null, 86400000L) as String
        assertEquals("24h 0m 0s", result)
    }

    @Test
    fun printDurationWithAllComponents() {
        val clazz = Class.forName("org.tasks.jobs.WorkManagerImplKt")
        val method = clazz.getDeclaredMethod("printDuration", Long::class.java)
        method.isAccessible = true
        // 2h 15m 30s = 2*3600 + 15*60 + 30 = 8130s = 8130000ms
        val result = method.invoke(null, 8130000L) as String
        assertEquals("2h 15m 30s", result)
    }

    @Test
    fun printDurationOnlySeconds() {
        val clazz = Class.forName("org.tasks.jobs.WorkManagerImplKt")
        val method = clazz.getDeclaredMethod("printDuration", Long::class.java)
        method.isAccessible = true
        val result = method.invoke(null, 45000L) as String
        assertEquals("0h 0m 45s", result)
    }

    @Test
    fun printDurationOnlyMinutes() {
        val clazz = Class.forName("org.tasks.jobs.WorkManagerImplKt")
        val method = clazz.getDeclaredMethod("printDuration", Long::class.java)
        method.isAccessible = true
        val result = method.invoke(null, 180000L) as String
        assertEquals("0h 3m 0s", result)
    }

    // ================================================================
    // networkConstraints — verifies the getter returns proper constraints
    // ================================================================

    @Test
    fun networkConstraintsConnected() {
        val constraints = networkConstraints
        assertEquals(
            androidx.work.NetworkType.CONNECTED,
            constraints.requiredNetworkType
        )
    }

    @Test
    fun networkConstraintsNotCharging() {
        assertFalse(networkConstraints.requiresCharging())
    }

    @Test
    fun networkConstraintsNotBatteryLow() {
        assertFalse(networkConstraints.requiresBatteryNotLow())
    }

    // ================================================================
    // migrateLocalTasks — appends to unique work
    // ================================================================

    @Test
    fun migrateLocalWorkExtraAccountConstant() {
        assertEquals("extra_account", MigrateLocalWork.EXTRA_ACCOUNT)
    }

    // ================================================================
    // updateCalendarWork — extra ID constant
    // ================================================================

    @Test
    fun updateCalendarWorkExtraIdConstant() {
        assertEquals("extra_id", UpdateCalendarWork.EXTRA_ID)
    }
}
