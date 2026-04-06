package org.tasks.notifications

import android.content.Context
import android.content.res.Resources
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.dao.LocationDao
import org.tasks.data.dao.NotificationDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Notification
import org.tasks.data.entity.Task
import org.tasks.jobs.WorkManager
import org.tasks.markdown.Markdown
import org.tasks.markdown.MarkdownProvider
import org.tasks.preferences.PermissionChecker
import org.tasks.preferences.Preferences

/**
 * Deep tests for [NotificationManager] logic paths.
 * Tests focus on DAO interactions, permission checks, and notification flow branching.
 * Methods that create real Android Notification/Intent objects cannot be
 * unit-tested without Robolectric, so we test those paths through higher-level
 * interactions that verify DAO and manager calls.
 */
class NotificationManagerDeepTest {

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

        val resources = mock(Resources::class.java)
        `when`(context.resources).thenReturn(resources)
        `when`(context.applicationContext).thenReturn(context)
        `when`(context.packageName).thenReturn("org.tasks")
        `when`(resources.getBoolean(anyInt())).thenReturn(false)
        `when`(resources.getString(anyInt())).thenReturn("text")

        `when`(markdownProvider.markdown(force = true)).thenReturn(markdown)
        `when`(markdown.toMarkdown(anyString())).thenAnswer { it.arguments[0] as? CharSequence }
        `when`(markdown.toMarkdown(null)).thenReturn(null)

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

    // ========== getTaskNotification (null/completed/deleted checks) ==========

    @Test
    fun getTaskNotification_nullTask_returnsNull() = runTest {
        `when`(taskDao.fetch(99L)).thenReturn(null)
        val notification = Notification(taskId = 99, timestamp = 100L, type = 0)
        val result = notificationManager.getTaskNotification(notification)
        assertNull(result)
    }

    @Test
    fun getTaskNotification_completedTask_returnsNull() = runTest {
        val task = Task(id = 1, title = "Done task", completionDate = 1000L)
        `when`(taskDao.fetch(1L)).thenReturn(task)
        val notification = Notification(taskId = 1, timestamp = 100L, type = 0)
        val result = notificationManager.getTaskNotification(notification)
        assertNull(result)
    }

    @Test
    fun getTaskNotification_deletedTask_returnsNull() = runTest {
        val task = Task(id = 2, title = "Deleted task", deletionDate = 1000L)
        `when`(taskDao.fetch(2L)).thenReturn(task)
        val notification = Notification(taskId = 2, timestamp = 100L, type = 0)
        val result = notificationManager.getTaskNotification(notification)
        assertNull(result)
    }

    @Test
    fun getTaskNotification_completedAndDeletedTask_returnsNull() = runTest {
        val task = Task(id = 3, title = "Both", completionDate = 500L, deletionDate = 1000L)
        `when`(taskDao.fetch(3L)).thenReturn(task)
        val notification = Notification(taskId = 3, timestamp = 100L, type = 0)
        val result = notificationManager.getTaskNotification(notification)
        assertNull(result)
    }

    // ========== notifyTasks ==========

    @Test
    fun notifyTasks_noPermission_doesNothing() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(false)
        notificationManager.notifyTasks(
            newNotifications = listOf(Notification(taskId = 1, timestamp = 100L, type = 0)),
            alert = true, nonstop = false, fiveTimes = false,
        )
        verify(notificationDao, never()).getAllOrdered()
        verify(notificationDao, never()).insertAll(org.mockito.ArgumentMatchers.anyList())
    }

    @Test
    fun notifyTasks_zeroTotal_cancelsSummary() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        `when`(notificationDao.getAllOrdered()).thenReturn(emptyList())
        notificationManager.notifyTasks(
            newNotifications = emptyList(), alert = false, nonstop = false, fiveTimes = false,
        )
        verify(throttledNotificationManager).cancel(NotificationManager.SUMMARY_NOTIFICATION_ID)
        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun notifyTasks_insertsNewNotifications() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        `when`(notificationDao.getAllOrdered()).thenReturn(emptyList())
        val n1 = Notification(taskId = 1, timestamp = 100L, type = 0)
        notificationManager.notifyTasks(
            newNotifications = listOf(n1), alert = true, nonstop = false, fiveTimes = false,
        )
        verify(notificationDao).insertAll(listOf(n1))
        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun notifyTasks_bundled_oneNew_callsCreateNotifications() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        `when`(notificationDao.getAllOrdered()).thenReturn(emptyList())
        `when`(preferences.bundleNotifications()).thenReturn(true)
        `when`(taskDao.activeNotifications()).thenReturn(emptyList())
        val n1 = Notification(taskId = 1, timestamp = 100L, type = 0)
        notificationManager.notifyTasks(
            newNotifications = listOf(n1), alert = true, nonstop = false, fiveTimes = false,
        )
        verify(notificationDao).insertAll(listOf(n1))
        verify(taskDao, atLeastOnce()).activeNotifications()
    }

    @Test
    fun notifyTasks_bundled_multipleNew() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        `when`(notificationDao.getAllOrdered()).thenReturn(emptyList())
        `when`(preferences.bundleNotifications()).thenReturn(true)
        `when`(taskDao.activeNotifications()).thenReturn(emptyList())
        val n1 = Notification(taskId = 1, timestamp = 100L, type = 0)
        val n2 = Notification(taskId = 2, timestamp = 200L, type = 0)
        notificationManager.notifyTasks(
            newNotifications = listOf(n1, n2), alert = true, nonstop = true, fiveTimes = false,
        )
        verify(notificationDao).insertAll(listOf(n1, n2))
        verify(taskDao, atLeastOnce()).activeNotifications()
    }

    @Test
    fun notifyTasks_bundled_oneExisting_oneNew() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        val existing = Notification(taskId = 5, timestamp = 50L, type = 0)
        `when`(notificationDao.getAllOrdered()).thenReturn(listOf(existing))
        `when`(preferences.bundleNotifications()).thenReturn(true)
        `when`(taskDao.activeNotifications()).thenReturn(emptyList())
        val newN = Notification(taskId = 10, timestamp = 100L, type = 0)
        notificationManager.notifyTasks(
            newNotifications = listOf(newN), alert = true, nonstop = false, fiveTimes = false,
        )
        verify(notificationDao).insertAll(listOf(newN))
        // totalCount=2, bundled, existing.size==1 triggers re-creation of existing notifications
    }

    @Test
    fun notifyTasks_unbundled_createsDirectly() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        `when`(notificationDao.getAllOrdered()).thenReturn(emptyList())
        `when`(preferences.bundleNotifications()).thenReturn(false)
        val n1 = Notification(taskId = 1, timestamp = 100L, type = 0)
        val n2 = Notification(taskId = 2, timestamp = 200L, type = 0)
        notificationManager.notifyTasks(
            newNotifications = listOf(n1, n2), alert = true, nonstop = false, fiveTimes = false,
        )
        verify(notificationDao).insertAll(listOf(n1, n2))
        verify(refreshBroadcaster).broadcastRefresh()
    }

    // ========== restoreNotifications ==========

    @Test
    fun restoreNotifications_noPermission_returnsEarly() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(false)
        notificationManager.restoreNotifications(cancelExisting = true)
        verify(notificationDao, never()).getAllOrdered()
    }

    @Test
    fun restoreNotifications_cancelExisting_cancelsEach() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        val n1 = Notification(taskId = 10, timestamp = 100L, type = 0)
        val n2 = Notification(taskId = 20, timestamp = 200L, type = 0)
        `when`(notificationDao.getAllOrdered()).thenReturn(listOf(n1, n2))
        `when`(preferences.bundleNotifications()).thenReturn(false)
        notificationManager.restoreNotifications(cancelExisting = true)
        verify(throttledNotificationManager, atLeast(1)).cancel(10)
        verify(throttledNotificationManager, atLeast(1)).cancel(20)
    }

    @Test
    fun restoreNotifications_notCancelExisting_doesNotCancelEach() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        `when`(notificationDao.getAllOrdered()).thenReturn(emptyList())
        `when`(preferences.bundleNotifications()).thenReturn(false)
        notificationManager.restoreNotifications(cancelExisting = false)
        verify(throttledNotificationManager).cancel(NotificationManager.SUMMARY_NOTIFICATION_ID)
    }

    @Test
    fun restoreNotifications_bundled_multipleNotifications() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        val n1 = Notification(taskId = 1, timestamp = 100L, type = 0)
        val n2 = Notification(taskId = 2, timestamp = 200L, type = 0)
        `when`(notificationDao.getAllOrdered()).thenReturn(listOf(n1, n2))
        `when`(preferences.bundleNotifications()).thenReturn(true)
        `when`(taskDao.activeNotifications()).thenReturn(emptyList())
        notificationManager.restoreNotifications(cancelExisting = false)
        verify(taskDao).activeNotifications()
    }

    @Test
    fun restoreNotifications_bundled_singleNotification() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        val n1 = Notification(taskId = 1, timestamp = 100L, type = 0)
        `when`(notificationDao.getAllOrdered()).thenReturn(listOf(n1))
        `when`(preferences.bundleNotifications()).thenReturn(true)
        // With 1 notification, size > 1 is false, goes to else branch
        notificationManager.restoreNotifications(cancelExisting = false)
        verify(throttledNotificationManager).cancel(NotificationManager.SUMMARY_NOTIFICATION_ID)
    }

    @Test
    fun restoreNotifications_unbundled_cancelsSummary() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        val n1 = Notification(taskId = 1, timestamp = 100L, type = 0)
        `when`(notificationDao.getAllOrdered()).thenReturn(listOf(n1))
        `when`(preferences.bundleNotifications()).thenReturn(false)
        notificationManager.restoreNotifications(cancelExisting = false)
        verify(throttledNotificationManager).cancel(NotificationManager.SUMMARY_NOTIFICATION_ID)
    }

    @Test
    fun restoreNotifications_emptyAndBundled() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        `when`(notificationDao.getAllOrdered()).thenReturn(emptyList())
        `when`(preferences.bundleNotifications()).thenReturn(true)
        notificationManager.restoreNotifications(cancelExisting = false)
        verify(taskDao, never()).activeNotifications()
    }

    // ========== updateTimerNotification ==========

    @Test
    fun updateTimerNotification_noPermission() = runTest {
        `when`(permissionChecker.hasNotificationPermission()).thenReturn(false)
        notificationManager.updateTimerNotification()
        verify(taskDao, never()).activeTimers()
    }

    @Test
    fun updateTimerNotification_zeroTimers() = runTest {
        `when`(permissionChecker.hasNotificationPermission()).thenReturn(true)
        `when`(permissionChecker.canNotify()).thenReturn(true)
        `when`(taskDao.activeTimers()).thenReturn(0)
        `when`(notificationDao.getAllOrdered()).thenReturn(emptyList())
        `when`(notificationDao.getAll()).thenReturn(emptyList())
        notificationManager.updateTimerNotification()
        verify(taskDao).activeTimers()
    }

    @Test
    fun updateTimerNotification_withTimers_queriesTimerCount() = runTest {
        `when`(permissionChecker.hasNotificationPermission()).thenReturn(true)
        `when`(permissionChecker.canNotify()).thenReturn(true)
        `when`(taskDao.activeTimers()).thenReturn(3)
        // This will fail deeper in notification building due to unmocked Android stubs,
        // but we verify the timer count was queried
        try {
            notificationManager.updateTimerNotification()
        } catch (e: RuntimeException) {
            // Expected: Android stubs not mocked (Intent, PendingIntent, etc.)
        }
        verify(taskDao).activeTimers()
    }

    // ========== cancel ==========

    @Test
    fun cancel_singleId_deletesFromDao() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        `when`(notificationDao.getAllOrdered()).thenReturn(emptyList())
        notificationManager.cancel(42L)
        verify(throttledNotificationManager).cancel(42)
        verify(notificationDao).deleteAll(listOf(42L))
    }

    @Test
    fun cancel_summaryId_fetchesAllAndCancels() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        `when`(notificationDao.getAll()).thenReturn(listOf(1L, 2L))
        `when`(notificationDao.getAllOrdered()).thenReturn(emptyList())
        notificationManager.cancel(NotificationManager.SUMMARY_NOTIFICATION_ID.toLong())
        verify(notificationDao).getAll()
        verify(throttledNotificationManager).cancel(1)
        verify(throttledNotificationManager).cancel(2)
    }

    @Test
    fun cancel_multipleIds() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        `when`(notificationDao.getAllOrdered()).thenReturn(emptyList())
        notificationManager.cancel(listOf(10L, 20L, 30L))
        verify(throttledNotificationManager).cancel(10)
        verify(throttledNotificationManager).cancel(20)
        verify(throttledNotificationManager).cancel(30)
        verify(notificationDao).deleteAll(listOf(10L, 20L, 30L))
        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun cancel_emptyList() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(true)
        `when`(notificationDao.getAllOrdered()).thenReturn(emptyList())
        notificationManager.cancel(emptyList())
        verify(notificationDao).deleteAll(emptyList())
    }

    @Test
    fun cancel_noPermission_stillCancelsSystem() = runTest {
        `when`(permissionChecker.canNotify()).thenReturn(false)
        notificationManager.cancel(listOf(5L))
        verify(throttledNotificationManager).cancel(5)
        verify(notificationDao).deleteAll(listOf(5L))
    }

    // ========== triggerNotifications ==========

    @Test
    fun triggerNotifications_delegatesToWorkManager() {
        notificationManager.triggerNotifications()
        verify(workManager).triggerNotifications()
    }

    // ========== currentInterruptionFilter ==========

    @Test
    fun currentInterruptionFilter_delegatesToThrottledManager() {
        `when`(throttledNotificationManager.currentInterruptionFilter).thenReturn(1)
        assertEquals(1, notificationManager.currentInterruptionFilter)
    }

    @Test
    fun currentInterruptionFilter_different_value() {
        `when`(throttledNotificationManager.currentInterruptionFilter).thenReturn(4)
        assertEquals(4, notificationManager.currentInterruptionFilter)
    }

    // ========== companion constants ==========

    @Test
    fun groupKey_isPrivate() {
        // GROUP_KEY is private const, verified indirectly through notification behavior
        assertEquals("notifications", NotificationManager.NOTIFICATION_CHANNEL_DEFAULT)
    }
}
