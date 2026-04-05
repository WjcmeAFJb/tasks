package org.tasks.tasklist

import com.todoroo.astrid.core.SortHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.TaskContainer
import org.tasks.data.entity.Task
import org.tasks.time.ONE_DAY

class SectionedDataSourceTest {

    private fun taskContainer(
        id: Long = 1L,
        sortGroup: Long? = null,
        parentComplete: Boolean = false,
    ) = TaskContainer(
        task = Task(id = id),
        sortGroup = sortGroup,
        parentComplete = parentComplete,
    )

    // ===== Empty data source =====

    @Test
    fun emptyDataSourceHasSizeZero() {
        val ds = SectionedDataSource()
        assertEquals(0, ds.size)
    }

    @Test
    fun emptyDataSourceIsEmpty() {
        val ds = SectionedDataSource()
        assertTrue(ds.isEmpty())
    }

    @Test
    fun emptyDataSourceHasZeroTaskCount() {
        val ds = SectionedDataSource()
        assertEquals(0, ds.taskCount)
    }

    @Test
    fun emptyDataSourceGroupsNotEnabled() {
        val ds = SectionedDataSource()
        assertFalse(ds.groupsEnabled)
    }

    @Test
    fun emptyDataSourceSectionValuesEmpty() {
        val ds = SectionedDataSource()
        assertEquals(emptyList<Long>(), ds.getSectionValues())
    }

    // ===== No headers mode =====

    @Test
    fun noHeadersSizeEqualsTaskCount() {
        val tasks = listOf(taskContainer(id = 1), taskContainer(id = 2), taskContainer(id = 3))
        val ds = SectionedDataSource(tasks, disableHeaders = true)
        assertEquals(3, ds.size)
        assertEquals(3, ds.taskCount)
    }

    @Test
    fun noHeadersGroupsNotEnabled() {
        val tasks = listOf(taskContainer(id = 1))
        val ds = SectionedDataSource(tasks, disableHeaders = true)
        assertFalse(ds.groupsEnabled)
    }

    @Test
    fun noHeadersNotEmpty() {
        val tasks = listOf(taskContainer(id = 1))
        val ds = SectionedDataSource(tasks, disableHeaders = true)
        assertFalse(ds.isEmpty())
    }

    @Test
    fun noHeadersIsNotHeader() {
        val tasks = listOf(taskContainer(id = 1))
        val ds = SectionedDataSource(tasks, disableHeaders = true)
        assertFalse(ds.isHeader(0))
    }

    @Test
    fun noHeadersGetItemReturnsTask() {
        val task = taskContainer(id = 42)
        val ds = SectionedDataSource(listOf(task), disableHeaders = true)
        assertEquals(42L, ds.getItem(0).id)
    }

    @Test
    fun noHeadersGetByIndexReturnsUiItemTask() {
        val task = taskContainer(id = 7)
        val ds = SectionedDataSource(listOf(task), disableHeaders = true)
        val item = ds[0]
        assertTrue(item is UiItem.Task)
        assertEquals(7L, (item as UiItem.Task).task.id)
    }

    // ===== GROUP_NONE =====

    @Test
    fun groupNoneNoHeaders() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 100L),
            taskContainer(id = 2, sortGroup = 200L),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.GROUP_NONE,
        )
        assertFalse(ds.groupsEnabled)
        assertEquals(2, ds.size)
    }

    // ===== Importance grouping =====

    @Test
    fun importanceGroupingSameGroup() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 0L),
            taskContainer(id = 2, sortGroup = 0L),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
        )
        assertTrue(ds.groupsEnabled)
        // 1 header + 2 tasks = 3
        assertEquals(3, ds.size)
    }

    @Test
    fun importanceGroupingDifferentGroups() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 0L),
            taskContainer(id = 2, sortGroup = 1L),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
        )
        // 2 headers + 2 tasks = 4
        assertEquals(4, ds.size)
    }

    @Test
    fun importanceGroupingHeaderValues() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 0L),
            taskContainer(id = 2, sortGroup = 3L),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
        )
        val sectionValues = ds.getSectionValues()
        assertEquals(listOf(0L, 3L), sectionValues)
    }

    @Test
    fun importanceGroupingFirstPositionIsHeader() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 0L),
            taskContainer(id = 2, sortGroup = 0L),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
        )
        assertTrue(ds.isHeader(0))
        assertFalse(ds.isHeader(1))
        assertFalse(ds.isHeader(2))
    }

    // ===== List grouping =====

    @Test
    fun listGroupingSameGroup() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 5L),
            taskContainer(id = 2, sortGroup = 5L),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_LIST,
        )
        // 1 header + 2 tasks
        assertEquals(3, ds.size)
    }

    @Test
    fun listGroupingDifferentGroups() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 5L),
            taskContainer(id = 2, sortGroup = 10L),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_LIST,
        )
        // 2 headers + 2 tasks
        assertEquals(4, ds.size)
    }

    // ===== Completed at bottom =====

    @Test
    fun completedAtBottomCreatesCompletedSection() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 0L, parentComplete = false),
            taskContainer(id = 2, sortGroup = 0L, parentComplete = true),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
            completedAtBottom = true,
        )
        assertTrue(ds.groupsEnabled)
        val sectionValues = ds.getSectionValues()
        assertTrue(sectionValues.contains(SectionedDataSource.HEADER_COMPLETED))
    }

    @Test
    fun completedAtBottomDisabled() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 0L, parentComplete = false),
            taskContainer(id = 2, sortGroup = 0L, parentComplete = true),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
            completedAtBottom = false,
        )
        val sectionValues = ds.getSectionValues()
        assertFalse(sectionValues.contains(SectionedDataSource.HEADER_COMPLETED))
    }

    // ===== Collapsed sections =====

    @Test
    fun collapsedSectionRemovesTasks() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 0L),
            taskContainer(id = 2, sortGroup = 0L),
            taskContainer(id = 3, sortGroup = 1L),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
            collapsed = setOf(0L),
        )
        // Group 0 is collapsed: header remains but tasks removed.
        // Group 1: header + 1 task
        // Total: 1 (collapsed header) + 1 (header) + 1 (task) = 3
        assertEquals(3, ds.size)
    }

    // ===== Null sort groups =====

    @Test
    fun nullSortGroupSkipsHeader() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = null),
            taskContainer(id = 2, sortGroup = null),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
        )
        // Null sort groups are skipped, no headers added
        assertFalse(ds.groupsEnabled)
        assertEquals(2, ds.size)
    }

    // ===== Iterator =====

    @Test
    fun iteratorReturnsAllItems() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 0L),
            taskContainer(id = 2, sortGroup = 0L),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
        )
        val items = ds.iterator().asSequence().toList()
        assertEquals(ds.size, items.size)
    }

    @Test
    fun iteratorFirstItemIsHeader() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 0L),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
        )
        val first = ds.iterator().next()
        assertTrue(first is UiItem.Header)
    }

    // ===== subList =====

    @Test
    fun subListReturnsCorrectRange() {
        val tasks = listOf(
            taskContainer(id = 1),
            taskContainer(id = 2),
            taskContainer(id = 3),
        )
        val ds = SectionedDataSource(tasks, disableHeaders = true)
        val sub = ds.subList(1, 3)
        assertEquals(2, sub.size)
    }

    // ===== getNearestHeader =====

    @Test
    fun getNearestHeaderReturnsMinusOneForNegativePosition() {
        val ds = SectionedDataSource(
            listOf(taskContainer(id = 1, sortGroup = 5L)),
            groupMode = SortHelper.SORT_IMPORTANCE,
        )
        assertEquals(-1L, ds.getNearestHeader(-1))
    }

    @Test
    fun getNearestHeaderReturnsHeaderValueAtHeaderPosition() {
        val ds = SectionedDataSource(
            listOf(taskContainer(id = 1, sortGroup = 5L)),
            groupMode = SortHelper.SORT_IMPORTANCE,
        )
        // Position 0 should be the header
        assertTrue(ds.isHeader(0))
        assertEquals(5L, ds.getNearestHeader(0))
    }

    @Test
    fun getNearestHeaderSearchesBackward() {
        val ds = SectionedDataSource(
            listOf(
                taskContainer(id = 1, sortGroup = 5L),
                taskContainer(id = 2, sortGroup = 5L),
            ),
            groupMode = SortHelper.SORT_IMPORTANCE,
        )
        // Position 1 is task, should find header at position 0
        assertEquals(5L, ds.getNearestHeader(1))
    }

    // ===== getItem =====

    @Test
    fun getItemReturnsCorrectTask() {
        val tasks = listOf(
            taskContainer(id = 10, sortGroup = 0L),
            taskContainer(id = 20, sortGroup = 0L),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
        )
        // Position 0 is header, 1 is first task, 2 is second task
        assertEquals(10L, ds.getItem(1).id)
        assertEquals(20L, ds.getItem(2).id)
    }

    // ===== Constants =====

    @Test
    fun headerOverdueConstant() {
        assertEquals(-1L, SectionedDataSource.HEADER_OVERDUE)
    }

    @Test
    fun headerCompletedConstant() {
        assertEquals(-2L, SectionedDataSource.HEADER_COMPLETED)
    }

    // ===== add and removeAt =====

    @Test
    fun addTaskInsertsAtCorrectPosition() {
        val tasks = listOf(taskContainer(id = 1), taskContainer(id = 2))
        val ds = SectionedDataSource(tasks, disableHeaders = true)
        ds.add(1, taskContainer(id = 99))
        assertEquals(3, ds.taskCount)
        assertEquals(99L, ds.getItem(1).id)
    }

    @Test
    fun removeAtRemovesTask() {
        val tasks = listOf(taskContainer(id = 1), taskContainer(id = 2), taskContainer(id = 3))
        val ds = SectionedDataSource(tasks, disableHeaders = true)
        val removed = ds.removeAt(1)
        assertEquals(2L, removed.id)
        assertEquals(2, ds.taskCount)
    }
}
