package com.todoroo.astrid.adapter

import com.todoroo.astrid.subtasks.SubtasksFilterUpdater
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
import org.tasks.data.TaskContainer
import org.tasks.data.TaskSaver
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task
import org.tasks.data.entity.TaskListMetadata
import org.tasks.filters.AstridOrderingFilter
import com.todoroo.astrid.service.TaskMover

class AstridTaskAdapterTest {
    private lateinit var list: TaskListMetadata
    private lateinit var filter: AstridOrderingFilter
    private lateinit var updater: SubtasksFilterUpdater
    private lateinit var googleTaskDao: GoogleTaskDao
    private lateinit var caldavDao: CaldavDao
    private lateinit var taskDao: TaskDao
    private lateinit var taskSaver: TaskSaver
    private lateinit var refreshBroadcaster: RefreshBroadcaster
    private lateinit var taskMover: TaskMover
    private lateinit var dataSource: TaskAdapterDataSource
    private lateinit var adapter: AstridTaskAdapter

    @Suppress("UNCHECKED_CAST")
    private fun <T> any(): T = Mockito.any<T>() as T

    @Before
    fun setUp() {
        list = TaskListMetadata()
        filter = mock(AstridOrderingFilter::class.java)
        updater = mock(SubtasksFilterUpdater::class.java)
        googleTaskDao = mock(GoogleTaskDao::class.java)
        caldavDao = mock(CaldavDao::class.java)
        taskDao = mock(TaskDao::class.java)
        taskSaver = mock(TaskSaver::class.java)
        refreshBroadcaster = mock(RefreshBroadcaster::class.java)
        taskMover = mock(TaskMover::class.java)
        dataSource = mock(TaskAdapterDataSource::class.java)

        adapter = AstridTaskAdapter(
            list, filter, updater, googleTaskDao, caldavDao, taskDao, taskSaver,
            refreshBroadcaster, taskMover
        )
        adapter.setDataSource(dataSource)
    }

    // --- getIndent ---

    @Test
    fun getIndentDelegatesToUpdater() {
        val task = makeTask(id = 1, uuid = "uuid-1")
        `when`(updater.getIndentForTask("uuid-1")).thenReturn(3)
        assertEquals(3, adapter.getIndent(task))
    }

    @Test
    fun getIndentReturnsZeroWhenUpdaterReturnsZero() {
        val task = makeTask(id = 1, uuid = "uuid-1")
        `when`(updater.getIndentForTask("uuid-1")).thenReturn(0)
        assertEquals(0, adapter.getIndent(task))
    }

    // --- canMove ---

    @Test
    fun canMoveReturnsTrueWhenTargetIsNotDescendant() {
        val source = makeTask(id = 1, uuid = "source")
        val target = makeTask(id = 2, uuid = "target")
        `when`(updater.isDescendantOf("target", "source")).thenReturn(false)
        assertTrue(adapter.canMove(source, 0, target, 1))
    }

    @Test
    fun canMoveReturnsFalseWhenTargetIsDescendant() {
        val source = makeTask(id = 1, uuid = "source")
        val target = makeTask(id = 2, uuid = "target")
        `when`(updater.isDescendantOf("target", "source")).thenReturn(true)
        assertFalse(adapter.canMove(source, 0, target, 1))
    }

    // --- maxIndent ---

    @Test
    fun maxIndentReturnsPreviousIndentPlusOne() {
        val previous = makeTask(id = 1, uuid = "prev-uuid")
        val task = makeTask(id = 2, uuid = "task-uuid")
        `when`(dataSource.getItem(0)).thenReturn(previous)
        `when`(updater.getIndentForTask("prev-uuid")).thenReturn(2)
        assertEquals(3, adapter.maxIndent(0, task))
    }

    @Test
    fun maxIndentReturnsOneWhenPreviousHasZeroIndent() {
        val previous = makeTask(id = 1, uuid = "prev-uuid")
        val task = makeTask(id = 2, uuid = "task-uuid")
        `when`(dataSource.getItem(0)).thenReturn(previous)
        `when`(updater.getIndentForTask("prev-uuid")).thenReturn(0)
        assertEquals(1, adapter.maxIndent(0, task))
    }

    // --- supportsAstridSorting ---

    @Test
    fun supportsAstridSortingReturnsTrue() {
        assertTrue(adapter.supportsAstridSorting())
    }

    // --- supportsHiddenTasks ---

    @Test
    fun supportsHiddenTasksReturnsFalse() {
        assertFalse(adapter.supportsHiddenTasks())
    }

    // --- moved ---

    @Test
    fun movedToEndOfList() = runTest {
        val source = makeTask(id = 1, uuid = "source-uuid")
        `when`(dataSource.getTaskCount()).thenReturn(5)
        `when`(dataSource.getItem(2)).thenReturn(source)
        `when`(updater.getIndentForTask("source-uuid")).thenReturn(0)

        adapter.moved(2, 5, 0) // to >= count

        verify(updater).moveTo(list, filter, "source-uuid", "-1")
        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun movedToSpecificPosition() = runTest {
        val source = makeTask(id = 1, uuid = "source-uuid")
        val dest = makeTask(id = 2, uuid = "dest-uuid")
        `when`(dataSource.getTaskCount()).thenReturn(5)
        `when`(dataSource.getItem(0)).thenReturn(source)
        `when`(dataSource.getItem(3)).thenReturn(dest)
        `when`(updater.getIndentForTask("source-uuid")).thenReturn(0)

        adapter.moved(0, 3, 0)

        verify(updater).moveTo(list, filter, "source-uuid", "dest-uuid")
        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun movedWithIndentIncrease() = runTest {
        val source = makeTask(id = 1, uuid = "source-uuid")
        val dest = makeTask(id = 2, uuid = "dest-uuid")
        `when`(dataSource.getTaskCount()).thenReturn(5)
        `when`(dataSource.getItem(0)).thenReturn(source)
        `when`(dataSource.getItem(3)).thenReturn(dest)
        `when`(updater.getIndentForTask("source-uuid")).thenReturn(0)

        adapter.moved(0, 3, 2)

        verify(updater).moveTo(list, filter, "source-uuid", "dest-uuid")
        verify(updater, Mockito.times(2)).indent(list, filter, "source-uuid", 2)
        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun movedWithIndentDecrease() = runTest {
        val source = makeTask(id = 1, uuid = "source-uuid")
        val dest = makeTask(id = 2, uuid = "dest-uuid")
        `when`(dataSource.getTaskCount()).thenReturn(5)
        `when`(dataSource.getItem(0)).thenReturn(source)
        `when`(dataSource.getItem(3)).thenReturn(dest)
        `when`(updater.getIndentForTask("source-uuid")).thenReturn(3)

        adapter.moved(0, 3, 1)

        verify(updater).moveTo(list, filter, "source-uuid", "dest-uuid")
        verify(updater, Mockito.times(2)).indent(list, filter, "source-uuid", -2)
        verify(refreshBroadcaster).broadcastRefresh()
    }

    @Test
    fun movedWithNoIndentChange() = runTest {
        val source = makeTask(id = 1, uuid = "source-uuid")
        val dest = makeTask(id = 2, uuid = "dest-uuid")
        `when`(dataSource.getTaskCount()).thenReturn(5)
        `when`(dataSource.getItem(0)).thenReturn(source)
        `when`(dataSource.getItem(3)).thenReturn(dest)
        `when`(updater.getIndentForTask("source-uuid")).thenReturn(2)

        adapter.moved(0, 3, 2)

        verify(updater).moveTo(list, filter, "source-uuid", "dest-uuid")
        verify(updater, never()).indent(any(), any(), any(), Mockito.anyInt())
        verify(refreshBroadcaster).broadcastRefresh()
    }

    // --- onTaskCreated ---

    @Test
    fun onTaskCreatedDelegatesToUpdater() = runTest {
        adapter.onTaskCreated("new-uuid")
        verify(updater).onCreateTask(list, filter, "new-uuid")
    }

    // --- onTaskDeleted ---

    @Test
    fun onTaskDeletedDelegatesToUpdater() = runTest {
        val task = Task(id = 1, remoteId = "uuid-1")
        adapter.onTaskDeleted(task)
        verify(updater).onDeleteTask(list, filter, "uuid-1")
    }

    // --- onCompletedTask (completing - newState = true) ---

    @Test
    fun onCompletedTaskCompletesDescendants() = runTest {
        `when`(taskDao.getRecurringTasks(any())).thenReturn(emptyList())

        Mockito.doAnswer { invocation ->
            val visitor = invocation.getArgument<suspend (SubtasksFilterUpdater.Node) -> Unit>(1)
            val child1 = SubtasksFilterUpdater.Node("child-1", null, 1)
            val child2 = SubtasksFilterUpdater.Node("child-2", null, 1)
            kotlinx.coroutines.runBlocking {
                visitor(child1)
                visitor(child2)
            }
            null
        }.`when`(updater).applyToDescendants(any(), any())

        adapter.onCompletedTask("parent-uuid", true)

        // Verify setCompletionDate was called (3 JVM params due to default updateTime)
        verify(taskDao, Mockito.atLeast(2)).setCompletionDate(any(), Mockito.anyLong(), Mockito.anyLong())
        verify(taskDao).getRecurringTasks(any())
    }

    @Test
    fun onCompletedTaskMovesRecurringDescendantsToParent() = runTest {
        val recurringTask = Task(id = 10, remoteId = "child-recurring", recurrence = "RRULE:FREQ=DAILY")
        `when`(taskDao.getRecurringTasks(any())).thenReturn(listOf(recurringTask))

        Mockito.doAnswer { invocation ->
            val visitor = invocation.getArgument<suspend (SubtasksFilterUpdater.Node) -> Unit>(1)
            val child = SubtasksFilterUpdater.Node("child-recurring", null, 1)
            kotlinx.coroutines.runBlocking { visitor(child) }
            null
        }.`when`(updater).applyToDescendants(any(), any())

        adapter.onCompletedTask("parent-uuid", true)

        verify(updater).moveToParentOf("child-recurring", "parent-uuid")
        verify(updater).writeSerialization(any(), any())
    }

    @Test
    fun onCompletedTaskNoRecurringTasksNoSerialization() = runTest {
        val nonRecurringTask = Task(id = 10, remoteId = "child-1", recurrence = null)
        `when`(taskDao.getRecurringTasks(any())).thenReturn(listOf(nonRecurringTask))

        Mockito.doAnswer { invocation ->
            val visitor = invocation.getArgument<suspend (SubtasksFilterUpdater.Node) -> Unit>(1)
            val child = SubtasksFilterUpdater.Node("child-1", null, 1)
            kotlinx.coroutines.runBlocking { visitor(child) }
            null
        }.`when`(updater).applyToDescendants(any(), any())

        adapter.onCompletedTask("parent-uuid", true)

        verify(updater, never()).moveToParentOf(any(), any())
        verify(updater, never()).writeSerialization(any(), any())
    }

    @Test
    fun onCompletedTaskWithNoDescendants() = runTest {
        Mockito.doAnswer { null }
            .`when`(updater).applyToDescendants(any(), any())

        adapter.onCompletedTask("parent-uuid", true)

        verify(taskDao, never()).setCompletionDate(any(), Mockito.anyLong(), Mockito.anyLong())
        verify(taskDao, never()).getRecurringTasks(any())
    }

    // --- onCompletedTask (uncompleting - newState = false) ---

    @Test
    fun onUncompletedTaskUncompletesCachedDescendants() = runTest {
        `when`(taskDao.getRecurringTasks(any())).thenReturn(emptyList())
        Mockito.doAnswer { invocation ->
            val visitor = invocation.getArgument<suspend (SubtasksFilterUpdater.Node) -> Unit>(1)
            val child = SubtasksFilterUpdater.Node("child-1", null, 1)
            kotlinx.coroutines.runBlocking { visitor(child) }
            null
        }.`when`(updater).applyToDescendants(any(), any())

        adapter.onCompletedTask("parent-uuid", true)

        // Now uncomplete
        adapter.onCompletedTask("parent-uuid", false)

        // setCompletionDate should be called for uncomplete too (with completionDate=0)
        verify(taskDao, Mockito.atLeast(2)).setCompletionDate(any(), Mockito.anyLong(), Mockito.anyLong())
    }

    @Test
    fun onUncompletedTaskWithNoCachedChainedCompletions() = runTest {
        adapter.onCompletedTask("no-cache-uuid", false)

        verify(taskDao, never()).setCompletionDate(any(), Mockito.anyLong(), Mockito.anyLong())
    }

    @Test
    fun onCompletedTaskWithEmptyRecurrence() = runTest {
        val taskWithEmptyRecurrence = Task(id = 10, remoteId = "child-1", recurrence = "")
        `when`(taskDao.getRecurringTasks(any())).thenReturn(listOf(taskWithEmptyRecurrence))

        Mockito.doAnswer { invocation ->
            val visitor = invocation.getArgument<suspend (SubtasksFilterUpdater.Node) -> Unit>(1)
            val child = SubtasksFilterUpdater.Node("child-1", null, 1)
            kotlinx.coroutines.runBlocking { visitor(child) }
            null
        }.`when`(updater).applyToDescendants(any(), any())

        adapter.onCompletedTask("parent-uuid", true)

        verify(updater, never()).moveToParentOf(any(), any())
        verify(updater, never()).writeSerialization(any(), any())
    }

    @Test
    fun onCompletedTaskWithMultipleRecurringDescendants() = runTest {
        val recurring1 = Task(id = 10, remoteId = "child-1", recurrence = "RRULE:FREQ=DAILY")
        val recurring2 = Task(id = 11, remoteId = "child-2", recurrence = "RRULE:FREQ=WEEKLY")
        `when`(taskDao.getRecurringTasks(any())).thenReturn(listOf(recurring1, recurring2))

        Mockito.doAnswer { invocation ->
            val visitor = invocation.getArgument<suspend (SubtasksFilterUpdater.Node) -> Unit>(1)
            val child1 = SubtasksFilterUpdater.Node("child-1", null, 1)
            val child2 = SubtasksFilterUpdater.Node("child-2", null, 1)
            kotlinx.coroutines.runBlocking {
                visitor(child1)
                visitor(child2)
            }
            null
        }.`when`(updater).applyToDescendants(any(), any())

        adapter.onCompletedTask("parent-uuid", true)

        verify(updater).moveToParentOf("child-1", "parent-uuid")
        verify(updater).moveToParentOf("child-2", "parent-uuid")
        verify(updater).writeSerialization(any(), any())
    }

    // --- Helper ---

    private fun makeTask(
        id: Long = 0,
        uuid: String = "uuid",
        parent: Long = 0,
        indent: Int = 0,
        children: Int = 0,
        caldavTask: CaldavTask? = null,
        accountType: Int = 0,
        primarySort: Long = 0,
        secondarySort: Long = 0,
    ): TaskContainer {
        val task = Task(id = id, remoteId = uuid, parent = parent)
        return TaskContainer(
            task = task,
            caldavTask = caldavTask,
            indent = indent,
            children = children,
            accountType = accountType,
            primarySort = primarySort,
            secondarySort = secondarySort,
        )
    }
}
