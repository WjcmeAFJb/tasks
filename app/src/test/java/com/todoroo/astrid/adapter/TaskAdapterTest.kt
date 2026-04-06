package com.todoroo.astrid.adapter

import com.todoroo.astrid.core.SortHelper.SORT_DUE
import com.todoroo.astrid.core.SortHelper.SORT_IMPORTANCE
import com.todoroo.astrid.core.SortHelper.SORT_LIST
import com.todoroo.astrid.core.SortHelper.SORT_MANUAL
import com.todoroo.astrid.core.SortHelper.SORT_START
import com.todoroo.astrid.service.TaskMover
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
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.tasks.broadcast.RefreshBroadcaster
import org.tasks.data.TaskContainer
import org.tasks.data.TaskSaver
import org.tasks.data.dao.CaldavDao
import org.tasks.data.dao.GoogleTaskDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_LOCAL
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Task

class TaskAdapterTest {
    private lateinit var googleTaskDao: GoogleTaskDao
    private lateinit var caldavDao: CaldavDao
    private lateinit var taskDao: TaskDao
    private lateinit var taskSaver: TaskSaver
    private lateinit var refreshBroadcaster: RefreshBroadcaster
    private lateinit var taskMover: TaskMover
    private lateinit var dataSource: TaskAdapterDataSource
    private lateinit var adapter: TaskAdapter

    @Suppress("UNCHECKED_CAST")
    private fun <T> any(): T = Mockito.any<T>() as T

    @Before
    fun setUp() {
        googleTaskDao = mock(GoogleTaskDao::class.java)
        caldavDao = mock(CaldavDao::class.java)
        taskDao = mock(TaskDao::class.java)
        taskSaver = mock(TaskSaver::class.java)
        refreshBroadcaster = mock(RefreshBroadcaster::class.java)
        taskMover = mock(TaskMover::class.java)
        dataSource = mock(TaskAdapterDataSource::class.java)

        adapter = TaskAdapter(
            false, googleTaskDao, caldavDao, taskDao, taskSaver, refreshBroadcaster, taskMover
        )
        adapter.setDataSource(dataSource)
    }

    // --- count ---

    @Test
    fun countDelegatesToDataSource() {
        `when`(dataSource.getTaskCount()).thenReturn(42)
        assertEquals(42, adapter.count)
    }

    // --- getIndent ---

    @Test
    fun getIndentReturnsTaskIndent() {
        val task = makeTask(id = 1, indent = 3)
        assertEquals(3, adapter.getIndent(task))
    }

    // --- selection management ---

    @Test
    fun initialSelectionIsEmpty() {
        assertEquals(0, adapter.numSelected)
        assertTrue(adapter.getSelected().isEmpty())
    }

    @Test
    fun toggleSelectionAddsTask() {
        val task = makeTask(id = 1)
        adapter.toggleSelection(task)
        assertTrue(adapter.isSelected(task))
        assertEquals(1, adapter.numSelected)
    }

    @Test
    fun toggleSelectionRemovesTask() {
        val task = makeTask(id = 1)
        adapter.toggleSelection(task)
        adapter.toggleSelection(task)
        assertFalse(adapter.isSelected(task))
        assertEquals(0, adapter.numSelected)
    }

    @Test
    fun setSelectedOverridesExisting() {
        val task1 = makeTask(id = 1)
        val task2 = makeTask(id = 2)
        adapter.toggleSelection(task1)
        adapter.setSelected(listOf(2L, 3L))
        assertFalse(adapter.isSelected(task1))
        assertTrue(adapter.isSelected(task2))
        assertEquals(2, adapter.numSelected)
    }

    @Test
    fun clearSelectionsRemovesAll() {
        adapter.toggleSelection(makeTask(id = 1))
        adapter.toggleSelection(makeTask(id = 2))
        adapter.clearSelections()
        assertEquals(0, adapter.numSelected)
    }

    @Test
    fun getSelectedReturnsNewList() {
        adapter.toggleSelection(makeTask(id = 5))
        val selected = adapter.getSelected()
        assertEquals(listOf(5L), selected)
        selected.clear()
        assertEquals(1, adapter.numSelected)
    }

    // --- isHeader / getTask / getItemUuid ---

    @Test
    fun isHeaderDelegatesToDataSource() {
        `when`(dataSource.isHeader(3)).thenReturn(true)
        assertTrue(adapter.isHeader(3))
    }

    @Test
    fun isHeaderReturnsFalse() {
        `when`(dataSource.isHeader(4)).thenReturn(false)
        assertFalse(adapter.isHeader(4))
    }

    @Test
    fun getTaskDelegatesToDataSource() {
        val task = makeTask(id = 5)
        `when`(dataSource.getItem(2)).thenReturn(task)
        assertEquals(task, adapter.getTask(2))
    }

    @Test
    fun getItemUuidReturnsTaskUuid() {
        val task = makeTask(id = 5, uuid = "my-uuid")
        `when`(dataSource.getItem(2)).thenReturn(task)
        assertEquals("my-uuid", adapter.getItemUuid(2))
    }

    // --- supportsAstridSorting / supportsHiddenTasks ---

    @Test
    fun supportsAstridSortingReturnsFalseByDefault() {
        assertFalse(adapter.supportsAstridSorting())
    }

    @Test
    fun supportsHiddenTasksReturnsTrueByDefault() {
        assertTrue(adapter.supportsHiddenTasks())
    }

    // --- canMove with non-single-level-subtask (uses taskIsChild) ---

    @Test
    fun canMoveReturnsTrueWhenTargetIsNotChild() {
        val source = makeTask(id = 1, parent = 0)
        val target = makeTask(id = 2, parent = 0)
        `when`(dataSource.getItem(1)).thenReturn(target)
        `when`(dataSource.isHeader(1)).thenReturn(false)
        assertTrue(adapter.canMove(source, 0, target, 1))
    }

    @Test
    fun canMoveReturnsFalseWhenTargetIsChild() {
        val source = makeTask(id = 1, parent = 0)
        val child = makeTask(id = 2, parent = 1) // child of source
        `when`(dataSource.getItem(1)).thenReturn(child)
        `when`(dataSource.isHeader(1)).thenReturn(false)
        assertFalse(adapter.canMove(source, 0, child, 1))
    }

    @Test
    fun canMoveReturnsTrueWhenTargetIsChildOfDifferentParent() {
        val source = makeTask(id = 1, parent = 0)
        // target's parent is 99, not source.id(1) or source.parent(0)
        val target = makeTask(id = 3, parent = 99)
        // Walk back: at position 2, getTask(2) -> target with parent=99
        // 99 != 0L, 99 != source.parent(0), 99 != source.id(1) -> continue
        // at position 1, getTask(1) -> task with parent=0 -> returns false from taskIsChild
        `when`(dataSource.getItem(2)).thenReturn(target)
        `when`(dataSource.isHeader(2)).thenReturn(false)
        `when`(dataSource.getItem(1)).thenReturn(makeTask(id = 4, parent = 0))
        `when`(dataSource.isHeader(1)).thenReturn(false)
        `when`(dataSource.getItem(0)).thenReturn(source)
        `when`(dataSource.isHeader(0)).thenReturn(false)
        assertTrue(adapter.canMove(source, 0, target, 2))
    }

    @Test
    fun canMoveReturnsTrueWhenHeaderInPath() {
        // For the header scenario: the walk starts at destinationIndex and goes downTo 0.
        // If we encounter a header, taskIsChild returns false (not a child).
        val source = makeTask(id = 1, parent = 0)
        // target at position 3, its parent is source.id(1) which looks like a child
        // but there's a header at position 2, so taskIsChild walks 3->header at 2 -> false
        val target = makeTask(id = 5, parent = 1)
        `when`(dataSource.getItem(3)).thenReturn(target)
        `when`(dataSource.isHeader(3)).thenReturn(false)
        `when`(dataSource.isHeader(2)).thenReturn(true)
        // canMove calls taskIsChild(source, 3)
        // it=3: isHeader(3)=false, getTask(3).parent=1, 1!=0(parent of 0L), 1!=0(source.parent), 1==1(source.id) -> true
        // So we'd still get true from taskIsChild = child of source.
        // Actually the walk starts from destinationIndex, not destinationIndex-1.
        // Let me reconsider: we need the header to be AT the destinationIndex path.
        // taskIsChild checks (destinationIndex downTo 0), checking getTask at each position.
        // For the header to matter, it needs to be encountered before we find parent == source.id.

        // Better scenario: target at position 2, target's parent is some middle node,
        // and position 1 has a header, so walk stops before checking if anything is child.
        val target2 = makeTask(id = 5, parent = 99)
        `when`(dataSource.getItem(2)).thenReturn(target2)
        `when`(dataSource.isHeader(2)).thenReturn(false)
        `when`(dataSource.isHeader(1)).thenReturn(true)
        assertTrue(adapter.canMove(source, 0, target2, 2))
    }

    // --- canMove with single-level subtask (Google Tasks / Microsoft) ---

    @Test
    fun canMoveSingleLevelSourceWithNoChildrenAllowed() {
        val source = makeTask(id = 1, accountType = TYPE_GOOGLE_TASKS, children = 0)
        val target = makeTask(id = 2, accountType = TYPE_GOOGLE_TASKS, children = 0, parent = 0)
        `when`(dataSource.getTaskCount()).thenReturn(5)
        assertTrue(adapter.canMove(source, 0, target, 2))
    }

    @Test
    fun canMoveSingleLevelSourceWithChildrenToStart() {
        val source = makeTask(id = 1, accountType = TYPE_GOOGLE_TASKS, children = 2)
        val target = makeTask(id = 2, accountType = TYPE_GOOGLE_TASKS, children = 0, parent = 0)
        `when`(dataSource.getTaskCount()).thenReturn(5)
        assertTrue(adapter.canMove(source, 2, target, 0))
    }

    @Test
    fun canMoveSingleLevelSourceWithChildrenToEnd() {
        val source = makeTask(id = 1, accountType = TYPE_GOOGLE_TASKS, children = 2)
        val target = makeTask(id = 2, accountType = TYPE_GOOGLE_TASKS, children = 0, parent = 0)
        `when`(dataSource.getTaskCount()).thenReturn(5)
        assertTrue(adapter.canMove(source, 2, target, 4))
    }

    @Test
    fun canMoveDownwardWithChildrenTargetHasChildren() {
        val source = makeTask(id = 1, accountType = TYPE_GOOGLE_TASKS, children = 1)
        val target = makeTask(id = 2, accountType = TYPE_GOOGLE_TASKS, children = 1, parent = 0)
        `when`(dataSource.getTaskCount()).thenReturn(10)
        assertFalse(adapter.canMove(source, 1, target, 3))
    }

    @Test
    fun canMoveDownwardWithChildrenTargetHasParentNextHasNoParent() {
        val source = makeTask(id = 1, accountType = TYPE_GOOGLE_TASKS, children = 1)
        val target = makeTask(id = 3, accountType = TYPE_GOOGLE_TASKS, children = 0, parent = 2)
        val next = makeTask(id = 4, accountType = TYPE_GOOGLE_TASKS, children = 0, parent = 0)
        `when`(dataSource.getTaskCount()).thenReturn(10)
        `when`(dataSource.getItem(4)).thenReturn(next)
        assertTrue(adapter.canMove(source, 1, target, 3))
    }

    @Test
    fun canMoveDownwardWithChildrenTargetHasParentNextAlsoHasParent() {
        val source = makeTask(id = 1, accountType = TYPE_GOOGLE_TASKS, children = 1)
        val target = makeTask(id = 3, accountType = TYPE_GOOGLE_TASKS, children = 0, parent = 2)
        val next = makeTask(id = 4, accountType = TYPE_GOOGLE_TASKS, children = 0, parent = 2)
        `when`(dataSource.getTaskCount()).thenReturn(10)
        `when`(dataSource.getItem(4)).thenReturn(next)
        assertFalse(adapter.canMove(source, 1, target, 3))
    }

    @Test
    fun canMoveDownwardWithChildrenTargetNoChildrenNoParent() {
        val source = makeTask(id = 1, accountType = TYPE_GOOGLE_TASKS, children = 1)
        val target = makeTask(id = 2, accountType = TYPE_GOOGLE_TASKS, children = 0, parent = 0)
        `when`(dataSource.getTaskCount()).thenReturn(10)
        assertTrue(adapter.canMove(source, 1, target, 3))
    }

    @Test
    fun canMoveUpwardWithChildrenTargetHasChildren() {
        val source = makeTask(id = 1, accountType = TYPE_GOOGLE_TASKS, children = 1)
        val target = makeTask(id = 2, accountType = TYPE_GOOGLE_TASKS, children = 2, parent = 0)
        `when`(dataSource.getTaskCount()).thenReturn(10)
        assertTrue(adapter.canMove(source, 5, target, 2))
    }

    @Test
    fun canMoveUpwardWithChildrenTargetIsChildOfSourceAndSecondarySortZero() {
        val source = makeTask(id = 1, accountType = TYPE_GOOGLE_TASKS, children = 1)
        val target = makeTask(id = 3, accountType = TYPE_GOOGLE_TASKS, children = 0, parent = 1, secondarySort = 0)
        `when`(dataSource.getTaskCount()).thenReturn(10)
        assertTrue(adapter.canMove(source, 5, target, 2))
    }

    @Test
    fun canMoveUpwardWithChildrenTargetIsChildOfOtherParent() {
        val source = makeTask(id = 1, accountType = TYPE_GOOGLE_TASKS, children = 1)
        val target = makeTask(id = 3, accountType = TYPE_GOOGLE_TASKS, children = 0, parent = 99, secondarySort = 0)
        `when`(dataSource.getTaskCount()).thenReturn(10)
        assertFalse(adapter.canMove(source, 5, target, 2))
    }

    @Test
    fun canMoveUpwardWithChildrenTargetIsChildOfSourceNonZeroSecondarySort() {
        val source = makeTask(id = 1, accountType = TYPE_GOOGLE_TASKS, children = 1)
        val target = makeTask(id = 3, accountType = TYPE_GOOGLE_TASKS, children = 0, parent = 1, secondarySort = 5)
        `when`(dataSource.getTaskCount()).thenReturn(10)
        assertFalse(adapter.canMove(source, 5, target, 2))
    }

    @Test
    fun canMoveUpwardWithChildrenTargetNoChildrenNoParent() {
        val source = makeTask(id = 1, accountType = TYPE_GOOGLE_TASKS, children = 1)
        val target = makeTask(id = 2, accountType = TYPE_GOOGLE_TASKS, children = 0, parent = 0)
        `when`(dataSource.getTaskCount()).thenReturn(10)
        assertTrue(adapter.canMove(source, 5, target, 2))
    }

    // --- maxIndent ---

    @Test
    fun maxIndentSingleLevelSubtaskWithChildren() {
        val previous = makeTask(id = 1, accountType = TYPE_GOOGLE_TASKS)
        val task = makeTask(id = 2, accountType = TYPE_GOOGLE_TASKS, children = 2)
        `when`(dataSource.getItem(0)).thenReturn(previous)
        assertEquals(0, adapter.maxIndent(0, task))
    }

    @Test
    fun maxIndentSingleLevelSubtaskWithNoChildren() {
        val previous = makeTask(id = 1, accountType = TYPE_GOOGLE_TASKS)
        val task = makeTask(id = 2, accountType = TYPE_GOOGLE_TASKS, children = 0)
        `when`(dataSource.getItem(0)).thenReturn(previous)
        assertEquals(1, adapter.maxIndent(0, task))
    }

    @Test
    fun maxIndentNonSingleLevelSubtask() {
        val previous = makeTask(id = 1, indent = 2, accountType = TYPE_LOCAL)
        val task = makeTask(id = 2, accountType = TYPE_LOCAL)
        `when`(dataSource.getItem(0)).thenReturn(previous)
        assertEquals(3, adapter.maxIndent(0, task))
    }

    // --- minIndent ---

    @Test
    fun minIndentWhenAllRemainingAreChildren() {
        val task = makeTask(id = 1)
        `when`(dataSource.getTaskCount()).thenReturn(3)
        `when`(dataSource.isHeader(1)).thenReturn(false)
        `when`(dataSource.isHeader(2)).thenReturn(false)
        `when`(dataSource.getItem(1)).thenReturn(makeTask(id = 2, parent = 1))
        `when`(dataSource.getItem(2)).thenReturn(makeTask(id = 3, parent = 1))
        assertEquals(0, adapter.minIndent(1, task))
    }

    @Test
    fun minIndentWhenNextIsNotChild() {
        val task = makeTask(id = 1)
        `when`(dataSource.getTaskCount()).thenReturn(3)
        `when`(dataSource.isHeader(1)).thenReturn(false)
        val next = makeTask(id = 2, parent = 0, indent = 2)
        `when`(dataSource.getItem(1)).thenReturn(next)
        assertEquals(2, adapter.minIndent(1, task))
    }

    @Test
    fun minIndentWhenNextIsHeader() {
        val task = makeTask(id = 1)
        `when`(dataSource.getTaskCount()).thenReturn(3)
        `when`(dataSource.isHeader(1)).thenReturn(true)
        assertEquals(0, adapter.minIndent(1, task))
    }

    @Test
    fun minIndentWithNoRemainingTasks() {
        val task = makeTask(id = 1)
        `when`(dataSource.getTaskCount()).thenReturn(1)
        assertEquals(0, adapter.minIndent(1, task))
    }

    @Test
    fun minIndentSingleLevelSubtaskTaskHasChildren() {
        val task = makeTask(id = 1, accountType = TYPE_GOOGLE_TASKS, children = 1)
        `when`(dataSource.getTaskCount()).thenReturn(3)
        `when`(dataSource.isHeader(1)).thenReturn(false)
        val next = makeTask(id = 2, accountType = TYPE_GOOGLE_TASKS, parent = 0, children = 0)
        `when`(dataSource.getItem(1)).thenReturn(next)
        assertEquals(0, adapter.minIndent(1, task))
    }

    @Test
    fun minIndentSingleLevelSubtaskNextHasParent() {
        val task = makeTask(id = 1, accountType = TYPE_GOOGLE_TASKS, children = 0)
        `when`(dataSource.getTaskCount()).thenReturn(3)
        `when`(dataSource.isHeader(1)).thenReturn(false)
        val next = makeTask(id = 2, accountType = TYPE_GOOGLE_TASKS, parent = 5, children = 0)
        `when`(dataSource.getItem(1)).thenReturn(next)
        assertEquals(1, adapter.minIndent(1, task))
    }

    @Test
    fun minIndentSingleLevelSubtaskNextNoParent() {
        val task = makeTask(id = 1, accountType = TYPE_GOOGLE_TASKS, children = 0)
        `when`(dataSource.getTaskCount()).thenReturn(3)
        `when`(dataSource.isHeader(1)).thenReturn(false)
        val next = makeTask(id = 2, accountType = TYPE_GOOGLE_TASKS, parent = 0, children = 0)
        `when`(dataSource.getItem(1)).thenReturn(next)
        assertEquals(0, adapter.minIndent(1, task))
    }

    // --- moved: same parent, indent == 0 -> changeSortGroup ---

    @Test
    fun movedChangeSortGroupByImportance() = runTest {
        val task = makeTask(id = 1, parent = 0, indent = 0)
        `when`(dataSource.getItem(0)).thenReturn(task)
        `when`(dataSource.getTaskCount()).thenReturn(5)
        `when`(dataSource.sortMode).thenReturn(SORT_IMPORTANCE)
        `when`(dataSource.nearestHeader(1)).thenReturn(2L)
        `when`(dataSource.subtaskSortMode).thenReturn(-1)

        adapter.moved(0, 2, 0)

        verify(taskSaver).save(task.task.copy(priority = 2))
    }

    @Test
    fun movedChangeSortGroupByImportanceSamePriority() = runTest {
        val taskEntity = Task(id = 1, priority = 2)
        val task = TaskContainer(task = taskEntity, indent = 0)
        `when`(dataSource.getItem(0)).thenReturn(task)
        `when`(dataSource.getTaskCount()).thenReturn(5)
        `when`(dataSource.sortMode).thenReturn(SORT_IMPORTANCE)
        `when`(dataSource.nearestHeader(1)).thenReturn(2L)
        `when`(dataSource.subtaskSortMode).thenReturn(-1)

        adapter.moved(0, 2, 0)

        verify(taskSaver, never()).save(any(), any())
    }

    @Test
    fun movedChangeSortGroupByList() = runTest {
        val task = makeTask(id = 1, parent = 0, indent = 0)
        `when`(dataSource.getItem(0)).thenReturn(task)
        `when`(dataSource.getTaskCount()).thenReturn(5)
        `when`(dataSource.sortMode).thenReturn(SORT_LIST)
        `when`(dataSource.nearestHeader(1)).thenReturn(42L)
        `when`(dataSource.subtaskSortMode).thenReturn(-1)

        adapter.moved(0, 2, 0)

        verify(taskMover).move(1L, 42L)
    }

    @Test
    fun movedChangeSortGroupByDueDateZero() = runTest {
        val taskEntity = Task(id = 1, dueDate = 0)
        val task = TaskContainer(task = taskEntity, indent = 0)
        `when`(dataSource.getItem(0)).thenReturn(task)
        `when`(dataSource.getTaskCount()).thenReturn(5)
        `when`(dataSource.sortMode).thenReturn(SORT_DUE)
        `when`(dataSource.nearestHeader(1)).thenReturn(0L)
        `when`(dataSource.subtaskSortMode).thenReturn(-1)

        adapter.moved(0, 2, 0)

        verify(taskSaver, never()).save(any(), any())
    }

    @Test
    fun movedChangeSortGroupByStartDateZero() = runTest {
        val taskEntity = Task(id = 1, hideUntil = 0)
        val task = TaskContainer(task = taskEntity, indent = 0)
        `when`(dataSource.getItem(0)).thenReturn(task)
        `when`(dataSource.getTaskCount()).thenReturn(5)
        `when`(dataSource.sortMode).thenReturn(SORT_START)
        `when`(dataSource.nearestHeader(1)).thenReturn(0L)
        `when`(dataSource.subtaskSortMode).thenReturn(-1)

        adapter.moved(0, 2, 0)

        verify(taskSaver, never()).save(any(), any())
    }

    @Test
    fun movedAtPositionZeroUsesNearestHeaderAtOne() = runTest {
        val task = makeTask(id = 1, parent = 0, indent = 0)
        `when`(dataSource.getItem(0)).thenReturn(task)
        `when`(dataSource.getTaskCount()).thenReturn(5)
        `when`(dataSource.sortMode).thenReturn(SORT_IMPORTANCE)
        `when`(dataSource.nearestHeader(1)).thenReturn(3L)
        `when`(dataSource.subtaskSortMode).thenReturn(-1)

        adapter.moved(0, 0, 0)

        verify(dataSource).nearestHeader(1)
    }

    // --- moved: new parent with google task ---

    @Test
    fun movedToNewGoogleTaskParent() = runTest {
        val caldavTask = CaldavTask(id = 1, task = 1, calendar = "list-1")
        val task = makeTask(id = 1, parent = 0, accountType = TYPE_GOOGLE_TASKS, caldavTask = caldavTask)
        val parent = makeTask(id = 2, parent = 0, accountType = TYPE_GOOGLE_TASKS,
            caldavTask = CaldavTask(id = 2, task = 2, calendar = "list-1"), indent = 0)
        `when`(dataSource.getItem(0)).thenReturn(task)
        `when`(dataSource.getItem(1)).thenReturn(parent)
        `when`(dataSource.getTaskCount()).thenReturn(5)
        `when`(dataSource.subtaskSortMode).thenReturn(-1)
        `when`(googleTaskDao.getBottom(any(), Mockito.anyLong())).thenReturn(5L)

        adapter.moved(0, 2, 1)

        verify(googleTaskDao).move(any(), any(), Mockito.anyLong(), Mockito.anyLong())
        verify(taskSaver).touch(listOf(1L))
    }

    // --- moved: new parent with caldav task ---

    @Test
    fun movedToNewCaldavParent() = runTest {
        val caldavTask = CaldavTask(id = 1, task = 1, calendar = "list-1", remoteId = "remote-1")
        val parentCaldavTask = CaldavTask(id = 2, task = 2, calendar = "list-1", remoteId = "remote-parent")
        val task = makeTask(id = 1, parent = 0, caldavTask = caldavTask)
        val parent = makeTask(id = 2, parent = 0, caldavTask = parentCaldavTask, indent = 0)
        `when`(dataSource.getItem(0)).thenReturn(task)
        `when`(dataSource.getItem(1)).thenReturn(parent)
        `when`(dataSource.getTaskCount()).thenReturn(5)
        `when`(dataSource.subtaskSortMode).thenReturn(-1)
        `when`(caldavDao.findLastTask(any(), Mockito.anyLong())).thenReturn(null)

        adapter.moved(0, 2, 1)

        verify(caldavDao).update(any<CaldavTask>())
        verify(taskDao).setParent(Mockito.anyLong(), any())
        verify(taskSaver).touch(listOf(1L))
        verify(refreshBroadcaster).broadcastRefresh()
    }

    // --- moved: move to top level ---

    @Test
    fun movedToTopLevelGoogleTask() = runTest {
        val caldavTask = CaldavTask(id = 1, task = 1, calendar = "list-1")
        val task = makeTask(id = 1, parent = 5, accountType = TYPE_GOOGLE_TASKS, caldavTask = caldavTask)
        `when`(dataSource.getItem(0)).thenReturn(task)
        `when`(dataSource.getTaskCount()).thenReturn(5)
        `when`(dataSource.sortMode).thenReturn(SORT_IMPORTANCE)
        `when`(dataSource.nearestHeader(1)).thenReturn(0L)
        `when`(dataSource.subtaskSortMode).thenReturn(-1)
        `when`(googleTaskDao.getBottom(any(), Mockito.anyLong())).thenReturn(10L)

        adapter.moved(0, 2, 0)

        verify(googleTaskDao).move(any(), any(), Mockito.anyLong(), Mockito.anyLong())
    }

    // --- moved: same parent, indent > 0, manual subtask sort -> caldav move ---

    @Test
    fun movedSameParentManualSortCaldavTask() = runTest {
        val caldavTask = CaldavTask(id = 1, task = 1, calendar = "list-1", remoteId = "remote-1")
        val task = makeTask(id = 1, parent = 5, caldavTask = caldavTask, indent = 1, secondarySort = 10)
        val parentTask = makeTask(id = 5, parent = 0, indent = 0,
            caldavTask = CaldavTask(id = 5, task = 5, calendar = "list-1", remoteId = "remote-parent"))
        val sibling = makeTask(id = 6, parent = 5, indent = 1,
            caldavTask = CaldavTask(id = 6, task = 6, calendar = "list-1", remoteId = "remote-sibling"),
            secondarySort = 20)
        `when`(dataSource.getItem(1)).thenReturn(task)
        `when`(dataSource.getItem(0)).thenReturn(parentTask)
        `when`(dataSource.getItem(2)).thenReturn(sibling)
        `when`(dataSource.getTaskCount()).thenReturn(5)
        `when`(dataSource.subtaskSortMode).thenReturn(SORT_MANUAL)
        `when`(caldavDao.getTask(Mockito.anyLong())).thenReturn(
            CaldavTask(id = 5, task = 5, calendar = "list-1", remoteId = "remote-parent")
        )

        adapter.moved(1, 2, 1)

        verify(taskSaver).touch(listOf(1L))
        verify(refreshBroadcaster).broadcastRefresh()
    }

    // --- moved: cross-list caldav task ---

    @Test
    fun movedCaldavTaskToDifferentList() = runTest {
        val caldavTask = CaldavTask(id = 1, task = 1, calendar = "list-1", remoteId = "remote-1")
        val parentCaldavTask = CaldavTask(id = 2, task = 2, calendar = "list-2", remoteId = "remote-parent")
        val task = makeTask(id = 1, parent = 0, caldavTask = caldavTask)
        val newParent = makeTask(id = 2, parent = 0, caldavTask = parentCaldavTask, indent = 0)
        `when`(dataSource.getItem(0)).thenReturn(task)
        `when`(dataSource.getItem(1)).thenReturn(newParent)
        `when`(dataSource.getTaskCount()).thenReturn(5)
        `when`(dataSource.subtaskSortMode).thenReturn(-1)

        adapter.moved(0, 2, 1)

        verify(caldavDao).markDeleted(any(), Mockito.anyLong())
    }

    // --- onCompletedTask, onTaskCreated, onTaskDeleted (base class no-ops) ---

    @Test
    fun onCompletedTaskIsNoOp() = runTest {
        adapter.onCompletedTask("uuid", true)
        verifyNoInteractions(taskDao, taskSaver, refreshBroadcaster)
    }

    @Test
    fun onTaskCreatedIsNoOp() = runTest {
        adapter.onTaskCreated("uuid")
        verifyNoInteractions(taskDao, taskSaver, refreshBroadcaster)
    }

    @Test
    fun onTaskDeletedIsNoOp() = runTest {
        adapter.onTaskDeleted(Task())
        verifyNoInteractions(taskDao, taskSaver, refreshBroadcaster)
    }

    // --- moved: newTasksOnTop flag ---

    @Test
    fun newTasksOnTopGoogleTaskParentChange() = runTest {
        val topAdapter = TaskAdapter(
            true, googleTaskDao, caldavDao, taskDao, taskSaver, refreshBroadcaster, taskMover
        )
        topAdapter.setDataSource(dataSource)

        val caldavTask = CaldavTask(id = 1, task = 1, calendar = "list-1")
        val task = makeTask(id = 1, parent = 0, accountType = TYPE_GOOGLE_TASKS, caldavTask = caldavTask)
        val parent = makeTask(id = 2, parent = 0, accountType = TYPE_GOOGLE_TASKS,
            caldavTask = CaldavTask(id = 2, task = 2, calendar = "list-1"), indent = 0)
        `when`(dataSource.getItem(0)).thenReturn(task)
        `when`(dataSource.getItem(1)).thenReturn(parent)
        `when`(dataSource.getTaskCount()).thenReturn(5)
        `when`(dataSource.subtaskSortMode).thenReturn(-1)

        topAdapter.moved(0, 2, 1)

        // newTasksOnTop = true -> position should be 0
        verify(googleTaskDao).move(any(), any(), Mockito.anyLong(), Mockito.eq(0L))
    }

    // --- moved: changeSortGroup with pos == 0 ---

    @Test
    fun movedToPositionZeroFromLarger() = runTest {
        val task = makeTask(id = 1, parent = 0, indent = 0)
        `when`(dataSource.getItem(2)).thenReturn(task)
        `when`(dataSource.getTaskCount()).thenReturn(5)
        `when`(dataSource.sortMode).thenReturn(SORT_IMPORTANCE)
        `when`(dataSource.nearestHeader(1)).thenReturn(1L)
        `when`(dataSource.subtaskSortMode).thenReturn(-1)

        adapter.moved(2, 0, 0)

        verify(taskSaver).save(task.task.copy(priority = 1))
    }

    // --- Helper ---

    private fun makeTask(
        id: Long = 0,
        uuid: String = "uuid-$id",
        parent: Long = 0,
        indent: Int = 0,
        children: Int = 0,
        caldavTask: CaldavTask? = null,
        accountType: Int = TYPE_LOCAL,
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
