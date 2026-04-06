package com.todoroo.astrid.service

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
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

class TaskMoverDeepTest {
    private lateinit var taskDao: TaskDao
    private lateinit var caldavDao: CaldavDao
    private lateinit var googleTaskDao: GoogleTaskDao
    private lateinit var preferences: Preferences
    private lateinit var refreshBroadcaster: RefreshBroadcaster
    private lateinit var syncAdapters: SyncAdapters
    private lateinit var vtodoCache: VtodoCache
    private lateinit var taskMover: TaskMover

    @Before
    fun setUp() {
        taskDao = mock()
        caldavDao = mock()
        googleTaskDao = mock()
        preferences = mock()
        refreshBroadcaster = mock()
        syncAdapters = mock()
        vtodoCache = mock()

        runTest {
            whenever(preferences.addTasksToTop()).thenReturn(false)
            whenever(caldavDao.insert(any<CaldavTask>())).thenReturn(1L)
            whenever(caldavDao.insert(any<Task>(), any(), any())).thenReturn(1L)
            whenever(googleTaskDao.insert(any<CaldavTask>())).thenReturn(1L)
            whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
            whenever(taskDao.fetch(any<List<Long>>())).thenReturn(emptyList())
            whenever(caldavDao.getTasks(any<List<Long>>())).thenReturn(emptyList())
            whenever(caldavDao.getCalendars(any<List<Long>>())).thenReturn(emptyList())
            whenever(taskDao.getChildren(any<Long>())).thenReturn(emptyList())
        }

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

    // ========== getSingleFilter ==========

    @Test
    fun getSingleFilter_nullAccountField_returnsNull() = runTest {
        val cal = CaldavCalendar(uuid = "u", account = null)
        whenever(caldavDao.getCalendars(any<List<Long>>())).thenReturn(listOf(cal))
        assertNull(taskMover.getSingleFilter(listOf(1L)))
    }

    @Test
    fun getSingleFilter_accountNotFound_returnsNull() = runTest {
        val cal = CaldavCalendar(uuid = "u", account = "acc")
        whenever(caldavDao.getCalendars(any<List<Long>>())).thenReturn(listOf(cal))
        whenever(caldavDao.getAccountByUuid("acc")).thenReturn(null)
        assertNull(taskMover.getSingleFilter(listOf(1L)))
    }

    // ========== move(Long, Long) ==========

    @Test
    fun moveById_calendarNull_doesNothing() = runTest {
        whenever(caldavDao.getCalendarById(5L)).thenReturn(null)
        taskMover.move(1L, 5L)
        verify(taskDao, never()).getChildren(any<List<Long>>())
        verifyNoInteractions(refreshBroadcaster)
    }

    @Test
    fun moveById_accountNull_doesNothing() = runTest {
        val cal = CaldavCalendar(id = 5L, uuid = "c", account = "a")
        whenever(caldavDao.getCalendarById(5L)).thenReturn(cal)
        whenever(caldavDao.getAccountByUuid("a")).thenReturn(null)
        taskMover.move(1L, 5L)
        verifyNoInteractions(refreshBroadcaster)
    }

    @Test
    fun moveById_calendarAccountFieldNull_doesNothing() = runTest {
        val cal = CaldavCalendar(id = 5L, uuid = "c", account = null)
        whenever(caldavDao.getCalendarById(5L)).thenReturn(cal)
        taskMover.move(1L, 5L)
        verifyNoInteractions(refreshBroadcaster)
    }

    @Test
    fun moveById_success_callsMoveFlow() = runTest {
        val cal = CaldavCalendar(id = 5L, uuid = "c", account = "a")
        val acc = CaldavAccount(uuid = "a")
        val task = Task(id = 1L)
        whenever(caldavDao.getCalendarById(5L)).thenReturn(cal)
        whenever(caldavDao.getAccountByUuid("a")).thenReturn(acc)
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        whenever(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        whenever(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        whenever(caldavDao.getTask(1L)).thenReturn(null)
        taskMover.move(1L, 5L)
        verify(refreshBroadcaster).broadcastRefresh()
        verify(syncAdapters).sync(SyncSource.TASK_CHANGE)
    }

    // ========== move(List<Long>, CaldavFilter) - readOnly filtering ==========

    @Test
    fun move_allReadOnly_nothingMoved() = runTest {
        val t1 = Task(id = 1L, readOnly = true)
        val t2 = Task(id = 2L, readOnly = true)
        val filter = makeCaldavFilter("cal")
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        whenever(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(t1, t2))
        taskMover.move(listOf(1L, 2L), filter)
        verify(googleTaskDao, never()).getByTaskId(any())
        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun move_childrenExcludedFromTopLevel() = runTest {
        val parent = Task(id = 1L)
        val filter = makeCaldavFilter("cal")
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(listOf(2L))
        whenever(taskDao.fetch(listOf(1L))).thenReturn(listOf(parent))
        whenever(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        whenever(caldavDao.getTask(1L)).thenReturn(null)
        taskMover.move(listOf(1L, 2L), filter)
        verify(googleTaskDao, never()).getByTaskId(2L)
    }

    @Test
    fun move_callsUpdateParents_forNonGoogleList() = runTest {
        val task = Task(id = 1L)
        val filter = makeCaldavFilter("cal", TYPE_CALDAV)
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        whenever(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        whenever(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        whenever(caldavDao.getTask(1L)).thenReturn(null)
        taskMover.move(listOf(1L), filter)
        verify(caldavDao).updateParents("cal")
    }

    @Test
    fun move_skipsUpdateParents_forGoogleList() = runTest {
        val task = Task(id = 1L)
        val filter = makeCaldavFilter("cal", TYPE_GOOGLE_TASKS)
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        whenever(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        whenever(taskDao.fetch(1L)).thenReturn(task)
        whenever(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        whenever(caldavDao.getTask(1L)).thenReturn(null)
        taskMover.move(listOf(1L), filter)
        verify(caldavDao, never()).updateParents(any())
    }

    // ========== performMove dispatching ==========

    @Test
    fun performMove_googleTask_sameCalendar_skips() = runTest {
        val task = Task(id = 1L)
        val gt = CaldavTask(task = 1L, calendar = "cal")
        val filter = makeCaldavFilter("cal", TYPE_GOOGLE_TASKS)
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        whenever(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        whenever(googleTaskDao.getByTaskId(1L)).thenReturn(gt)
        taskMover.move(listOf(1L), filter)
        verify(caldavDao, never()).markDeleted(any(), any())
    }

    @Test
    fun performMove_caldavTask_sameCalendar_skips() = runTest {
        val task = Task(id = 1L)
        val ct = CaldavTask(task = 1L, calendar = "cal")
        val filter = makeCaldavFilter("cal", TYPE_CALDAV)
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        whenever(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        whenever(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        whenever(caldavDao.getTask(1L)).thenReturn(ct)
        taskMover.move(listOf(1L), filter)
        verify(caldavDao, never()).markDeleted(any(), any())
    }

    // ========== moveGoogleTask ==========

    @Test
    fun moveGoogleTask_toDifferentGoogleList_withChildren() = runTest {
        val task = Task(id = 1L)
        val gt = CaldavTask(task = 1L, calendar = "old")
        val filter = makeCaldavFilter("new", TYPE_GOOGLE_TASKS)
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        whenever(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        whenever(googleTaskDao.getByTaskId(1L)).thenReturn(gt)
        whenever(taskDao.getChildren(1L)).thenReturn(listOf(10L, 11L))
        taskMover.move(listOf(1L), filter)
        verify(caldavDao).markDeleted(any(), any())
        verify(googleTaskDao).insertAndShift(any(), any(), any())
        verify(googleTaskDao).insert(any<Iterable<CaldavTask>>())
    }

    @Test
    fun moveGoogleTask_toDifferentGoogleList_noChildren() = runTest {
        val task = Task(id = 1L)
        val gt = CaldavTask(task = 1L, calendar = "old")
        val filter = makeCaldavFilter("new", TYPE_GOOGLE_TASKS)
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        whenever(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        whenever(googleTaskDao.getByTaskId(1L)).thenReturn(gt)
        whenever(taskDao.getChildren(1L)).thenReturn(emptyList())
        taskMover.move(listOf(1L), filter)
        verify(caldavDao).markDeleted(any(), any())
        verify(googleTaskDao).insertAndShift(any(), any(), any())
        verify(googleTaskDao, never()).insert(any<Iterable<CaldavTask>>())
    }

    @Test
    fun moveGoogleTask_toCaldavList_noChildren() = runTest {
        val task = Task(id = 1L)
        val gt = CaldavTask(task = 1L, calendar = "old")
        val filter = makeCaldavFilter("caldav-cal", TYPE_CALDAV)
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        whenever(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        whenever(googleTaskDao.getByTaskId(1L)).thenReturn(gt)
        whenever(taskDao.getChildren(1L)).thenReturn(emptyList())
        taskMover.move(listOf(1L), filter)
        verify(caldavDao).markDeleted(any(), any())
        verify(caldavDao).insert(any<Task>(), any(), any())
        verify(caldavDao).insert(any<Iterable<CaldavTask>>())
    }

    @Test
    fun moveGoogleTask_toCaldavList_withChildren() = runTest {
        val task = Task(id = 1L)
        val gt = CaldavTask(task = 1L, calendar = "old")
        val filter = makeCaldavFilter("caldav-cal", TYPE_CALDAV)
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        whenever(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        whenever(googleTaskDao.getByTaskId(1L)).thenReturn(gt)
        whenever(taskDao.getChildren(1L)).thenReturn(listOf(10L))
        taskMover.move(listOf(1L), filter)
        verify(caldavDao).markDeleted(any(), any())
        verify(caldavDao).insert(any<Task>(), any(), any())
        verify(caldavDao).insert(any<Iterable<CaldavTask>>())
    }

    // ========== moveCaldavTask ==========

    @Test
    fun moveCaldavTask_toDifferentCaldavList_noChildren() = runTest {
        val task = Task(id = 1L)
        val ct = CaldavTask(task = 1L, calendar = "old", remoteId = "rid", obj = "obj.ics")
        val from = CaldavCalendar(uuid = "old", account = "acc")
        val filter = makeCaldavFilter("new", TYPE_CALDAV)
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        whenever(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        whenever(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        whenever(caldavDao.getTask(1L)).thenReturn(ct)
        whenever(taskDao.getChildren(1L)).thenReturn(emptyList())
        whenever(caldavDao.getCalendar("old")).thenReturn(from)
        taskMover.move(listOf(1L), filter)
        verify(caldavDao).markDeleted(any(), any())
        verify(vtodoCache).move(any(), any(), any())
        verify(caldavDao).insert(any<Task>(), any(), any())
    }

    @Test
    fun moveCaldavTask_toDifferentCaldavList_withChildren() = runTest {
        val task = Task(id = 1L)
        val ct = CaldavTask(task = 1L, calendar = "old", remoteId = "rid", obj = "o.ics")
        val childCt = CaldavTask(task = 10L, calendar = "old", remoteId = "crid", obj = "c.ics")
        val from = CaldavCalendar(uuid = "old", account = "acc")
        val filter = makeCaldavFilter("new", TYPE_CALDAV)
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        whenever(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        whenever(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        whenever(caldavDao.getTask(1L)).thenReturn(ct)
        whenever(taskDao.getChildren(1L)).thenReturn(listOf(10L))
        whenever(caldavDao.getTasks(listOf(10L))).thenReturn(listOf(childCt))
        whenever(caldavDao.getCalendar("old")).thenReturn(from)
        taskMover.move(listOf(1L), filter)
        verify(vtodoCache, times(2)).move(any(), any(), any())
        verify(caldavDao).insert(any<Task>(), any(), any())
        verify(caldavDao).insert(any<Iterable<CaldavTask>>())
    }

    @Test
    fun moveCaldavTask_toGoogleList() = runTest {
        val task = Task(id = 1L)
        val ct = CaldavTask(task = 1L, calendar = "old")
        val filter = makeCaldavFilter("gcal", TYPE_GOOGLE_TASKS)
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        whenever(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        whenever(taskDao.fetch(1L)).thenReturn(task)
        whenever(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        whenever(caldavDao.getTask(1L)).thenReturn(ct)
        whenever(taskDao.getChildren(1L)).thenReturn(emptyList())
        taskMover.move(listOf(1L), filter)
        verify(caldavDao).markDeleted(any(), any())
        verify(googleTaskDao).insertAndShift(any(), any(), any())
    }

    @Test
    fun moveCaldavTask_toGoogleList_withChildren() = runTest {
        val task = Task(id = 1L)
        val ct = CaldavTask(task = 1L, calendar = "old")
        val childCt = CaldavTask(task = 10L, calendar = "old")
        val filter = makeCaldavFilter("gcal", TYPE_GOOGLE_TASKS)
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        whenever(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        whenever(taskDao.fetch(1L)).thenReturn(task)
        whenever(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        whenever(caldavDao.getTask(1L)).thenReturn(ct)
        whenever(taskDao.getChildren(1L)).thenReturn(listOf(10L))
        whenever(caldavDao.getTasks(listOf(10L))).thenReturn(listOf(childCt))
        taskMover.move(listOf(1L), filter)
        verify(caldavDao).markDeleted(any(), any())
        verify(googleTaskDao).insertAndShift(any(), any(), any())
        verify(googleTaskDao).insert(any<Iterable<CaldavTask>>())
    }

    // ========== moveLocalTask ==========

    @Test
    fun moveLocalTask_toCaldavList_noChildren() = runTest {
        val task = Task(id = 1L)
        val filter = makeCaldavFilter("cal", TYPE_CALDAV)
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        whenever(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        whenever(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        whenever(caldavDao.getTask(1L)).thenReturn(null)
        whenever(taskDao.getChildren(1L)).thenReturn(emptyList())
        taskMover.move(listOf(1L), filter)
        verify(caldavDao).insert(any<Task>(), any(), any())
        verify(caldavDao).insert(any<Iterable<CaldavTask>>())
    }

    @Test
    fun moveLocalTask_toCaldavList_withChildren() = runTest {
        val parent = Task(id = 1L)
        val child = Task(id = 10L, parent = 1L)
        val filter = makeCaldavFilter("cal", TYPE_CALDAV)
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        whenever(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(parent))
        whenever(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        whenever(caldavDao.getTask(1L)).thenReturn(null)
        whenever(taskDao.getChildren(1L)).thenReturn(listOf(10L))
        whenever(taskDao.fetch(10L)).thenReturn(child)
        taskMover.move(listOf(1L), filter)
        verify(caldavDao).insert(any<Task>(), any(), any())
        verify(caldavDao).insert(any<Iterable<CaldavTask>>())
    }

    @Test
    fun moveLocalTask_toGoogleList_noChildren() = runTest {
        val task = Task(id = 1L)
        val filter = makeCaldavFilter("gcal", TYPE_GOOGLE_TASKS)
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        whenever(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        whenever(taskDao.fetch(1L)).thenReturn(task)
        whenever(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        whenever(caldavDao.getTask(1L)).thenReturn(null)
        whenever(taskDao.getChildren(1L)).thenReturn(emptyList())
        taskMover.move(listOf(1L), filter)
        verify(googleTaskDao).insertAndShift(any(), any(), any())
    }

    @Test
    fun moveLocalTask_toGoogleList_withChildren() = runTest {
        val task = Task(id = 1L)
        val filter = makeCaldavFilter("gcal", TYPE_GOOGLE_TASKS)
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        whenever(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        whenever(taskDao.fetch(1L)).thenReturn(task)
        whenever(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        whenever(caldavDao.getTask(1L)).thenReturn(null)
        whenever(taskDao.getChildren(1L)).thenReturn(listOf(10L))
        taskMover.move(listOf(1L), filter)
        verify(googleTaskDao).insertAndShift(any(), any(), any())
        verify(googleTaskDao).insert(any<Iterable<CaldavTask>>())
    }

    // ========== moveToGoogleTasks guard ==========

    @Test
    fun moveToGoogleTasks_notGoogleFilter_returnsEarly() = runTest {
        // moveToGoogleTasks checks isGoogleTasks and returns if false
        // This path is hit when moveCaldavTask dispatches to moveToGoogleTasks
        // but for some reason the filter isn't Google tasks
        // We test by moving a caldav task to a caldav filter (goes to else branch)
        val task = Task(id = 1L)
        val ct = CaldavTask(task = 1L, calendar = "old", remoteId = "r", obj = "o")
        val from = CaldavCalendar(uuid = "old")
        val filter = makeCaldavFilter("new", TYPE_CALDAV)
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        whenever(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        whenever(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        whenever(caldavDao.getTask(1L)).thenReturn(ct)
        whenever(taskDao.getChildren(1L)).thenReturn(emptyList())
        whenever(caldavDao.getCalendar("old")).thenReturn(from)
        taskMover.move(listOf(1L), filter)
        // Should go through caldav-to-caldav path, not google
        verify(vtodoCache).move(any(), any(), any())
        verify(caldavDao).insert(any<Task>(), any(), any())
    }

    @Test
    fun moveToGoogleTasks_taskFetchReturnsNull_earlyReturn() = runTest {
        // When moveToGoogleTasks can't fetch the task, it returns early
        val task = Task(id = 1L)
        val ct = CaldavTask(task = 1L, calendar = "old")
        val filter = makeCaldavFilter("gcal", TYPE_GOOGLE_TASKS)
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        whenever(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        whenever(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        whenever(caldavDao.getTask(1L)).thenReturn(ct)
        whenever(taskDao.getChildren(1L)).thenReturn(emptyList())
        // moveToGoogleTasks fetches by taskDao.fetch(id) - return null
        whenever(taskDao.fetch(1L)).thenReturn(null)
        taskMover.move(listOf(1L), filter)
        // Should not crash, and should not insert
        verify(googleTaskDao, never()).insertAndShift(any(), any(), any())
    }

    // ========== addTasksToTop preference ==========

    @Test
    fun move_addTasksToTop_true() = runTest {
        whenever(preferences.addTasksToTop()).thenReturn(true)
        val task = Task(id = 1L)
        val filter = makeCaldavFilter("cal", TYPE_CALDAV)
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        whenever(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        whenever(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        whenever(caldavDao.getTask(1L)).thenReturn(null)
        whenever(taskDao.getChildren(1L)).thenReturn(emptyList())
        taskMover.move(listOf(1L), filter)
        verify(caldavDao).insert(any<Task>(), any(), eq(true))
    }

    @Test
    fun move_addTasksToTop_false() = runTest {
        whenever(preferences.addTasksToTop()).thenReturn(false)
        val task = Task(id = 1L)
        val filter = makeCaldavFilter("cal", TYPE_CALDAV)
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        whenever(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        whenever(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        whenever(caldavDao.getTask(1L)).thenReturn(null)
        whenever(taskDao.getChildren(1L)).thenReturn(emptyList())
        taskMover.move(listOf(1L), filter)
        verify(caldavDao).insert(any<Task>(), any(), eq(false))
    }

    // ========== multiple tasks ==========

    @Test
    fun move_multipleTasks_eachDispatched() = runTest {
        val t1 = Task(id = 1L)
        val t2 = Task(id = 2L)
        val filter = makeCaldavFilter("cal", TYPE_CALDAV)
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        whenever(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(t1, t2))
        whenever(googleTaskDao.getByTaskId(any())).thenReturn(null)
        whenever(caldavDao.getTask(any())).thenReturn(null)
        whenever(taskDao.getChildren(any<Long>())).thenReturn(emptyList())
        taskMover.move(listOf(1L, 2L), filter)
        verify(caldavDao, times(2)).insert(any<Task>(), any(), any())
    }

    // ========== moveLocalTask children hierarchy ==========

    @Test
    fun moveLocalTask_nestedChildren_setsRemoteParent() = runTest {
        val parent = Task(id = 1L)
        val child = Task(id = 10L, parent = 1L)
        val grandchild = Task(id = 20L, parent = 10L)
        val filter = makeCaldavFilter("cal", TYPE_CALDAV)
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        whenever(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(parent))
        whenever(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        whenever(caldavDao.getTask(1L)).thenReturn(null)
        whenever(taskDao.getChildren(1L)).thenReturn(listOf(10L, 20L))
        whenever(taskDao.fetch(10L)).thenReturn(child)
        whenever(taskDao.fetch(20L)).thenReturn(grandchild)
        taskMover.move(listOf(1L), filter)
        verify(caldavDao).insert(any<Task>(), any(), any())
        verify(caldavDao).insert(any<Iterable<CaldavTask>>())
    }

    // ========== empty lists don't crash ==========

    @Test
    fun move_emptyList_doesNotCrash() = runTest {
        val filter = makeCaldavFilter("cal")
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        whenever(taskDao.fetch(any<List<Long>>())).thenReturn(emptyList())
        taskMover.move(emptyList(), filter)
        verify(refreshBroadcaster).broadcastRefresh()
        verify(syncAdapters).sync(SyncSource.TASK_CHANGE)
    }

    @Test
    fun move_callsTouchAfterMove() = runTest {
        val task = Task(id = 1L)
        val filter = makeCaldavFilter("cal")
        whenever(taskDao.getChildren(any<List<Long>>())).thenReturn(emptyList())
        whenever(taskDao.fetch(any<List<Long>>())).thenReturn(listOf(task))
        whenever(googleTaskDao.getByTaskId(1L)).thenReturn(null)
        whenever(caldavDao.getTask(1L)).thenReturn(null)
        taskMover.move(listOf(1L), filter)
        verify(taskDao).touch(any<List<Long>>(), any())
    }

    // ========== migrateLocalTasks needs getLocalList to return list with account ==========

    // Note: migrateLocalTasks calls CaldavDao.getLocalList() which is an extension function
    // and cannot be easily mocked without PowerMock. We test the move() path instead.

    private fun makeCaldavFilter(
        uuid: String,
        accountType: Int = TYPE_CALDAV,
        accountUuid: String = "acc-uuid",
    ): CaldavFilter = CaldavFilter(
        calendar = CaldavCalendar(uuid = uuid, account = accountUuid),
        account = CaldavAccount(uuid = accountUuid, accountType = accountType),
    )
}
