package org.tasks.data

import com.todoroo.astrid.timers.TimerPlugin
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Task
import org.tasks.jobs.BackgroundWork
import org.tasks.location.LocationService
import org.tasks.notifications.Notifier
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource

class TaskSaverTest {

    private lateinit var taskDao: TaskDao
    private lateinit var refreshBroadcaster: RefreshBroadcaster
    private lateinit var notifier: Notifier
    private lateinit var locationService: LocationService
    private lateinit var timerPlugin: TimerPlugin
    private lateinit var syncAdapters: SyncAdapters
    private lateinit var backgroundWork: BackgroundWork
    private lateinit var taskSaver: TaskSaver

    @Suppress("UNCHECKED_CAST")
    private fun <T> any(): T = Mockito.any<T>() as T

    @Before
    fun setUp() {
        taskDao = mock(TaskDao::class.java)
        refreshBroadcaster = mock(RefreshBroadcaster::class.java)
        notifier = mock(Notifier::class.java)
        locationService = mock(LocationService::class.java)
        timerPlugin = mock(TimerPlugin::class.java)
        syncAdapters = mock(SyncAdapters::class.java)
        backgroundWork = mock(BackgroundWork::class.java)
        taskSaver = TaskSaver(
            taskDao = taskDao,
            refreshBroadcaster = refreshBroadcaster,
            notifier = notifier,
            locationService = locationService,
            timerPlugin = timerPlugin,
            syncAdapters = syncAdapters,
            backgroundWork = backgroundWork,
        )
    }

    // --- save ---

    @Test
    fun saveCallsAfterSaveWhenUpdateReturnsTrue() = runTest {
        val task = Task(id = 1, title = "Test")
        `when`(taskDao.update(any<Task>(), Mockito.any())).thenReturn(true)

        taskSaver.save(task)

        verify(notifier).triggerNotifications()
        verify(backgroundWork).scheduleRefresh(anyLong())
    }

    @Test
    fun saveDoesNotCallAfterSaveWhenUpdateReturnsFalse() = runTest {
        val task = Task(id = 1, title = "Test")
        `when`(taskDao.update(any<Task>(), Mockito.any())).thenReturn(false)

        taskSaver.save(task)

        verify(notifier, never()).triggerNotifications()
    }

    // --- afterSave: calendar ---

    @Test
    fun afterSaveUpdatesCalendarWhenUriPresent() = runTest {
        val task = Task(id = 1, calendarURI = "content://calendar/events/123")
        val original = task.copy()

        taskSaver.afterSave(task, original)

        verify(backgroundWork).updateCalendar(task)
    }

    @Test
    fun afterSaveDoesNotUpdateCalendarWhenUriNull() = runTest {
        val task = Task(id = 1, calendarURI = null)
        val original = task.copy()

        taskSaver.afterSave(task, original)

        verify(backgroundWork, never()).updateCalendar(any<Task>())
    }

    @Test
    fun afterSaveDoesNotUpdateCalendarWhenUriBlank() = runTest {
        val task = Task(id = 1, calendarURI = "  ")
        val original = task.copy()

        taskSaver.afterSave(task, original)

        verify(backgroundWork, never()).updateCalendar(any<Task>())
    }

    @Test
    fun afterSaveDoesNotUpdateCalendarWhenUriEmpty() = runTest {
        val task = Task(id = 1, calendarURI = "")
        val original = task.copy()

        taskSaver.afterSave(task, original)

        verify(backgroundWork, never()).updateCalendar(any<Task>())
    }

    // --- afterSave: timer ---

    @Test
    fun afterSaveStopsTimerWhenJustCompleted() = runTest {
        val task = Task(id = 1, completionDate = 1000L, timerStart = 500L)
        val original = Task(id = 1, completionDate = 0L, timerStart = 500L)

        taskSaver.afterSave(task, original)

        verify(timerPlugin).stopTimer(task)
    }

    @Test
    fun afterSaveDoesNotStopTimerWhenNotJustCompleted() = runTest {
        val task = Task(id = 1, completionDate = 1000L, timerStart = 500L)
        val original = Task(id = 1, completionDate = 1000L, timerStart = 500L)

        taskSaver.afterSave(task, original)

        verify(timerPlugin, never()).stopTimer(any<Task>())
    }

    @Test
    fun afterSaveDoesNotStopTimerWhenCompletedButTimerNotStarted() = runTest {
        val task = Task(id = 1, completionDate = 1000L, timerStart = 0L)
        val original = Task(id = 1, completionDate = 0L)

        taskSaver.afterSave(task, original)

        verify(timerPlugin, never()).stopTimer(any<Task>())
    }

    @Test
    fun afterSaveDoesNotStopTimerWhenUncompletedAndTimerRunning() = runTest {
        val task = Task(id = 1, completionDate = 0L, timerStart = 500L)
        val original = Task(id = 1, completionDate = 1000L, timerStart = 500L)

        taskSaver.afterSave(task, original)

        verify(timerPlugin, never()).stopTimer(any<Task>())
    }

    // --- afterSave: notifier cancel ---

    @Test
    fun afterSaveCancelsNotificationWhenDueDateMovedToFuture() = runTest {
        val farFuture = System.currentTimeMillis() + 86_400_000L
        val task = Task(id = 1, dueDate = farFuture)
        val original = Task(id = 1, dueDate = 1000L)

        taskSaver.afterSave(task, original)

        verify(notifier).cancel(1L)
    }

    @Test
    fun afterSaveDoesNotCancelNotificationWhenDueDateUnchanged() = runTest {
        val task = Task(id = 1, dueDate = 1000L)
        val original = Task(id = 1, dueDate = 1000L)

        taskSaver.afterSave(task, original)

        verify(notifier, never()).cancel(anyLong())
    }

    // --- afterSave: geofences ---

    @Test
    fun afterSaveUpdatesGeofencesWhenCompletionDateChanged() = runTest {
        val task = Task(id = 42, completionDate = 1000L)
        val original = Task(id = 42, completionDate = 0L)

        taskSaver.afterSave(task, original)

        verify(locationService).updateGeofences(42L)
    }

    @Test
    fun afterSaveUpdatesGeofencesWhenDeletionDateChanged() = runTest {
        val task = Task(id = 42, deletionDate = 1000L)
        val original = Task(id = 42, deletionDate = 0L)

        taskSaver.afterSave(task, original)

        verify(locationService).updateGeofences(42L)
    }

    @Test
    fun afterSaveDoesNotUpdateGeofencesWhenNeitherChanged() = runTest {
        val task = Task(id = 42, completionDate = 1000L, deletionDate = 500L)
        val original = Task(id = 42, completionDate = 1000L, deletionDate = 500L)

        taskSaver.afterSave(task, original)

        verify(locationService, never()).updateGeofences(anyLong())
    }

    // --- afterSave: refresh ---

    @Test
    fun afterSaveBroadcastsRefreshByDefault() = runTest {
        val task = Task(id = 1)
        val original = task.copy()

        taskSaver.afterSave(task, original)

        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun afterSaveDoesNotBroadcastRefreshWhenSuppressed() = runTest {
        val task = Task(id = 1)
        task.suppressRefresh()
        val original = task.copy()

        taskSaver.afterSave(task, original)

        verify(refreshBroadcaster, never()).broadcastRefresh()
    }

    // --- afterSave: always triggers ---

    @Test
    fun afterSaveAlwaysTriggersNotifications() = runTest {
        val task = Task(id = 1)
        val original = task.copy()

        taskSaver.afterSave(task, original)

        verify(notifier).triggerNotifications()
    }

    @Test
    fun afterSaveAlwaysSchedulesRefresh() = runTest {
        val task = Task(id = 1)
        val original = task.copy()

        taskSaver.afterSave(task, original)

        verify(backgroundWork).scheduleRefresh(anyLong())
    }

    // --- touch ---

    @Test
    fun touchCallsTouchOnDaoAndSyncs() = runTest {
        val ids = listOf(1L, 2L, 3L)

        taskSaver.touch(ids)

        verify(taskDao).touch(anyList(), anyLong())
        verify(syncAdapters).sync(SyncSource.TASK_CHANGE)
    }

    @Test
    fun touchWithEmptyListStillSyncs() = runTest {
        taskSaver.touch(emptyList())

        verify(syncAdapters).sync(SyncSource.TASK_CHANGE)
    }

    // --- setCollapsed(id) ---

    @Test
    fun setCollapsedByIdSyncsAndRefreshes() = runTest {
        taskSaver.setCollapsed(42L, true)

        verify(syncAdapters).sync(SyncSource.TASK_CHANGE)
        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun setCollapsedByIdExpandSyncsAndRefreshes() = runTest {
        taskSaver.setCollapsed(42L, false)

        verify(syncAdapters).sync(SyncSource.TASK_CHANGE)
        verify(refreshBroadcaster).broadcastRefresh()
    }

    // --- afterSave: with null original ---

    @Test
    fun afterSaveWithNullOriginalCompletionDateModified() = runTest {
        // When original is null, completionDate defaults to 0, so completionDate=1000 is "modified"
        val task = Task(id = 1, completionDate = 1000L, timerStart = 500L)

        taskSaver.afterSave(task, null)

        verify(timerPlugin).stopTimer(task)
    }

    @Test
    fun afterSaveWithNullOriginalDeletionDateModified() = runTest {
        val task = Task(id = 1, deletionDate = 1000L)

        taskSaver.afterSave(task, null)

        verify(locationService).updateGeofences(1L)
    }

    @Test
    fun afterSaveWithNullOriginalDueDateChangedToFuture() = runTest {
        val farFuture = System.currentTimeMillis() + 86_400_000L
        val task = Task(id = 1, dueDate = farFuture)

        taskSaver.afterSave(task, null)

        verify(notifier).cancel(1L)
    }

    @Test
    fun afterSaveDoesNotCancelNotificationWhenDueDateInPast() = runTest {
        val task = Task(id = 1, dueDate = 1000L)
        val original = Task(id = 1, dueDate = 2000L)

        taskSaver.afterSave(task, original)

        verify(notifier, never()).cancel(anyLong())
    }

    @Test
    fun afterSaveSyncsAlways() = runTest {
        val task = Task(id = 1)

        taskSaver.afterSave(task, task.copy())

        verify(syncAdapters).sync(any<Task>(), any())
    }

    @Test
    fun savePassesOriginalToUpdate() = runTest {
        val task = Task(id = 1, title = "Updated")
        val original = Task(id = 1, title = "Original")
        `when`(taskDao.update(any<Task>(), any())).thenReturn(true)

        taskSaver.save(task, original)

        verify(taskDao).update(task, original)
    }

    @Test
    fun setCollapsedCallsTaskDaoSetCollapsed() = runTest {
        taskSaver.setCollapsed(99L, true)

        verify(taskDao).setCollapsed(org.mockito.kotlin.eq(listOf(99L)), org.mockito.kotlin.eq(true), org.mockito.kotlin.any())
    }
}
