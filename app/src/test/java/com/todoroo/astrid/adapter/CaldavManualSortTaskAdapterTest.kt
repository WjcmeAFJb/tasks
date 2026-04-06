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
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_LOCAL
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task

class CaldavManualSortTaskAdapterTest {
    private lateinit var googleTaskDao: GoogleTaskDao
    private lateinit var caldavDao: CaldavDao
    private lateinit var taskDao: TaskDao
    private lateinit var taskSaver: TaskSaver
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private lateinit var taskMover: TaskMover
    private lateinit var dataSource: TaskAdapterDataSource
    private lateinit var adapter: CaldavManualSortTaskAdapter

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

        adapter = CaldavManualSortTaskAdapter(
            googleTaskDao, caldavDao, taskDao, taskSaver, localBroadcastManager, taskMover
        )
        adapter.setDataSource(dataSource)
    }

    // --- moved: delegates to moveCaldavTask ---

    @Test
    fun movedSamePositionSameParentReturnsEarly() = runTest {
        val caldavTask = CaldavTask(id = 1, task = 1, calendar = "list-1", remoteId = "remote-1")
        val task = makeCaldavTask(id = 1, parent = 0, caldavTask = caldavTask, indent = 0)
        `when`(dataSource.getItem(2)).thenReturn(task)
        `when`(dataSource.getTaskCount()).thenReturn(5)

        // from == to and parent doesn't change -> returns early
        adapter.moved(2, 2, 0)

        Mockito.verify(taskSaver, Mockito.never()).touch(any())
    }

    @Test
    fun movedToDifferentPositionWithPreviousNull() = runTest {
        val caldavTask = CaldavTask(id = 1, task = 1, calendar = "list-1", remoteId = "remote-1")
        val task = makeCaldavTask(id = 1, parent = 0, caldavTask = caldavTask, indent = 0, secondarySort = 5)
        val next = makeCaldavTask(id = 2, parent = 0, indent = 0, primarySort = 10)
        `when`(dataSource.getItem(2)).thenReturn(task)
        `when`(dataSource.getItem(0)).thenReturn(next)
        `when`(dataSource.getTaskCount()).thenReturn(5)

        // to = 0, previous = null -> newPosition = next.caldavSortOrder - 1
        adapter.moved(2, 0, 0)

        verify(caldavDao).move(
            task = any(),
            previousParent = Mockito.anyLong(),
            newParent = Mockito.anyLong(),
            newPosition = any(),
        )
        verify(taskSaver).touch(listOf(1L))
        verify(localBroadcastManager).broadcastRefresh()
    }

    @Test
    fun movedIndentGreaterThanPreviousWithNextAtSameIndent() = runTest {
        val caldavTask = CaldavTask(id = 1, task = 1, calendar = "list-1", remoteId = "remote-1")
        val task = makeCaldavTask(id = 1, parent = 0, caldavTask = caldavTask, indent = 0)
        val previous = makeCaldavTask(id = 2, parent = 0, indent = 0, primarySort = 5)
        val next = makeCaldavTask(id = 3, parent = 2, indent = 1, secondarySort = 20)
        `when`(dataSource.getItem(0)).thenReturn(task)
        `when`(dataSource.getItem(1)).thenReturn(previous)
        `when`(dataSource.getItem(2)).thenReturn(next)
        `when`(dataSource.getTaskCount()).thenReturn(5)
        `when`(caldavDao.getTask(Mockito.anyLong())).thenReturn(
            CaldavTask(id = 2, task = 2, calendar = "list-1", remoteId = "remote-2")
        )

        adapter.moved(0, 2, 1)

        verify(caldavDao).move(
            task = any(),
            previousParent = Mockito.anyLong(),
            newParent = Mockito.anyLong(),
            newPosition = any(),
        )
        verify(taskSaver).touch(listOf(1L))
    }

    @Test
    fun movedSameIndentAsPrevious() = runTest {
        val caldavTask = CaldavTask(id = 1, task = 1, calendar = "list-1", remoteId = "remote-1")
        val task = makeCaldavTask(id = 1, parent = 0, caldavTask = caldavTask, indent = 0, primarySort = 3)
        val previous = makeCaldavTask(id = 2, parent = 0, indent = 0, primarySort = 5)
        val next = makeCaldavTask(id = 3, parent = 0, indent = 0, primarySort = 8)
        `when`(dataSource.getItem(0)).thenReturn(task)
        `when`(dataSource.getItem(1)).thenReturn(previous)
        `when`(dataSource.getItem(2)).thenReturn(next)
        `when`(dataSource.getTaskCount()).thenReturn(5)

        adapter.moved(0, 2, 0)

        verify(caldavDao).move(
            task = any(),
            previousParent = Mockito.anyLong(),
            newParent = Mockito.anyLong(),
            newPosition = any(),
        )
        verify(taskSaver).touch(listOf(1L))
    }

    @Test
    fun movedIndentGreaterThanPreviousNoNext() = runTest {
        val caldavTask = CaldavTask(id = 1, task = 1, calendar = "list-1", remoteId = "remote-1")
        val task = makeCaldavTask(id = 1, parent = 0, caldavTask = caldavTask, indent = 0)
        val previous = makeCaldavTask(id = 2, parent = 0, indent = 0, primarySort = 5)
        `when`(dataSource.getItem(0)).thenReturn(task)
        `when`(dataSource.getItem(1)).thenReturn(previous)
        `when`(dataSource.getTaskCount()).thenReturn(2) // no next
        `when`(caldavDao.getTask(Mockito.anyLong())).thenReturn(
            CaldavTask(id = 2, task = 2, calendar = "list-1", remoteId = "remote-2")
        )

        adapter.moved(0, 2, 1)

        verify(caldavDao).move(
            task = any(),
            previousParent = Mockito.anyLong(),
            newParent = Mockito.anyLong(),
            newPosition = Mockito.isNull(),
        )
    }

    @Test
    fun movedChangesParentAcrossCalendars() = runTest {
        val caldavTask = CaldavTask(id = 1, task = 1, calendar = "list-1", remoteId = "remote-1")
        val task = makeCaldavTask(id = 1, parent = 0, caldavTask = caldavTask, indent = 0)
        val previous = makeCaldavTask(id = 2, parent = 0, indent = 0, primarySort = 5,
            caldavTask = CaldavTask(id = 2, task = 2, calendar = "list-2", remoteId = "remote-2"))
        `when`(dataSource.getItem(0)).thenReturn(task)
        `when`(dataSource.getItem(1)).thenReturn(previous)
        `when`(dataSource.getTaskCount()).thenReturn(2)
        `when`(caldavDao.getTask(Mockito.anyLong())).thenReturn(
            CaldavTask(id = 2, task = 2, calendar = "list-2", remoteId = "remote-2")
        )

        adapter.moved(0, 2, 1)

        verify(caldavDao).markDeleted(any(), Mockito.anyLong())
        verify(caldavDao).insert(any<CaldavTask>())
    }

    // --- Helper ---

    private fun makeCaldavTask(
        id: Long = 0,
        uuid: String = "uuid-$id",
        parent: Long = 0,
        indent: Int = 0,
        caldavTask: CaldavTask? = CaldavTask(id = id, task = id, calendar = "list-1", remoteId = "remote-$id"),
        primarySort: Long = 0,
        secondarySort: Long = 0,
    ): TaskContainer {
        val task = Task(id = id, remoteId = uuid, parent = parent)
        return TaskContainer(
            task = task,
            caldavTask = caldavTask,
            accountType = TYPE_LOCAL,
            indent = indent,
            primarySort = primarySort,
            secondarySort = secondarySort,
        )
    }
}
