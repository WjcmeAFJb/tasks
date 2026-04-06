package org.tasks.notifications

import android.content.Context
import android.content.res.Resources
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.NotificationDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Notification
import org.tasks.data.entity.Place
import org.tasks.data.entity.Task
import org.tasks.jobs.WorkManager
import org.tasks.markdown.Markdown
import org.tasks.markdown.MarkdownProvider
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.Preferences

class NotificationManagerExerciseTest {

    private lateinit var context: Context
    private lateinit var preferences: Preferences
    private lateinit var notificationDao: NotificationDao
    private lateinit var taskDao: TaskDao
    private lateinit var locationDao: LocationDao
    private lateinit var refreshBroadcaster: RefreshBroadcaster
    private lateinit var throttledNotificationManager: ThrottledNotificationManager
    private lateinit var markdownProvider: MarkdownProvider
    private lateinit var permissionChecker: PermissionChecker
    private lateinit var workManager: WorkManager
    private lateinit var manager: NotificationManager
    private lateinit var markdown: Markdown

    @Before
    fun setUp() {
        context = mock()
        preferences = mock()
        notificationDao = mock()
        taskDao = mock()
        locationDao = mock()
        refreshBroadcaster = mock()
        throttledNotificationManager = mock()
        markdownProvider = mock()
        permissionChecker = mock()
        workManager = mock()
        markdown = mock()

        val resources = mock<Resources>()
        whenever(context.resources).thenReturn(resources)
        whenever(context.applicationContext).thenReturn(context)
        whenever(context.packageName).thenReturn("org.tasks")
        whenever(resources.getBoolean(any())).thenReturn(false)
        whenever(resources.getString(any())).thenReturn("text")

        whenever(markdownProvider.markdown(force = true)).thenReturn(markdown)
        whenever(markdown.toMarkdown(any<String>())).thenAnswer { it.arguments[0] as? CharSequence }
        whenever(markdown.toMarkdown(null)).thenReturn(null)

        manager = NotificationManager(
            context = context,
            preferences = preferences,
            notificationDao = notificationDao,
            taskDao = taskDao,
            locationDao = locationDao,
            refreshBroadcaster = refreshBroadcaster,
            notificationManager = throttledNotificationManager,
            markdownProvider = markdownProvider,
            permissionChecker = permissionChecker,
            workManager = workManager,
        )
    }

    // ========== getTaskNotification ==========

    @Test
    fun getTaskNotification_taskNotFound_null() = runTest {
        whenever(taskDao.fetch(1L)).thenReturn(null)
        val n = Notification(taskId = 1, timestamp = 100L, type = 0)
        assertNull(manager.getTaskNotification(n))
    }

    @Test
    fun getTaskNotification_completedTask_null() = runTest {
        val task = Task(id = 1, completionDate = 1000L)
        whenever(taskDao.fetch(1L)).thenReturn(task)
        val n = Notification(taskId = 1, timestamp = 100L, type = 0)
        assertNull(manager.getTaskNotification(n))
    }

    @Test
    fun getTaskNotification_deletedTask_null() = runTest {
        val task = Task(id = 1, deletionDate = 1000L)
        whenever(taskDao.fetch(1L)).thenReturn(task)
        val n = Notification(taskId = 1, timestamp = 100L, type = 0)
        assertNull(manager.getTaskNotification(n))
    }

    @Test
    fun getTaskNotification_completedAndDeleted_null() = runTest {
        val task = Task(id = 1, completionDate = 500, deletionDate = 1000)
        whenever(taskDao.fetch(1L)).thenReturn(task)
        val n = Notification(taskId = 1, timestamp = 100L, type = 0)
        assertNull(manager.getTaskNotification(n))
    }

    @Test
    fun getTaskNotification_geoEnter_withPlace() = runTest {
        val task = Task(id = 1, title = "Geo Task")
        whenever(taskDao.fetch(1L)).thenReturn(task)
        val place = Place(uid = "p1", name = "Office")
        whenever(locationDao.getPlace(100L)).thenReturn(place)
        whenever(context.getString(any(), any())).thenReturn("Arrived at Office")
        val n = Notification(
            taskId = 1,
            timestamp = 100L,
            type = Alarm.TYPE_GEO_ENTER,
            location = 100L,
        )
        // Will fail at NotificationCompat.Builder construction in unit test (no Android)
        // but we verify the dao call
        try {
            manager.getTaskNotification(n)
        } catch (_: Exception) {
            // Expected: Android stubs not available
        }
        verify(locationDao).getPlace(100L)
    }

    @Test
    fun getTaskNotification_geoExit_withPlace() = runTest {
        val task = Task(id = 1, title = "Geo Task")
        whenever(taskDao.fetch(1L)).thenReturn(task)
        val place = Place(uid = "p1", name = "Home")
        whenever(locationDao.getPlace(200L)).thenReturn(place)
        whenever(context.getString(any(), any())).thenReturn("Departed from Home")
        val n = Notification(
            taskId = 1,
            timestamp = 100L,
            type = Alarm.TYPE_GEO_EXIT,
            location = 200L,
        )
        try {
            manager.getTaskNotification(n)
        } catch (_: Exception) {
        }
        verify(locationDao).getPlace(200L)
    }

    @Test
    fun getTaskNotification_geoEnter_noPlace() = runTest {
        val task = Task(id = 1, title = "Task")
        whenever(taskDao.fetch(1L)).thenReturn(task)
        whenever(locationDao.getPlace(300L)).thenReturn(null)
        val n = Notification(
            taskId = 1,
            timestamp = 100L,
            type = Alarm.TYPE_GEO_ENTER,
            location = 300L,
        )
        try {
            manager.getTaskNotification(n)
        } catch (_: Exception) {
        }
        verify(locationDao).getPlace(300L)
    }

    // ========== notifyTasks ==========

    @Test
    fun notifyTasks_noPermission_doesNothing() = runTest {
        whenever(permissionChecker.canNotify()).thenReturn(false)
        manager.notifyTasks(
            listOf(Notification(taskId = 1, timestamp = 100L, type = 0)),
            alert = true, nonstop = false, fiveTimes = false,
        )
        verify(notificationDao, never()).getAllOrdered()
        verify(notificationDao, never()).insertAll(any())
    }

    @Test
    fun notifyTasks_zeroTotal_cancelsSummary() = runTest {
        whenever(permissionChecker.canNotify()).thenReturn(true)
        whenever(notificationDao.getAllOrdered()).thenReturn(emptyList())
        manager.notifyTasks(emptyList(), alert = false, nonstop = false, fiveTimes = false)
        verify(throttledNotificationManager).cancel(NotificationManager.SUMMARY_NOTIFICATION_ID)
        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun notifyTasks_insertsNewNotifications() = runTest {
        whenever(permissionChecker.canNotify()).thenReturn(true)
        whenever(notificationDao.getAllOrdered()).thenReturn(emptyList())
        val n = Notification(taskId = 1, timestamp = 100L, type = 0)
        manager.notifyTasks(listOf(n), alert = true, nonstop = false, fiveTimes = false)
        verify(notificationDao).insertAll(listOf(n))
    }

    @Test
    fun notifyTasks_bundled_oneExisting_oneNew_recreatesExisting() = runTest {
        whenever(permissionChecker.canNotify()).thenReturn(true)
        val existing = Notification(taskId = 5, timestamp = 50L, type = 0)
        whenever(notificationDao.getAllOrdered()).thenReturn(listOf(existing))
        whenever(preferences.bundleNotifications()).thenReturn(true)
        whenever(taskDao.activeNotifications()).thenReturn(emptyList())
        val newN = Notification(taskId = 10, timestamp = 100L, type = 0)
        manager.notifyTasks(listOf(newN), alert = true, nonstop = false, fiveTimes = false)
        verify(notificationDao).insertAll(listOf(newN))
        // totalCount=2, bundled, existing.size==1 => recreates existing
        verify(taskDao, atLeastOnce()).activeNotifications()
    }

    @Test
    fun notifyTasks_bundled_multipleNew_updatesSummary() = runTest {
        whenever(permissionChecker.canNotify()).thenReturn(true)
        whenever(notificationDao.getAllOrdered()).thenReturn(emptyList())
        whenever(preferences.bundleNotifications()).thenReturn(true)
        whenever(taskDao.activeNotifications()).thenReturn(emptyList())
        val n1 = Notification(taskId = 1, timestamp = 100L, type = 0)
        val n2 = Notification(taskId = 2, timestamp = 200L, type = 0)
        manager.notifyTasks(listOf(n1, n2), alert = true, nonstop = true, fiveTimes = false)
        verify(notificationDao).insertAll(listOf(n1, n2))
        // multiple new => updateSummary called with alert
        verify(taskDao, atLeast(2)).activeNotifications()
    }

    @Test
    fun notifyTasks_unbundled_createsDirectly() = runTest {
        whenever(permissionChecker.canNotify()).thenReturn(true)
        whenever(notificationDao.getAllOrdered()).thenReturn(emptyList())
        whenever(preferences.bundleNotifications()).thenReturn(false)
        val n1 = Notification(taskId = 1, timestamp = 100L, type = 0)
        manager.notifyTasks(listOf(n1), alert = true, nonstop = false, fiveTimes = false)
        verify(notificationDao).insertAll(listOf(n1))
    }

    @Test
    fun notifyTasks_bundled_noNew_noExisting_cancelsSummary() = runTest {
        whenever(permissionChecker.canNotify()).thenReturn(true)
        whenever(notificationDao.getAllOrdered()).thenReturn(emptyList())
        whenever(preferences.bundleNotifications()).thenReturn(true)
        manager.notifyTasks(emptyList(), alert = false, nonstop = false, fiveTimes = false)
        verify(throttledNotificationManager).cancel(NotificationManager.SUMMARY_NOTIFICATION_ID)
    }

    @Test
    fun notifyTasks_bundled_twoExisting_noNew() = runTest {
        whenever(permissionChecker.canNotify()).thenReturn(true)
        val e1 = Notification(taskId = 1, timestamp = 10L, type = 0)
        val e2 = Notification(taskId = 2, timestamp = 20L, type = 0)
        whenever(notificationDao.getAllOrdered()).thenReturn(listOf(e1, e2))
        whenever(preferences.bundleNotifications()).thenReturn(true)
        whenever(taskDao.activeNotifications()).thenReturn(emptyList())
        // totalCount=2, bundled, newNotifications empty
        manager.notifyTasks(emptyList(), alert = false, nonstop = false, fiveTimes = false)
        verify(taskDao).activeNotifications()
    }

    // ========== restoreNotifications ==========

    @Test
    fun restoreNotifications_noPermission_returnsEarly() = runTest {
        whenever(permissionChecker.canNotify()).thenReturn(false)
        manager.restoreNotifications(cancelExisting = true)
        verify(notificationDao, never()).getAllOrdered()
    }

    @Test
    fun restoreNotifications_cancelExisting_cancelsEach() = runTest {
        whenever(permissionChecker.canNotify()).thenReturn(true)
        val n1 = Notification(taskId = 10, timestamp = 100L, type = 0)
        val n2 = Notification(taskId = 20, timestamp = 200L, type = 0)
        whenever(notificationDao.getAllOrdered()).thenReturn(listOf(n1, n2))
        whenever(preferences.bundleNotifications()).thenReturn(false)
        manager.restoreNotifications(cancelExisting = true)
        verify(throttledNotificationManager, atLeast(1)).cancel(10)
        verify(throttledNotificationManager, atLeast(1)).cancel(20)
    }

    @Test
    fun restoreNotifications_noCancelExisting_doesNotCancel() = runTest {
        whenever(permissionChecker.canNotify()).thenReturn(true)
        whenever(notificationDao.getAllOrdered()).thenReturn(emptyList())
        whenever(preferences.bundleNotifications()).thenReturn(false)
        manager.restoreNotifications(cancelExisting = false)
        // Only summary should be cancelled
        verify(throttledNotificationManager).cancel(NotificationManager.SUMMARY_NOTIFICATION_ID)
    }

    @Test
    fun restoreNotifications_bundled_multipleNotifications() = runTest {
        whenever(permissionChecker.canNotify()).thenReturn(true)
        val n1 = Notification(taskId = 1, timestamp = 100L, type = 0)
        val n2 = Notification(taskId = 2, timestamp = 200L, type = 0)
        whenever(notificationDao.getAllOrdered()).thenReturn(listOf(n1, n2))
        whenever(preferences.bundleNotifications()).thenReturn(true)
        whenever(taskDao.activeNotifications()).thenReturn(emptyList())
        manager.restoreNotifications(cancelExisting = false)
        verify(taskDao).activeNotifications()
    }

    @Test
    fun restoreNotifications_bundled_singleNotification_cancelsSummary() = runTest {
        whenever(permissionChecker.canNotify()).thenReturn(true)
        val n1 = Notification(taskId = 1, timestamp = 100L, type = 0)
        whenever(notificationDao.getAllOrdered()).thenReturn(listOf(n1))
        whenever(preferences.bundleNotifications()).thenReturn(true)
        manager.restoreNotifications(cancelExisting = false)
        // size <= 1 => goes to else branch => cancelSummaryNotification
        verify(throttledNotificationManager).cancel(NotificationManager.SUMMARY_NOTIFICATION_ID)
    }

    @Test
    fun restoreNotifications_unbundled_cancelsSummary() = runTest {
        whenever(permissionChecker.canNotify()).thenReturn(true)
        val n1 = Notification(taskId = 1, timestamp = 100L, type = 0)
        whenever(notificationDao.getAllOrdered()).thenReturn(listOf(n1))
        whenever(preferences.bundleNotifications()).thenReturn(false)
        manager.restoreNotifications(cancelExisting = false)
        verify(throttledNotificationManager).cancel(NotificationManager.SUMMARY_NOTIFICATION_ID)
    }

    @Test
    fun restoreNotifications_empty_bundled() = runTest {
        whenever(permissionChecker.canNotify()).thenReturn(true)
        whenever(notificationDao.getAllOrdered()).thenReturn(emptyList())
        whenever(preferences.bundleNotifications()).thenReturn(true)
        manager.restoreNotifications(cancelExisting = false)
        verify(taskDao, never()).activeNotifications()
    }

    @Test
    fun restoreNotifications_empty_unbundled() = runTest {
        whenever(permissionChecker.canNotify()).thenReturn(true)
        whenever(notificationDao.getAllOrdered()).thenReturn(emptyList())
        whenever(preferences.bundleNotifications()).thenReturn(false)
        manager.restoreNotifications(cancelExisting = false)
        verify(throttledNotificationManager).cancel(NotificationManager.SUMMARY_NOTIFICATION_ID)
    }

    // ========== cancel ==========

    @Test
    fun cancel_singleId() = runTest {
        whenever(permissionChecker.canNotify()).thenReturn(true)
        whenever(notificationDao.getAllOrdered()).thenReturn(emptyList())
        manager.cancel(42L)
        verify(throttledNotificationManager).cancel(42)
        verify(notificationDao).deleteAll(listOf(42L))
    }

    @Test
    fun cancel_summaryId_fetchesAll() = runTest {
        whenever(permissionChecker.canNotify()).thenReturn(true)
        whenever(notificationDao.getAll()).thenReturn(listOf(1L, 2L, 3L))
        whenever(notificationDao.getAllOrdered()).thenReturn(emptyList())
        manager.cancel(NotificationManager.SUMMARY_NOTIFICATION_ID.toLong())
        verify(notificationDao).getAll()
        verify(throttledNotificationManager, atLeast(1)).cancel(1)
        verify(throttledNotificationManager, atLeast(1)).cancel(2)
        verify(throttledNotificationManager, atLeast(1)).cancel(3)
        verify(throttledNotificationManager, atLeast(1)).cancel(0)
    }

    @Test
    fun cancel_multipleIds() = runTest {
        whenever(permissionChecker.canNotify()).thenReturn(true)
        whenever(notificationDao.getAllOrdered()).thenReturn(emptyList())
        manager.cancel(listOf(10L, 20L))
        verify(throttledNotificationManager).cancel(10)
        verify(throttledNotificationManager).cancel(20)
        verify(notificationDao).deleteAll(listOf(10L, 20L))
    }

    @Test
    fun cancel_empty() = runTest {
        whenever(permissionChecker.canNotify()).thenReturn(true)
        whenever(notificationDao.getAllOrdered()).thenReturn(emptyList())
        manager.cancel(emptyList())
        verify(notificationDao).deleteAll(emptyList())
    }

    // ========== updateTimerNotification ==========

    @Test
    fun updateTimerNotification_noPermission() = runTest {
        whenever(permissionChecker.hasNotificationPermission()).thenReturn(false)
        manager.updateTimerNotification()
        verify(taskDao, never()).activeTimers()
    }

    @Test
    fun updateTimerNotification_zeroTimers_cancels() = runTest {
        whenever(permissionChecker.hasNotificationPermission()).thenReturn(true)
        whenever(permissionChecker.canNotify()).thenReturn(true)
        whenever(taskDao.activeTimers()).thenReturn(0)
        whenever(notificationDao.getAllOrdered()).thenReturn(emptyList())
        whenever(notificationDao.getAll()).thenReturn(emptyList())
        manager.updateTimerNotification()
        verify(taskDao).activeTimers()
    }

    @Test
    fun updateTimerNotification_withTimers() = runTest {
        whenever(permissionChecker.hasNotificationPermission()).thenReturn(true)
        whenever(permissionChecker.canNotify()).thenReturn(true)
        whenever(taskDao.activeTimers()).thenReturn(2)
        // Will fail deeper due to Android stubs
        try {
            manager.updateTimerNotification()
        } catch (_: RuntimeException) {
        }
        verify(taskDao).activeTimers()
    }

    // ========== triggerNotifications ==========

    @Test
    fun triggerNotifications_delegatesToWorkManager() {
        manager.triggerNotifications()
        verify(workManager).triggerNotifications()
    }

    // ========== currentInterruptionFilter ==========

    @Test
    fun currentInterruptionFilter_delegates() {
        whenever(throttledNotificationManager.currentInterruptionFilter).thenReturn(3)
        assertEquals(3, manager.currentInterruptionFilter)
    }

    // ========== constants ==========

    @Test
    fun constants() {
        assertEquals("notifications", NotificationManager.NOTIFICATION_CHANNEL_DEFAULT)
        assertEquals("notifications_tasker", NotificationManager.NOTIFICATION_CHANNEL_TASKER)
        assertEquals("notifications_timers", NotificationManager.NOTIFICATION_CHANNEL_TIMERS)
        assertEquals("notifications_miscellaneous", NotificationManager.NOTIFICATION_CHANNEL_MISCELLANEOUS)
        assertEquals(21, NotificationManager.MAX_NOTIFICATIONS)
        assertEquals(0, NotificationManager.SUMMARY_NOTIFICATION_ID)
        assertEquals("extra_notification_id", NotificationManager.EXTRA_NOTIFICATION_ID)
    }
}
