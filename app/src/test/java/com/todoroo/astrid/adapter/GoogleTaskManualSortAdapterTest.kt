package com.todoroo.astrid.adapter

import com.todoroo.astrid.service.TaskMover
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.tasks.LocalBroadcastManager
import org.tasks.data.TaskContainer
import org.tasks.data.TaskSaver
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task

class GoogleTaskManualSortAdapterTest {
    private lateinit var googleTaskDao: GoogleTaskDao
    private lateinit var caldavDao: CaldavDao
    private lateinit var taskDao: TaskDao
    private lateinit var taskSaver: TaskSaver
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private lateinit var taskMover: TaskMover
    private lateinit var dataSource: TaskAdapterDataSource
    private lateinit var adapter: GoogleTaskManualSortAdapter

    @Suppress("UNCHECKED_CAST")
    private fun <T> any(): T = Mockito.any<T>() as T

    @Before
    fun setUp() {
        googleTaskDao = mock(GoogleTaskDao::class.java)
        caldavDao = mock(CaldavDao::class.java)
        taskDao = mock(TaskDao::class.java)
        taskSaver = mock(TaskSaver::class.java)
        localBroadcastManager = mock(LocalBroadcastManager::class.java)
        taskMover = mock(TaskMover::class.java)
        dataSource = mock(TaskAdapterDataSource::class.java)

        adapter = GoogleTaskManualSortAdapter(
            googleTaskDao, caldavDao, taskDao, taskSaver, localBroadcastManager, taskMover
        )
        adapter.setDataSource(dataSource)
    }

    // --- moved: delegates to moveGoogleTask ---

    @Test
    fun movedToFirstPositionNoPrevious() = runTest {
        val caldavTask = CaldavTask(id = 1, task = 1, calendar = "list-1")
        val task = makeGoogleTask(id = 1, caldavTask = caldavTask)
        `when`(dataSource.getItem(2)).thenReturn(task)
        `when`(dataSource.getTaskCount()).thenReturn(5)

        adapter.moved(2, 0, 0)

        verify(googleTaskDao).move(
            task = any(),
            list = any(),
            newParent = Mockito.anyLong(),
            newPosition = Mockito.anyLong(),
        )
        verify(taskSaver).touch(listOf(1L))
        verify(localBroadcastManager).broadcastRefresh()
    }

    @Test
    fun movedDownToEndOfListIndentZero() = runTest {
        val caldavTask = CaldavTask(id = 1, task = 1, calendar = "list-1")
        val task = makeGoogleTask(id = 1, caldavTask = caldavTask)
        val previous = makeGoogleTask(id = 2, primarySort = 10)
        `when`(dataSource.getItem(0)).thenReturn(task)
        `when`(dataSource.getItem(4)).thenReturn(previous)
        `when`(dataSource.getTaskCount()).thenReturn(5)

        adapter.moved(0, 5, 0) // to == count

        verify(googleTaskDao).move(any(), any(), Mockito.anyLong(), Mockito.anyLong())
        verify(taskSaver).touch(listOf(1L))
    }

    @Test
    fun movedDownBelowFromIndentZero() = runTest {
        val caldavTask = CaldavTask(id = 1, task = 1, calendar = "list-1")
        val task = makeGoogleTask(id = 1, caldavTask = caldavTask)
        val previous = makeGoogleTask(id = 2, primarySort = 5)
        `when`(dataSource.getItem(3)).thenReturn(task)
        `when`(dataSource.getItem(1)).thenReturn(previous)
        `when`(dataSource.getTaskCount()).thenReturn(5)

        adapter.moved(3, 2, 0) // to <= from

        verify(googleTaskDao).move(any(), any(), Mockito.anyLong(), Mockito.anyLong())
        verify(taskSaver).touch(listOf(1L))
    }

    @Test
    fun movedDownBelowFromPreviousHasParentSameParent() = runTest {
        val caldavTask = CaldavTask(id = 1, task = 1, calendar = "list-1")
        val task = makeGoogleTask(id = 1, parent = 5, caldavTask = caldavTask)
        val previous = makeGoogleTask(id = 2, parent = 5, secondarySort = 3)
        `when`(dataSource.getItem(3)).thenReturn(task)
        `when`(dataSource.getItem(1)).thenReturn(previous)
        `when`(dataSource.getTaskCount()).thenReturn(5)

        adapter.moved(3, 2, 1) // indent=1, to <= from

        verify(googleTaskDao).move(any(), any(), Mockito.anyLong(), Mockito.anyLong())
        verify(taskSaver).touch(listOf(1L))
    }

    @Test
    fun movedDownBelowFromPreviousHasDifferentParent() = runTest {
        val caldavTask = CaldavTask(id = 1, task = 1, calendar = "list-1")
        val task = makeGoogleTask(id = 1, parent = 5, caldavTask = caldavTask)
        val previous = makeGoogleTask(id = 2, parent = 99, secondarySort = 3)
        `when`(dataSource.getItem(3)).thenReturn(task)
        `when`(dataSource.getItem(1)).thenReturn(previous)
        `when`(dataSource.getTaskCount()).thenReturn(5)

        adapter.moved(3, 2, 1) // indent=1, to <= from

        verify(googleTaskDao).move(any(), any(), Mockito.anyLong(), Mockito.anyLong())
        verify(taskSaver).touch(listOf(1L))
    }

    @Test
    fun movedDownBelowFromPreviousNoParentIndentPositive() = runTest {
        val caldavTask = CaldavTask(id = 1, task = 1, calendar = "list-1")
        val task = makeGoogleTask(id = 1, parent = 0, caldavTask = caldavTask)
        val previous = makeGoogleTask(id = 2, parent = 0)
        `when`(dataSource.getItem(3)).thenReturn(task)
        `when`(dataSource.getItem(1)).thenReturn(previous)
        `when`(dataSource.getTaskCount()).thenReturn(5)

        adapter.moved(3, 2, 1) // indent=1, to <= from

        verify(googleTaskDao).move(any(), any(), Mockito.anyLong(), Mockito.anyLong())
        verify(taskSaver).touch(listOf(1L))
    }

    @Test
    fun movedUpAboveFromIndentZeroTaskHasParent() = runTest {
        val caldavTask = CaldavTask(id = 1, task = 1, calendar = "list-1")
        val task = makeGoogleTask(id = 1, parent = 5, caldavTask = caldavTask)
        val previous = makeGoogleTask(id = 2, primarySort = 10, parent = 0)
        `when`(dataSource.getItem(0)).thenReturn(task)
        `when`(dataSource.getItem(2)).thenReturn(previous)
        `when`(dataSource.getTaskCount()).thenReturn(5)

        adapter.moved(0, 3, 0) // to > from, indent=0

        verify(googleTaskDao).move(any(), any(), Mockito.anyLong(), Mockito.anyLong())
        verify(taskSaver).touch(listOf(1L))
    }

    @Test
    fun movedUpAboveFromIndentZeroTaskNoParent() = runTest {
        val caldavTask = CaldavTask(id = 1, task = 1, calendar = "list-1")
        val task = makeGoogleTask(id = 1, parent = 0, caldavTask = caldavTask)
        val previous = makeGoogleTask(id = 2, primarySort = 10, parent = 0)
        `when`(dataSource.getItem(0)).thenReturn(task)
        `when`(dataSource.getItem(2)).thenReturn(previous)
        `when`(dataSource.getTaskCount()).thenReturn(5)

        adapter.moved(0, 3, 0)

        verify(googleTaskDao).move(any(), any(), Mockito.anyLong(), Mockito.anyLong())
        verify(taskSaver).touch(listOf(1L))
    }

    @Test
    fun movedUpAboveFromPreviousHasParentSameParent() = runTest {
        val caldavTask = CaldavTask(id = 1, task = 1, calendar = "list-1")
        val task = makeGoogleTask(id = 1, parent = 5, caldavTask = caldavTask)
        val previous = makeGoogleTask(id = 2, parent = 5, secondarySort = 3)
        `when`(dataSource.getItem(0)).thenReturn(task)
        `when`(dataSource.getItem(2)).thenReturn(previous)
        `when`(dataSource.getTaskCount()).thenReturn(5)

        adapter.moved(0, 3, 1) // indent=1, to > from

        verify(googleTaskDao).move(any(), any(), Mockito.anyLong(), Mockito.anyLong())
        verify(taskSaver).touch(listOf(1L))
    }

    @Test
    fun movedUpAboveFromPreviousHasDifferentParent() = runTest {
        val caldavTask = CaldavTask(id = 1, task = 1, calendar = "list-1")
        val task = makeGoogleTask(id = 1, parent = 5, caldavTask = caldavTask)
        val previous = makeGoogleTask(id = 2, parent = 99, secondarySort = 3)
        `when`(dataSource.getItem(0)).thenReturn(task)
        `when`(dataSource.getItem(2)).thenReturn(previous)
        `when`(dataSource.getTaskCount()).thenReturn(5)

        adapter.moved(0, 3, 1) // indent=1, to > from

        verify(googleTaskDao).move(any(), any(), Mockito.anyLong(), Mockito.anyLong())
        verify(taskSaver).touch(listOf(1L))
    }

    @Test
    fun movedUpAboveFromPreviousNoParent() = runTest {
        val caldavTask = CaldavTask(id = 1, task = 1, calendar = "list-1")
        val task = makeGoogleTask(id = 1, parent = 0, caldavTask = caldavTask)
        val previous = makeGoogleTask(id = 2, parent = 0)
        `when`(dataSource.getItem(0)).thenReturn(task)
        `when`(dataSource.getItem(2)).thenReturn(previous)
        `when`(dataSource.getTaskCount()).thenReturn(5)

        adapter.moved(0, 3, 1) // indent=1, to > from

        verify(googleTaskDao).move(any(), any(), Mockito.anyLong(), Mockito.anyLong())
        verify(taskSaver).touch(listOf(1L))
    }

    @Test
    fun movedReturnsEarlyWhenNoCaldavTask() = runTest {
        val task = makeGoogleTask(id = 1, caldavTask = null)
        `when`(dataSource.getItem(0)).thenReturn(task)
        `when`(dataSource.getTaskCount()).thenReturn(5)

        adapter.moved(0, 2, 0)

        Mockito.verifyNoInteractions(taskSaver)
    }

    @Test
    fun movedReturnsEarlyWhenNoCalendar() = runTest {
        val caldavTask = CaldavTask(id = 1, task = 1, calendar = null)
        val task = makeGoogleTask(id = 1, caldavTask = caldavTask)
        `when`(dataSource.getItem(0)).thenReturn(task)
        `when`(dataSource.getTaskCount()).thenReturn(5)

        adapter.moved(0, 2, 0)

        Mockito.verifyNoInteractions(taskSaver)
    }

    // --- Helper ---

    private fun makeGoogleTask(
        id: Long = 0,
        uuid: String = "uuid-$id",
        parent: Long = 0,
        caldavTask: CaldavTask? = CaldavTask(id = id, task = id, calendar = "list-1"),
        primarySort: Long = 0,
        secondarySort: Long = 0,
    ): TaskContainer {
        val task = Task(id = id, remoteId = uuid, parent = parent)
        return TaskContainer(
            task = task,
            caldavTask = caldavTask,
            accountType = TYPE_GOOGLE_TASKS,
            primarySort = primarySort,
            secondarySort = secondarySort,
        )
    }
}
