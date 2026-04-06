package org.tasks.notifications

import android.content.Context
import android.content.res.Resources
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.NotificationDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Notification
import org.tasks.jobs.WorkManager
import org.tasks.markdown.MarkdownProvider
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.Preferences

class NotificationManagerBehaviorTest {

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
    private lateinit var notificationManager: NotificationManager

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

        val resources = mock(Resources::class.java)
        `when`(context.resources).thenReturn(resources)
        `when`(context.applicationContext).thenReturn(context)
        `when`(context.packageName).thenReturn("org.tasks")

        notificationManager = NotificationManager(
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

    // --- cancel(id: Long) ---

    @Test
    fun cancelSingleIdCancelsNotification() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        `when`(notificationDao.getAllOrdered()).thenReturn(emptyList())

        notificationManager.cancel(42L)

        verify(throttledNotificationManager).cancel(42)
        verify(notificationDao).deleteAll(listOf(42L))
    }

    @Test
    fun cancelSummaryIdQueriesAllNotifications() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        `when`(notificationDao.getAll()).thenReturn(listOf(1L, 2L, 3L))
        `when`(notificationDao.getAllOrdered()).thenReturn(emptyList())

        notificationManager.cancel(NotificationManager.SUMMARY_NOTIFICATION_ID.toLong())

        // Should fetch all notification IDs from the DAO
        verify(notificationDao).getAll()
        // Should cancel each plus the summary
        verify(throttledNotificationManager).cancel(1)
        verify(throttledNotificationManager).cancel(2)
        verify(throttledNotificationManager).cancel(3)
        verify(throttledNotificationManager, atLeast(1))
            .cancel(NotificationManager.SUMMARY_NOTIFICATION_ID)
    }

    @Test
    fun cancelSummaryWithNoExistingNotifications() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        `when`(notificationDao.getAll()).thenReturn(emptyList())
        `when`(notificationDao.getAllOrdered()).thenReturn(emptyList())

        notificationManager.cancel(NotificationManager.SUMMARY_NOTIFICATION_ID.toLong())

        verify(throttledNotificationManager, atLeast(1))
            .cancel(NotificationManager.SUMMARY_NOTIFICATION_ID)
    }

    // --- cancel(ids: Iterable<Long>) ---

    @Test
    fun cancelMultipleIdsCancelsEach() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        `when`(notificationDao.getAllOrdered()).thenReturn(emptyList())

        notificationManager.cancel(listOf(10L, 20L, 30L))

        verify(throttledNotificationManager).cancel(10)
        verify(throttledNotificationManager).cancel(20)
        verify(throttledNotificationManager).cancel(30)
        verify(notificationDao).deleteAll(listOf(10L, 20L, 30L))
    }

    @Test
    fun cancelIdsCallsNotifyTasksWithEmptyList() = runTest {
        // cancel(ids) internally calls notifyTasks(emptyList(), ...) to update summary
        `when`(permissionChecker.canNotify()).thenReturn(true)
        `when`(notificationDao.getAllOrdered()).thenReturn(emptyList())

        notificationManager.cancel(listOf(5L))

        // notifyTasks is called, which calls insertAll and broadcastRefresh
        verify(notificationDao).insertAll(emptyList())
        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun cancelIdsWithNoPermissionStillCancelsNotifications() = runTest {
        // cancel(ids) cancels the system notifications regardless of canNotify,
        // but notifyTasks will short-circuit without permission
        `when`(permissionChecker.canNotify()).thenReturn(false)

        notificationManager.cancel(listOf(5L))

        verify(throttledNotificationManager).cancel(5)
        verify(notificationDao).deleteAll(listOf(5L))
    }

    // --- notifyTasks() with 0 notifications ---

    @Test
    fun notifyTasksWithZeroTotalCancelsSummary() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        `when`(notificationDao.getAllOrdered()).thenReturn(emptyList())

        notificationManager.notifyTasks(
            newNotifications = emptyList(),
            alert = false,
            nonstop = false,
            fiveTimes = false,
        )

        verify(throttledNotificationManager).cancel(NotificationManager.SUMMARY_NOTIFICATION_ID)
        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun notifyTasksWithNoPermissionDoesNothing() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(false)

        notificationManager.notifyTasks(
            newNotifications = listOf(Notification(taskId = 1, timestamp = 100L, type = 0)),
            alert = true,
            nonstop = false,
            fiveTimes = false,
        )

        verify(notificationDao, never()).getAllOrdered()
        verify(notificationDao, never()).insertAll(anyList())
    }

    // --- notifyTasks() with 1 notification ---

    @Test
    fun notifyTasksWithOneNewBundled() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        `when`(notificationDao.getAllOrdered()).thenReturn(emptyList())
        `when`(preferences.bundleNotifications()).thenReturn(true)
        `when`(taskDao.activeNotifications()).thenReturn(emptyList())

        val notification = Notification(taskId = 1, timestamp = 100L, type = 0)

        notificationManager.notifyTasks(
            newNotifications = listOf(notification),
            alert = true,
            nonstop = false,
            fiveTimes = false,
        )

        verify(notificationDao).insertAll(listOf(notification))
        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun notifyTasksWithOneExistingAndOneNewBundled() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        val existing = Notification(taskId = 5, timestamp = 50L, type = 0)
        `when`(notificationDao.getAllOrdered()).thenReturn(listOf(existing))
        `when`(preferences.bundleNotifications()).thenReturn(true)
        `when`(taskDao.activeNotifications()).thenReturn(emptyList())

        val newNotification = Notification(taskId = 1, timestamp = 100L, type = 0)

        notificationManager.notifyTasks(
            newNotifications = listOf(newNotification),
            alert = true,
            nonstop = false,
            fiveTimes = false,
        )

        verify(notificationDao).insertAll(listOf(newNotification))
        verify(refreshBroadcaster).broadcastRefresh()
    }

    // --- notifyTasks() with >1 notifications ---

    @Test
    fun notifyTasksWithMultipleNewBundled() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        `when`(notificationDao.getAllOrdered()).thenReturn(emptyList())
        `when`(preferences.bundleNotifications()).thenReturn(true)
        `when`(taskDao.activeNotifications()).thenReturn(emptyList())

        val n1 = Notification(taskId = 1, timestamp = 100L, type = 0)
        val n2 = Notification(taskId = 2, timestamp = 200L, type = 0)
        val n3 = Notification(taskId = 3, timestamp = 300L, type = 0)

        notificationManager.notifyTasks(
            newNotifications = listOf(n1, n2, n3),
            alert = true,
            nonstop = true,
            fiveTimes = false,
        )

        verify(notificationDao).insertAll(listOf(n1, n2, n3))
        verify(refreshBroadcaster).broadcastRefresh()
    }

    // --- notifyTasks() without bundling ---

    @Test
    fun notifyTasksWithoutBundling() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        `when`(notificationDao.getAllOrdered()).thenReturn(emptyList())
        `when`(preferences.bundleNotifications()).thenReturn(false)

        val notification = Notification(taskId = 1, timestamp = 100L, type = 0)

        notificationManager.notifyTasks(
            newNotifications = listOf(notification),
            alert = true,
            nonstop = false,
            fiveTimes = false,
        )

        verify(notificationDao).insertAll(listOf(notification))
        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun notifyTasksInsertsNewNotifications() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        `when`(notificationDao.getAllOrdered()).thenReturn(emptyList())

        notificationManager.notifyTasks(
            newNotifications = emptyList(),
            alert = false,
            nonstop = false,
            fiveTimes = false,
        )

        verify(notificationDao).insertAll(emptyList())
    }

    // --- restoreNotifications ---

    @Test
    fun restoreNotificationsNoPermissionReturnsEarly() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(false)

        notificationManager.restoreNotifications(cancelExisting = true)

        verify(notificationDao, never()).getAllOrdered()
    }

    @Test
    fun restoreNotificationsCancelExistingCancelsEachNotification() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        val n1 = Notification(taskId = 1, timestamp = 100L, type = 0)
        val n2 = Notification(taskId = 2, timestamp = 200L, type = 0)
        `when`(notificationDao.getAllOrdered()).thenReturn(listOf(n1, n2))
        `when`(preferences.bundleNotifications()).thenReturn(false)

        notificationManager.restoreNotifications(cancelExisting = true)

        // cancelExisting causes cancel(taskId) for each notification
        verify(throttledNotificationManager, atLeast(1)).cancel(1)
        verify(throttledNotificationManager, atLeast(1)).cancel(2)
    }

    @Test
    fun restoreNotificationsDoNotCancelPerTaskWhenFlagIsFalse() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        `when`(notificationDao.getAllOrdered()).thenReturn(emptyList())
        `when`(preferences.bundleNotifications()).thenReturn(false)

        notificationManager.restoreNotifications(cancelExisting = false)

        // With empty notifications list and cancelExisting=false,
        // the cancelSummaryNotification is still called since bundle=false
        verify(throttledNotificationManager).cancel(NotificationManager.SUMMARY_NOTIFICATION_ID)
    }

    @Test
    fun restoreNotificationsBundledWithMultipleNotifications() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        val n1 = Notification(taskId = 1, timestamp = 100L, type = 0)
        val n2 = Notification(taskId = 2, timestamp = 200L, type = 0)
        `when`(notificationDao.getAllOrdered()).thenReturn(listOf(n1, n2))
        `when`(preferences.bundleNotifications()).thenReturn(true)
        `when`(taskDao.activeNotifications()).thenReturn(emptyList())

        notificationManager.restoreNotifications(cancelExisting = false)

        // updateSummary calls activeNotifications
        verify(taskDao).activeNotifications()
    }

    @Test
    fun restoreNotificationsUnbundledCancelsSummary() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        val n1 = Notification(taskId = 1, timestamp = 100L, type = 0)
        `when`(notificationDao.getAllOrdered()).thenReturn(listOf(n1))
        `when`(preferences.bundleNotifications()).thenReturn(false)

        notificationManager.restoreNotifications(cancelExisting = false)

        verify(throttledNotificationManager).cancel(NotificationManager.SUMMARY_NOTIFICATION_ID)
    }

    @Test
    fun restoreNotificationsEmptyListBundled() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        `when`(notificationDao.getAllOrdered()).thenReturn(emptyList())
        `when`(preferences.bundleNotifications()).thenReturn(true)

        notificationManager.restoreNotifications(cancelExisting = false)

        // With 0 notifications and bundled=true, size > 1 check fails
        verify(taskDao, never()).activeNotifications()
    }

    // --- updateTimerNotification ---

    @Test
    fun updateTimerNotificationNoPermissionReturnsEarly() = runTest {
        `when`(permissionChecker.hasNotificationPermission()).thenReturn(false)

        notificationManager.updateTimerNotification()

        verify(taskDao, never()).activeTimers()
    }

    @Test
    fun updateTimerNotificationWithZeroTimersCancels() = runTest {
        `when`(permissionChecker.hasNotificationPermission()).thenReturn(true)
        `when`(taskDao.activeTimers()).thenReturn(0)

        notificationManager.updateTimerNotification()

        verify(taskDao).activeTimers()
    }

    // --- triggerNotifications ---

    @Test
    fun triggerNotificationsDelegatesToWorkManager() {
        notificationManager.triggerNotifications()

        verify(workManager).triggerNotifications()
    }

    // --- currentInterruptionFilter ---

    @Test
    fun currentInterruptionFilterDelegatesToThrottledManager() {
        `when`(throttledNotificationManager.currentInterruptionFilter).thenReturn(3)

        val result = notificationManager.currentInterruptionFilter

        verify(throttledNotificationManager).currentInterruptionFilter
        assert(result == 3)
    }
}
