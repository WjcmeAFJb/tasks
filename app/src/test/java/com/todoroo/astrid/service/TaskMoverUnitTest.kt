package com.todoroo.astrid.service

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.caldav.VtodoCache
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.preferences.Preferences
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource

class TaskMoverUnitTest {
    private lateinit var taskDao: TaskDao
    private lateinit var caldavDao: CaldavDao
    private lateinit var googleTaskDao: GoogleTaskDao
    private lateinit var preferences: Preferences
    private lateinit var refreshBroadcaster: RefreshBroadcaster
    private lateinit var syncAdapters: SyncAdapters
    private lateinit var vtodoCache: VtodoCache
    private lateinit var taskMover: TaskMover

    @Suppress("UNCHECKED_CAST")
    private fun <T> any(): T = Mockito.any<T>() as T

    @Before
    fun setUp() = kotlinx.coroutines.test.runTest {
        taskDao = mock(TaskDao::class.java)
        caldavDao = mock(CaldavDao::class.java)
        googleTaskDao = mock(GoogleTaskDao::class.java)
        preferences = mock(Preferences::class.java)
        refreshBroadcaster = mock(RefreshBroadcaster::class.java)
        syncAdapters = mock(SyncAdapters::class.java)
        vtodoCache = mock(VtodoCache::class.java)

        // Default stubs for suspend functions returning primitives/collections
        // to prevent NPE on unboxing or null collection iteration
        `when`(preferences.addTasksToTop()).thenReturn(false)
        `when`(caldavDao.insert(any<CaldavTask>())).thenReturn(1L)
        `when`(caldavDao.insert(any<Task>(), any(), Mockito.anyBoolean())).thenReturn(1L)
        `when`(googleTaskDao.insert(any<CaldavTask>())).thenReturn(1L)
        // Concrete suspend methods on mocked abstract classes return null by default
        `when`(taskDao.getChildren(Mockito.anyLong())).thenReturn(emptyList())
        `when`(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        `when`(taskDao.fetch(any<List<Long>>())).thenReturn(emptyList())
        `when`(caldavDao.getTasks(any<List<Long>>())).thenReturn(emptyList())
        `when`(caldavDao.getCalendars(any<List<Long>>())).thenReturn(emptyList())

        taskMover = TaskMover(
            taskDao = taskDao,
            caldavDao = caldavDao,
            googleTaskDao = googleTaskDao,
            preferences = preferences,
            refreshBroadcaster = refreshBroadcaster,
            syncAdapters = syncAdapters,
            vtodoCache = vtodoCache,
        )
    }

    @Test
    fun getSingleFilterReturnsNullWhenNoCalendars() = runTest {
        `when`(caldavDao.getCalendars(any<List<Long>>())).thenReturn(emptyList())
        assertNull(taskMover.getSingleFilter(listOf(1L, 2L)))
    }

    @Test
    fun getSingleFilterReturnsNullWhenMultipleCalendars() = runTest {
        val cal1 = CaldavCalendar(uuid = "uuid1", account = "acc1")
        val cal2 = CaldavCalendar(uuid = "uuid2", account = "acc2")
        `when`(caldavDao.getCalendars(any<List<Long>>())).thenReturn(listOf(cal1, cal2))
        assertNull(taskMover.getSingleFilter(listOf(1L, 2L)))
    }

    @Test
    fun getSingleFilterReturnsFilterForSingleCalendar() = runTest {
        val cal = CaldavCalendar(uuid = "uuid1", account = "acc-uuid")
        val account = CaldavAccount(uuid = "acc-uuid")
        `when`(caldavDao.getCalendars(any<List<Long>>())).thenReturn(listOf(cal))
        `when`(caldavDao.getAccountByUuid("acc-uuid")).thenReturn(account)
        val result = taskMover.getSingleFilter(listOf(1L))
        assertNotNull(result)
        assertTrue(result is CaldavFilter)
        assertEquals("uuid1", (result as CaldavFilter).uuid)
    }

    @Test
    fun getSingleFilterReturnsNullWhenAccountNotFound() = runTest {
        val cal = CaldavCalendar(uuid = "uuid1", account = "acc-uuid")
        `when`(caldavDao.getCalendars(any<List<Long>>())).thenReturn(listOf(cal))
        `when`(caldavDao.getAccountByUuid("acc-uuid")).thenReturn(null)
        assertNull(taskMover.getSingleFilter(listOf(1L)))
    }

    @Test
    fun getSingleFilterReturnsNullWhenCalendarHasNullAccount() = runTest {
        val cal = CaldavCalendar(uuid = "uuid1", account = null)
        `when`(caldavDao.getCalendars(any<List<Long>>())).thenReturn(listOf(cal))
        assertNull(taskMover.getSingleFilter(listOf(1L)))
    }

    @Test
    fun getSingleFilterWithEmptyTaskList() = runTest {
        `when`(caldavDao.getCalendars(any<List<Long>>())).thenReturn(emptyList())
        assertNull(taskMover.getSingleFilter(emptyList()))
    }

    @Test
    fun moveByIdReturnsWhenCalendarNotFound() = runTest {
        `when`(caldavDao.getCalendarById(99L)).thenReturn(null)
        taskMover.move(1L, 99L)
        verify(taskDao, never()).getChildren(any<List<Long>>())
        verifyNoInteractions(refreshBroadcaster)
    }

    @Test
    fun moveByIdReturnsWhenAccountNotFound() = runTest {
        val cal = CaldavCalendar(id = 99L, uuid = "cal-uuid", account = "acc-uuid")
        `when`(caldavDao.getCalendarById(99L)).thenReturn(cal)
        `when`(caldavDao.getAccountByUuid("acc-uuid")).thenReturn(null)
        taskMover.move(1L, 99L)
        verify(taskDao, never()).getChildren(any<List<Long>>())
        verifyNoInteractions(refreshBroadcaster)
    }

    @Test
    fun moveByIdReturnsWhenCalendarAccountNull() = runTest {
        val cal = CaldavCalendar(id = 99L, uuid = "cal-uuid", account = null)
        `when`(caldavDao.getCalendarById(99L)).thenReturn(cal)
        taskMover.move(1L, 99L)
        verify(taskDao, never()).getChildren(any<List<Long>>())
        verifyNoInteractions(refreshBroadcaster)
    }

    @Test
    fun moveByIdDelegatesWhenCalendarAndAccountExist() = runTest {
        val cal = CaldavCalendar(id = 99L, uuid = "cal-uuid", account = "acc-uuid")
        val account = CaldavAccount(uuid = "acc-uuid")
        val task = Task(id = 1L)
        `when`(caldavDao.getCalendarById(99L)).thenReturn(cal)
        `when`(caldavDao.getAccountByUuid("acc-uuid")).thenReturn(account)
        `when`(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        `when`(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        taskMover.move(1L, 99L)
        verify(refreshBroadcaster).broadcastRefresh()
        verify(syncAdapters).sync(SyncSource.TASK_CHANGE)
    }

    @Test
    fun moveFilterOutsReadOnlyTasks() = runTest {
        val readOnlyTask = Task(id = 1L, readOnly = true)
        val writableTask = Task(id = 2L, readOnly = false)
        val filter = makeCaldavFilter("cal-uuid")
        `when`(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        `when`(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(readOnlyTask, writableTask))
        `when`(caldavDao.getTask(2L)).thenReturn(null)
        `when`(googleTaskDao.getByTaskId(2L)).thenReturn(null)
        taskMover.move(listOf(1L, 2L), filter)
        verify(googleTaskDao, never()).getByTaskId(1L)
        verify(googleTaskDao).getByTaskId(2L)
        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun moveExcludesChildrenFromTopLevelMove() = runTest {
        val task1 = Task(id = 1L)
        val filter = makeCaldavFilter("cal-uuid")
        `when`(taskDao.getChildren(any<List<Long>>())).thenReturn(listOf(2L))
        `when`(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task1))
        `when`(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        `when`(caldavDao.getTask(1L)).thenReturn(null)
        taskMover.move(listOf(1L, 2L), filter)
        verify(googleTaskDao, never()).getByTaskId(2L)
    }

    @Test
    fun moveSetsParentToZeroForSelectedTasks() = runTest {
        val task1 = Task(id = 1L)
        val filter = makeCaldavFilter("cal-uuid")
        `when`(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        `when`(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task1))
        `when`(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        `when`(caldavDao.getTask(1L)).thenReturn(null)
        taskMover.move(listOf(1L), filter)
        verify(taskDao).setParent(Mockito.eq(0L), any())
    }

    @Test
    fun moveCallsUpdateParentsForNonGoogleTasksList() = runTest {
        val task1 = Task(id = 1L)
        val filter = makeCaldavFilter("cal-uuid", accountType = TYPE_CALDAV)
        `when`(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        `when`(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task1))
        `when`(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        `when`(caldavDao.getTask(1L)).thenReturn(null)
        taskMover.move(listOf(1L), filter)
        verify(caldavDao).updateParents("cal-uuid")
    }

    @Test
    fun moveDoesNotCallUpdateParentsForGoogleTasksList() = runTest {
        val task1 = Task(id = 1L)
        val filter = makeCaldavFilter("cal-uuid", accountType = TYPE_GOOGLE_TASKS)
        `when`(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        `when`(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task1))
        `when`(taskDao.fetch(1L)).thenReturn(task1)
        `when`(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        `when`(caldavDao.getTask(1L)).thenReturn(null)
        taskMover.move(listOf(1L), filter)
        verify(caldavDao, never()).updateParents(any())
    }

    @Test
    fun moveTouchesTasksAfterMove() = runTest {
        val task1 = Task(id = 1L)
        val filter = makeCaldavFilter("cal-uuid")
        `when`(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        `when`(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task1))
        `when`(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        `when`(caldavDao.getTask(1L)).thenReturn(null)
        taskMover.move(listOf(1L), filter)
        verify(taskDao).touch(any<List<Long>>(), Mockito.anyLong())
    }

    @Test
    fun moveBroadcastsRefreshAfterMove() = runTest {
        val task = Task(id = 1L)
        val filter = makeCaldavFilter("cal-uuid")
        `when`(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        `when`(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        `when`(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        `when`(caldavDao.getTask(1L)).thenReturn(null)
        taskMover.move(listOf(1L), filter)
        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun moveSyncsAfterMove() = runTest {
        val task = Task(id = 1L)
        val filter = makeCaldavFilter("cal-uuid")
        `when`(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        `when`(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        `when`(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        `when`(caldavDao.getTask(1L)).thenReturn(null)
        taskMover.move(listOf(1L), filter)
        verify(syncAdapters).sync(SyncSource.TASK_CHANGE)
    }

    @Test
    fun moveEmptyListDoesNotCrash() = runTest {
        val filter = makeCaldavFilter("cal-uuid")
        `when`(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        `when`(taskDao.fetch(any<List<Long>>())).thenReturn(emptyList())
        taskMover.move(emptyList(), filter)
        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun moveGoogleTaskSkipsWhenSameCalendar() = runTest {
        val task = Task(id = 1L)
        val googleTask = CaldavTask(task = 1L, calendar = "cal-uuid")
        val filter = makeCaldavFilter("cal-uuid", accountType = TYPE_GOOGLE_TASKS)
        `when`(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        `when`(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        `when`(googleTaskDao.getByTaskId(1L)).thenReturn(googleTask)
        taskMover.move(listOf(1L), filter)
        verify(caldavDao, never()).markDeleted(any(), Mockito.anyLong())
    }

    @Test
    fun moveGoogleTaskToNewGoogleList() = runTest {
        val task = Task(id = 1L)
        val googleTask = CaldavTask(task = 1L, calendar = "old-cal")
        val filter = makeCaldavFilter("new-cal", accountType = TYPE_GOOGLE_TASKS)
        `when`(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        `when`(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        `when`(googleTaskDao.getByTaskId(1L)).thenReturn(googleTask)
        `when`(taskDao.getChildren(1L)).thenReturn(emptyList())
        taskMover.move(listOf(1L), filter)
        verify(caldavDao).markDeleted(any(), Mockito.anyLong())
        verify(googleTaskDao).insertAndShift(any(), any(), Mockito.anyBoolean())
    }

    @Test
    fun moveGoogleTaskToCaldavList() = runTest {
        val task = Task(id = 1L)
        val googleTask = CaldavTask(task = 1L, calendar = "old-cal")
        val filter = makeCaldavFilter("caldav-cal", accountType = TYPE_CALDAV)
        `when`(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        `when`(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        `when`(googleTaskDao.getByTaskId(1L)).thenReturn(googleTask)
        `when`(taskDao.getChildren(1L)).thenReturn(emptyList())
        taskMover.move(listOf(1L), filter)
        verify(caldavDao).markDeleted(any(), Mockito.anyLong())
        verify(caldavDao).insert(any<Task>(), any(), Mockito.anyBoolean())
    }

    @Test
    fun moveCaldavTaskSkipsWhenSameCalendar() = runTest {
        val task = Task(id = 1L)
        val caldavTask = CaldavTask(task = 1L, calendar = "cal-uuid")
        val filter = makeCaldavFilter("cal-uuid")
        `when`(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        `when`(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        `when`(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        `when`(caldavDao.getTask(1L)).thenReturn(caldavTask)
        taskMover.move(listOf(1L), filter)
        verify(caldavDao, never()).markDeleted(any(), Mockito.anyLong())
    }

    @Test
    fun moveCaldavTaskToNewCaldavList() = runTest {
        val task = Task(id = 1L)
        val caldavTask = CaldavTask(task = 1L, calendar = "old-cal", remoteId = "rid", obj = "obj.ics")
        val fromCalendar = CaldavCalendar(uuid = "old-cal", account = "acc")
        val filter = makeCaldavFilter("new-cal")
        `when`(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        `when`(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        `when`(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        `when`(caldavDao.getTask(1L)).thenReturn(caldavTask)
        `when`(taskDao.getChildren(1L)).thenReturn(emptyList())
        `when`(caldavDao.getCalendar("old-cal")).thenReturn(fromCalendar)
        taskMover.move(listOf(1L), filter)
        verify(caldavDao).markDeleted(any(), Mockito.anyLong())
        verify(vtodoCache).move(any(), any(), any())
        verify(caldavDao).insert(any<Task>(), any(), Mockito.anyBoolean())
    }

    @Test
    fun moveCaldavTaskToGoogleList() = runTest {
        val task = Task(id = 1L)
        val caldavTask = CaldavTask(task = 1L, calendar = "old-cal")
        val filter = makeCaldavFilter("google-cal", accountType = TYPE_GOOGLE_TASKS)
        `when`(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        `when`(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        `when`(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        `when`(caldavDao.getTask(1L)).thenReturn(caldavTask)
        `when`(taskDao.getChildren(1L)).thenReturn(emptyList())
        `when`(taskDao.fetch(1L)).thenReturn(task)
        taskMover.move(listOf(1L), filter)
        verify(caldavDao).markDeleted(any(), Mockito.anyLong())
        verify(googleTaskDao).insertAndShift(any(), any(), Mockito.anyBoolean())
    }

    @Test
    fun moveLocalTaskToCaldavList() = runTest {
        val task = Task(id = 1L)
        val filter = makeCaldavFilter("cal-uuid")
        `when`(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        `when`(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        `when`(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        `when`(caldavDao.getTask(1L)).thenReturn(null)
        `when`(taskDao.getChildren(1L)).thenReturn(emptyList())
        taskMover.move(listOf(1L), filter)
        verify(caldavDao).insert(any<Task>(), any(), Mockito.anyBoolean())
    }

    @Test
    fun moveLocalTaskToGoogleList() = runTest {
        val task = Task(id = 1L)
        val filter = makeCaldavFilter("google-cal", accountType = TYPE_GOOGLE_TASKS)
        `when`(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        `when`(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        `when`(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        `when`(caldavDao.getTask(1L)).thenReturn(null)
        `when`(taskDao.getChildren(1L)).thenReturn(emptyList())
        `when`(taskDao.fetch(1L)).thenReturn(task)
        taskMover.move(listOf(1L), filter)
        verify(googleTaskDao).insertAndShift(any(), any(), Mockito.anyBoolean())
    }

    @Test
    fun moveGoogleTaskWithChildrenToNewGoogleList() = runTest {
        val task = Task(id = 1L)
        val googleTask = CaldavTask(task = 1L, calendar = "old-cal")
        val filter = makeCaldavFilter("new-cal", accountType = TYPE_GOOGLE_TASKS)
        `when`(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        `when`(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        `when`(googleTaskDao.getByTaskId(1L)).thenReturn(googleTask)
        `when`(taskDao.getChildren(1L)).thenReturn(listOf(10L, 11L))
        taskMover.move(listOf(1L), filter)
        verify(googleTaskDao).insertAndShift(any(), any(), Mockito.anyBoolean())
        verify(googleTaskDao).insert(any<Iterable<CaldavTask>>())
    }

    @Test
    fun moveGoogleTaskWithChildrenToCaldavList() = runTest {
        val task = Task(id = 1L)
        val googleTask = CaldavTask(task = 1L, calendar = "old-cal")
        val filter = makeCaldavFilter("caldav-cal", accountType = TYPE_CALDAV)
        `when`(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        `when`(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        `when`(googleTaskDao.getByTaskId(1L)).thenReturn(googleTask)
        `when`(taskDao.getChildren(1L)).thenReturn(listOf(10L, 11L))
        taskMover.move(listOf(1L), filter)
        verify(caldavDao).insert(any<Task>(), any(), Mockito.anyBoolean())
        verify(caldavDao).insert(any<Iterable<CaldavTask>>())
    }

    @Test
    fun moveCaldavTaskWithChildrenToNewCaldavList() = runTest {
        val task = Task(id = 1L)
        val caldavTask = CaldavTask(task = 1L, calendar = "old-cal", remoteId = "rid", obj = "obj.ics")
        val childCaldav = CaldavTask(task = 10L, calendar = "old-cal", remoteId = "child-rid", obj = "child.ics")
        val fromCalendar = CaldavCalendar(uuid = "old-cal", account = "acc")
        val filter = makeCaldavFilter("new-cal")
        `when`(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        `when`(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        `when`(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        `when`(caldavDao.getTask(1L)).thenReturn(caldavTask)
        `when`(taskDao.getChildren(1L)).thenReturn(listOf(10L))
        `when`(caldavDao.getTasks(listOf(10L))).thenReturn(listOf(childCaldav))
        `when`(caldavDao.getCalendar("old-cal")).thenReturn(fromCalendar)
        taskMover.move(listOf(1L), filter)
        verify(vtodoCache, Mockito.times(2)).move(any(), any(), any())
        verify(caldavDao).insert(any<Task>(), any(), Mockito.anyBoolean())
        verify(caldavDao).insert(any<Iterable<CaldavTask>>())
    }

    @Test
    fun moveAddsToTopWhenPreferenceSet() = runTest {
        `when`(preferences.addTasksToTop()).thenReturn(true)
        val task = Task(id = 1L)
        val filter = makeCaldavFilter("cal-uuid")
        `when`(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        `when`(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        `when`(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        `when`(caldavDao.getTask(1L)).thenReturn(null)
        `when`(taskDao.getChildren(1L)).thenReturn(emptyList())
        taskMover.move(listOf(1L), filter)
        verify(caldavDao).insert(any<Task>(), any(), Mockito.eq(true))
    }

    @Test
    fun moveLocalTaskWithChildrenToCaldavList() = runTest {
        val parentTask = Task(id = 1L)
        val childTask = Task(id = 10L, parent = 1L)
        val filter = makeCaldavFilter("cal-uuid")
        `when`(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        `when`(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(parentTask))
        `when`(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        `when`(caldavDao.getTask(1L)).thenReturn(null)
        `when`(taskDao.getChildren(1L)).thenReturn(listOf(10L))
        `when`(taskDao.fetch(10L)).thenReturn(childTask)
        taskMover.move(listOf(1L), filter)
        verify(caldavDao).insert(any<Task>(), any(), Mockito.anyBoolean())
        verify(caldavDao).insert(any<Iterable<CaldavTask>>())
    }

    private fun makeCaldavFilter(
        uuid: String,
        accountType: Int = TYPE_CALDAV,
        accountUuid: String = "acc-uuid",
    ): CaldavFilter = CaldavFilter(
        calendar = CaldavCalendar(uuid = uuid, account = accountUuid),
        account = CaldavAccount(uuid = accountUuid, accountType = accountType),
    )
}
