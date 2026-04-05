package org.tasks.service

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.caldav.VtodoCache
import org.tasks.data.dao.DeletionDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.Task
import org.tasks.preferences.TasksPreferences

class TaskDeleterTest {

    private lateinit var deletionDao: DeletionDao
    private lateinit var taskDao: TaskDao
    private lateinit var refreshBroadcaster: RefreshBroadcaster
    private lateinit var vtodoCache: VtodoCache
    private lateinit var tasksPreferences: TasksPreferences
    private lateinit var taskCleanup: TaskCleanup
    private lateinit var taskDeleter: TaskDeleter

    @Before
    fun setUp() {
        deletionDao = mock()
        taskDao = mock()
        refreshBroadcaster = mock()
        vtodoCache = mock()
        tasksPreferences = mock()
        taskCleanup = mock()
        taskDeleter = TaskDeleter(
            deletionDao = deletionDao,
            taskDao = taskDao,
            refreshBroadcaster = refreshBroadcaster,
            vtodoCache = vtodoCache,
            tasksPreferences = tasksPreferences,
            taskCleanup = taskCleanup,
        )
    }

    // --- markDeleted(Task) ---

    @Test
    fun markDeletedSingleTaskCallsOnMarkedDeleted() = runTest {
        val task = Task(id = 42, readOnly = false)
        `when`(taskDao.getChildren(listOf(42L))).thenReturn(emptyList())
        `when`(taskDao.fetch(listOf(42L))).thenReturn(listOf(task))

        taskDeleter.markDeleted(task)

        verify(taskCleanup).onMarkedDeleted()
    }

    @Test
    fun markDeletedSingleTaskBroadcastsRefresh() = runTest {
        val task = Task(id = 42, readOnly = false)
        `when`(taskDao.getChildren(listOf(42L))).thenReturn(emptyList())
        `when`(taskDao.fetch(listOf(42L))).thenReturn(listOf(task))

        taskDeleter.markDeleted(task)

        verify(refreshBroadcaster).broadcastRefresh()
    }

    // --- markDeleted(List<Long>) ---

    @Test
    fun markDeletedFiltersOutReadOnlyTasks() = runTest {
        val writable = Task(id = 1, readOnly = false)
        val readOnly = Task(id = 2, readOnly = true)
        `when`(taskDao.getChildren(Mockito.anyList())).thenReturn(emptyList())
        `when`(taskDao.fetch(Mockito.anyList())).thenReturn(listOf(writable, readOnly))
            .thenReturn(listOf(writable)) // second call from final fetch

        val result = taskDeleter.markDeleted(listOf(1L, 2L))

        assertEquals(listOf(writable), result)
    }

    @Test
    fun markDeletedIncludesChildren() = runTest {
        val parent = Task(id = 1, readOnly = false)
        val child = Task(id = 10, readOnly = false)
        `when`(taskDao.getChildren(Mockito.anyList())).thenReturn(listOf(10L))
        `when`(taskDao.fetch(Mockito.anyList())).thenReturn(listOf(parent, child))

        taskDeleter.markDeleted(listOf(1L))

        verify(taskCleanup).onMarkedDeleted()
    }

    @Test
    fun markDeletedBroadcastsRefresh() = runTest {
        `when`(taskDao.getChildren(Mockito.anyList())).thenReturn(emptyList())
        `when`(taskDao.fetch(Mockito.anyList())).thenReturn(emptyList())

        taskDeleter.markDeleted(listOf(1L))

        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun markDeletedCallsCleanupOnMarkedDeleted() = runTest {
        `when`(taskDao.getChildren(Mockito.anyList())).thenReturn(emptyList())
        `when`(taskDao.fetch(Mockito.anyList())).thenReturn(emptyList())

        taskDeleter.markDeleted(listOf(1L))

        verify(taskCleanup).onMarkedDeleted()
    }

    @Test
    fun markDeletedWithEmptyListStillBroadcasts() = runTest {
        `when`(taskDao.getChildren(Mockito.anyList())).thenReturn(emptyList())
        `when`(taskDao.fetch(Mockito.anyList())).thenReturn(emptyList())

        taskDeleter.markDeleted(emptyList())

        verify(refreshBroadcaster).broadcastRefresh()
    }

    // --- delete(Task) ---

    @Test
    fun deleteTaskBroadcastsRefresh() = runTest {
        val task = Task(id = 7)

        taskDeleter.delete(task)

        verify(refreshBroadcaster).broadcastRefresh()
    }

    // --- delete(Long) ---

    @Test
    fun deleteByIdBroadcastsRefresh() = runTest {
        taskDeleter.delete(99L)

        verify(refreshBroadcaster).broadcastRefresh()
    }

    // --- delete(List<Long>) ---

    @Test
    fun deleteListOfTasksBroadcastsRefresh() = runTest {
        taskDeleter.delete(listOf(1L, 2L, 3L))

        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun deleteEmptyListStillBroadcasts() = runTest {
        taskDeleter.delete(emptyList<Long>())

        verify(refreshBroadcaster).broadcastRefresh()
    }

    // --- delete(CaldavCalendar) ---

    @Test
    fun deleteCalendarDeletesVtodoCache() = runTest {
        val calendar = CaldavCalendar(account = "acc1", uuid = "cal1")

        taskDeleter.delete(calendar)

        verify(vtodoCache).delete(calendar)
    }

    @Test
    fun deleteCalendarDeletesFilterPreferences() = runTest {
        val calendar = CaldavCalendar(account = "acc1", uuid = "cal1")

        taskDeleter.delete(calendar)

        verify(tasksPreferences).removeByPrefix("filter_sort_list_acc1_cal1_")
    }

    @Test
    fun deleteCalendarBroadcastsRefresh() = runTest {
        val calendar = CaldavCalendar(account = "acc1", uuid = "cal1")

        taskDeleter.delete(calendar)

        verify(refreshBroadcaster).broadcastRefresh()
    }

    // --- delete(CaldavAccount) ---

    @Test
    fun deleteAccountDeletesVtodoCache() = runTest {
        val account = CaldavAccount(uuid = "acc1")
        `when`(deletionDao.delete(
            caldavAccount = Mockito.eq(account) ?: account,
            cleanup = Mockito.any() ?: {},
        )).thenReturn(emptyList())

        taskDeleter.delete(account)

        verify(vtodoCache).delete(account)
    }

    @Test
    fun deleteAccountDeletesFilterPreferencesForEachCalendar() = runTest {
        val account = CaldavAccount(uuid = "acc1")
        val cal1 = CaldavCalendar(account = "acc1", uuid = "cal1")
        val cal2 = CaldavCalendar(account = "acc1", uuid = "cal2")
        `when`(deletionDao.delete(
            caldavAccount = Mockito.eq(account) ?: account,
            cleanup = Mockito.any() ?: {},
        )).thenReturn(listOf(cal1, cal2))

        taskDeleter.delete(account)

        verify(tasksPreferences).removeByPrefix("filter_sort_list_acc1_cal1_")
        verify(tasksPreferences).removeByPrefix("filter_sort_list_acc1_cal2_")
    }

    @Test
    fun deleteAccountBroadcastsRefresh() = runTest {
        val account = CaldavAccount(uuid = "acc1")
        `when`(deletionDao.delete(
            caldavAccount = Mockito.eq(account) ?: account,
            cleanup = Mockito.any() ?: {},
        )).thenReturn(emptyList())

        taskDeleter.delete(account)

        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun deleteAccountWithNoCalendarsDoesNotDeletePreferences() = runTest {
        val account = CaldavAccount(uuid = "acc1")
        `when`(deletionDao.delete(
            caldavAccount = Mockito.eq(account) ?: account,
            cleanup = Mockito.any() ?: {},
        )).thenReturn(emptyList())

        taskDeleter.delete(account)

        verify(tasksPreferences, never()).removeByPrefix(Mockito.anyString())
    }

    // --- isDeleted ---

    @Test
    fun isDeletedReturnsTrueWhenDeleted() = runTest {
        `when`(deletionDao.isDeleted(42L)).thenReturn(true)

        assertTrue(taskDeleter.isDeleted(42L))
    }

    @Test
    fun isDeletedReturnsFalseWhenNotDeleted() = runTest {
        `when`(deletionDao.isDeleted(42L)).thenReturn(false)

        assertFalse(taskDeleter.isDeleted(42L))
    }
}
