package org.tasks.sync

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_ETEBASE
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.SUPPRESS_SYNC
import org.tasks.data.entity.Task
import org.tasks.jobs.BackgroundWork
import org.tasks.preferences.TasksPreferences

@OptIn(ExperimentalCoroutinesApi::class)
class SyncAdaptersTest {

    private val caldavDao = mock(CaldavDao::class.java)
    private val googleTaskDao = mock(GoogleTaskDao::class.java)
    private val backgroundWork = mock(BackgroundWork::class.java)
    private val tasksPreferences = mock(TasksPreferences::class.java)
    private val refreshBroadcaster = mock(RefreshBroadcaster::class.java)

    private fun createSyncAdapters(
        testDispatcher: kotlinx.coroutines.test.TestDispatcher,
        openTaskSyncCheck: suspend () -> Boolean = { false },
    ): SyncAdapters = SyncAdapters(
        backgroundWork = backgroundWork,
        caldavDao = caldavDao,
        googleTaskDao = googleTaskDao,
        openTaskSyncCheck = openTaskSyncCheck,
        tasksPreferences = tasksPreferences,
        refreshBroadcaster = refreshBroadcaster,
        coroutineContext = testDispatcher,
    )

    // --- sync(task, original): suppress sync ---

    @Test
    fun syncSuppressedWhenTransitorySet() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val adapters = createSyncAdapters(dispatcher)
        val task = Task(id = 1, title = "Changed")
        task.putTransitory(SUPPRESS_SYNC, true)

        adapters.sync(task, null)
        advanceUntilIdle()

        verify(backgroundWork, never()).sync(SyncSource.TASK_CHANGE)
    }

    @Test
    fun syncNotTriggeredWhenTaskUpToDate() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val adapters = createSyncAdapters(dispatcher)
        val task = Task(id = 1, title = "Same")
        val original = task.copy()
        `when`(googleTaskDao.getAllByTaskId(1)).thenReturn(emptyList())
        `when`(caldavDao.isAccountType(1, listOf(TYPE_MICROSOFT))).thenReturn(false)
        `when`(caldavDao.isAccountType(1, listOf(
            TYPE_CALDAV, TYPE_TASKS, TYPE_ETEBASE, CaldavAccount.TYPE_OPENTASKS
        ))).thenReturn(false)

        adapters.sync(task, original)
        advanceUntilIdle()

        verify(backgroundWork, never()).sync(SyncSource.TASK_CHANGE)
    }

    @Test
    fun syncTriggeredWhenGoogleTaskNeedsSync() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val adapters = createSyncAdapters(dispatcher)
        val task = Task(id = 1, title = "New Title")
        val original = Task(id = 1, title = "Old Title")
        `when`(googleTaskDao.getAllByTaskId(1))
            .thenReturn(listOf(CaldavTask(task = 1, calendar = "cal")))
        `when`(caldavDao.isAccountType(1, listOf(TYPE_MICROSOFT))).thenReturn(false)
        `when`(caldavDao.isAccountType(1, listOf(
            TYPE_CALDAV, TYPE_TASKS, TYPE_ETEBASE, CaldavAccount.TYPE_OPENTASKS
        ))).thenReturn(false)

        adapters.sync(task, original)
        advanceTimeBy(1100)

        verify(backgroundWork).sync(SyncSource.TASK_CHANGE)
    }

    // --- sync(source) ---

    @Test
    fun syncSourceTriggersWhenCaldavEnabled() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val adapters = createSyncAdapters(dispatcher)
        `when`(caldavDao.getAccounts(
            TYPE_GOOGLE_TASKS, TYPE_CALDAV, TYPE_TASKS, TYPE_ETEBASE, TYPE_MICROSOFT
        )).thenReturn(listOf(CaldavAccount()))

        adapters.sync(SyncSource.USER_INITIATED)
        advanceTimeBy(1100)

        verify(backgroundWork).sync(SyncSource.USER_INITIATED)
    }

    @Test
    fun syncSourceTriggersWhenOpenTasksEnabled() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val adapters = createSyncAdapters(dispatcher, openTaskSyncCheck = { true })
        `when`(caldavDao.getAccounts(
            TYPE_GOOGLE_TASKS, TYPE_CALDAV, TYPE_TASKS, TYPE_ETEBASE, TYPE_MICROSOFT
        )).thenReturn(emptyList())

        adapters.sync(SyncSource.USER_INITIATED)
        advanceTimeBy(1100)

        verify(backgroundWork).sync(SyncSource.USER_INITIATED)
    }

    @Test
    fun syncSourceDoesNotTriggerWhenNothingEnabled() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val adapters = createSyncAdapters(dispatcher, openTaskSyncCheck = { false })
        `when`(caldavDao.getAccounts(
            TYPE_GOOGLE_TASKS, TYPE_CALDAV, TYPE_TASKS, TYPE_ETEBASE, TYPE_MICROSOFT
        )).thenReturn(emptyList())

        adapters.sync(SyncSource.USER_INITIATED)
        advanceUntilIdle()

        verify(backgroundWork, never()).sync(SyncSource.USER_INITIATED)
    }

    // --- SyncSource.upgrade ---

    @Test
    fun upgradeReplacesNonIndicatorWithIndicator() {
        val result = SyncSource.NONE.upgrade(SyncSource.USER_INITIATED)
        assertEquals(SyncSource.USER_INITIATED, result)
    }

    @Test
    fun upgradeKeepsExistingIndicatorSource() {
        val result = SyncSource.USER_INITIATED.upgrade(SyncSource.TASK_CHANGE)
        assertEquals(SyncSource.USER_INITIATED, result)
    }

    @Test
    fun upgradeKeepsCurrentWhenNewDoesNotShowIndicator() {
        val result = SyncSource.NONE.upgrade(SyncSource.BACKGROUND)
        assertEquals(SyncSource.NONE, result)
    }

    @Test
    fun upgradeKeepsIndicatorWhenNewIsNonIndicator() {
        val result = SyncSource.USER_INITIATED.upgrade(SyncSource.NONE)
        assertEquals(SyncSource.USER_INITIATED, result)
    }

    // --- SyncSource.fromString ---

    @Test
    fun fromStringParsesValidName() {
        assertEquals(SyncSource.USER_INITIATED, SyncSource.fromString("USER_INITIATED"))
    }

    @Test
    fun fromStringReturnsNoneForNull() {
        assertEquals(SyncSource.NONE, SyncSource.fromString(null))
    }

    @Test
    fun fromStringReturnsNoneForInvalidValue() {
        assertEquals(SyncSource.NONE, SyncSource.fromString("INVALID_VALUE"))
    }

    @Test
    fun fromStringReturnsNoneForEmpty() {
        assertEquals(SyncSource.NONE, SyncSource.fromString(""))
    }

    // --- SyncSource.showIndicator ---

    @Test
    fun userInitiatedShowsIndicator() {
        assertEquals(true, SyncSource.USER_INITIATED.showIndicator)
    }

    @Test
    fun noneDoesNotShowIndicator() {
        assertEquals(false, SyncSource.NONE.showIndicator)
    }

    @Test
    fun backgroundDoesNotShowIndicator() {
        assertEquals(false, SyncSource.BACKGROUND.showIndicator)
    }

    @Test
    fun taskChangeShowsIndicator() {
        assertEquals(true, SyncSource.TASK_CHANGE.showIndicator)
    }

    @Test
    fun accountAddedShowsIndicator() {
        assertEquals(true, SyncSource.ACCOUNT_ADDED.showIndicator)
    }

    @Test
    fun appBackgroundDoesNotShowIndicator() {
        assertEquals(false, SyncSource.APP_BACKGROUND.showIndicator)
    }
}
