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

    // ===== DUE grouping =====

    @Test
    fun dueGroupingOverdueSection() {
        // A sortGroup in the distant past should be HEADER_OVERDUE
        val pastTimestamp = 946684800000L // 2000-01-01 00:00 UTC
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = pastTimestamp),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_DUE,
        )
        assertTrue(ds.groupsEnabled)
        val sectionValues = ds.getSectionValues()
        assertTrue(sectionValues.contains(SectionedDataSource.HEADER_OVERDUE))
    }

    @Test
    fun dueGroupingFutureSection() {
        // A sortGroup far in the future should be its startOfDay
        val farFuture = System.currentTimeMillis() + 30 * ONE_DAY
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = farFuture),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_DUE,
        )
        assertTrue(ds.groupsEnabled)
        val sectionValues = ds.getSectionValues()
        assertFalse(sectionValues.contains(SectionedDataSource.HEADER_OVERDUE))
    }

    @Test
    fun dueGroupingZeroSortGroupUsesZero() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 0L),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_DUE,
        )
        assertTrue(ds.groupsEnabled)
        val sectionValues = ds.getSectionValues()
        assertEquals(listOf(0L), sectionValues)
    }

    @Test
    fun dueGroupingTwoOverdueTasksSameSection() {
        val pastA = 946684800000L // 2000-01-01
        val pastB = 946771200000L // 2000-01-02
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = pastA),
            taskContainer(id = 2, sortGroup = pastB),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_DUE,
        )
        // Both should be in the OVERDUE section, so only 1 section
        val sectionValues = ds.getSectionValues()
        assertEquals(1, sectionValues.size)
        assertEquals(SectionedDataSource.HEADER_OVERDUE, sectionValues[0])
    }

    @Test
    fun dueGroupingOverdueAndFutureTwoSections() {
        val pastTimestamp = 946684800000L // 2000-01-01
        val farFuture = System.currentTimeMillis() + 30 * ONE_DAY
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = pastTimestamp),
            taskContainer(id = 2, sortGroup = farFuture),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_DUE,
        )
        val sectionValues = ds.getSectionValues()
        assertEquals(2, sectionValues.size)
    }

    // ===== START grouping =====

    @Test
    fun startGroupingCreatesSection() {
        val farFuture = System.currentTimeMillis() + 30 * ONE_DAY
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = farFuture),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_START,
        )
        assertTrue(ds.groupsEnabled)
    }

    @Test
    fun startGroupingSameDaySameSection() {
        val base = System.currentTimeMillis() + 30 * ONE_DAY
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = base),
            taskContainer(id = 2, sortGroup = base + 3600000), // 1 hour later, same day
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_START,
        )
        // Same day -> same section
        val sectionValues = ds.getSectionValues()
        assertEquals(1, sectionValues.size)
    }

    @Test
    fun startGroupingDifferentDaysDifferentSections() {
        val base = System.currentTimeMillis() + 30 * ONE_DAY
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = base),
            taskContainer(id = 2, sortGroup = base + 2 * ONE_DAY),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_START,
        )
        val sectionValues = ds.getSectionValues()
        assertEquals(2, sectionValues.size)
    }

    @Test
    fun startGroupingZeroToNonZeroCreatesNewSection() {
        val farFuture = System.currentTimeMillis() + 30 * ONE_DAY
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 0L),
            taskContainer(id = 2, sortGroup = farFuture),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_START,
        )
        val sectionValues = ds.getSectionValues()
        assertEquals(2, sectionValues.size)
    }

    // ===== Default (else) grouping =====

    @Test
    fun defaultGroupingCreatesSections() {
        // Using SORT_MODIFIED (value=4) which falls into the else branch
        val base = System.currentTimeMillis() + 30 * ONE_DAY
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = base),
            taskContainer(id = 2, sortGroup = base + 2 * ONE_DAY),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = 4, // SORT_MODIFIED
        )
        val sectionValues = ds.getSectionValues()
        assertEquals(2, sectionValues.size)
    }

    @Test
    fun defaultGroupingSameDaySameSection() {
        val base = System.currentTimeMillis() + 30 * ONE_DAY
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = base),
            taskContainer(id = 2, sortGroup = base + 3600000), // 1 hour later
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = 4, // SORT_MODIFIED
        )
        val sectionValues = ds.getSectionValues()
        assertEquals(1, sectionValues.size)
    }

    @Test
    fun defaultGroupingPreviousZeroNoNewSection() {
        val base = System.currentTimeMillis() + 30 * ONE_DAY
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 0L),
            taskContainer(id = 2, sortGroup = base),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = 4, // SORT_MODIFIED, else branch requires previous > 0
        )
        // First task gets a section at 0L, second doesn't get one because
        // previous (0L) is not > 0 so the else check fails
        val sectionValues = ds.getSectionValues()
        assertEquals(1, sectionValues.size)
    }

    // ===== Collapsed sections with multiple groups =====

    @Test
    fun collapsedSectionRemovesTasksFromMiddleGroup() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 0L),
            taskContainer(id = 2, sortGroup = 1L),
            taskContainer(id = 3, sortGroup = 1L),
            taskContainer(id = 4, sortGroup = 2L),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
            collapsed = setOf(1L),
        )
        // Group 0: header + 1 task = 2
        // Group 1: collapsed header (tasks removed) = 1
        // Group 2: header + 1 task = 2
        // Total = 5
        assertEquals(5, ds.size)
        assertEquals(2, ds.taskCount) // only tasks from groups 0 and 2
    }

    @Test
    fun collapsedLastSection() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 0L),
            taskContainer(id = 2, sortGroup = 1L),
            taskContainer(id = 3, sortGroup = 1L),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
            collapsed = setOf(1L),
        )
        // Group 0: header + 1 task = 2
        // Group 1: collapsed header = 1
        // Total = 3
        assertEquals(3, ds.size)
        assertEquals(1, ds.taskCount)
    }

    @Test
    fun allSectionsCollapsed() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 0L),
            taskContainer(id = 2, sortGroup = 1L),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
            collapsed = setOf(0L, 1L),
        )
        // 2 headers only, no tasks
        assertEquals(2, ds.size)
        assertEquals(0, ds.taskCount)
    }

    // ===== completedAtBottom edge cases =====

    @Test
    fun completedAtBottomConsecutiveCompletedNoDuplicateHeader() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 0L, parentComplete = false),
            taskContainer(id = 2, sortGroup = 0L, parentComplete = true),
            taskContainer(id = 3, sortGroup = 0L, parentComplete = true),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
            completedAtBottom = true,
        )
        val sectionValues = ds.getSectionValues()
        // Should have group 0 header and completed header, but not duplicate completed
        val completedCount = sectionValues.count { it == SectionedDataSource.HEADER_COMPLETED }
        assertEquals(1, completedCount)
    }

    @Test
    fun completedAtBottomFirstTaskComplete() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 0L, parentComplete = true),
            taskContainer(id = 2, sortGroup = 0L, parentComplete = true),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
            completedAtBottom = true,
        )
        val sectionValues = ds.getSectionValues()
        assertEquals(1, sectionValues.size)
        assertEquals(SectionedDataSource.HEADER_COMPLETED, sectionValues[0])
    }

    // ===== moveSection =====

    @Test
    fun moveSectionUpdatesPosition() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 0L),
            taskContainer(id = 2, sortGroup = 0L),
            taskContainer(id = 3, sortGroup = 1L),
            taskContainer(id = 4, sortGroup = 1L),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
        )
        // Initially: pos 0 = header(0L), pos 1 = task1, pos 2 = task2,
        //            pos 3 = header(1L), pos 4 = task3, pos 5 = task4
        assertTrue(ds.isHeader(0))
        assertTrue(ds.isHeader(3))

        // Move the header at position 3 by offset 1
        ds.moveSection(3, 1)

        // Verify the section was moved
        assertTrue(ds.isHeader(4))
    }

    // ===== sectionedPositionToPosition with headers =====

    @Test
    fun getItemAtHeaderPositionReturnsFirstTaskInSection() {
        val tasks = listOf(
            taskContainer(id = 10, sortGroup = 5L),
            taskContainer(id = 20, sortGroup = 5L),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
        )
        // Position 0 is header -> sectionedPositionToPosition returns firstPosition
        val item = ds.getItem(0)
        assertEquals(10L, item.id) // Returns first task in section
    }

    // ===== Multiple headers offset calculation =====

    @Test
    fun getItemCorrectWithMultipleHeaders() {
        val tasks = listOf(
            taskContainer(id = 10, sortGroup = 0L),
            taskContainer(id = 20, sortGroup = 1L),
            taskContainer(id = 30, sortGroup = 2L),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
        )
        // Layout: H(0L), T10, H(1L), T20, H(2L), T30
        assertEquals(6, ds.size)
        assertEquals(10L, ds.getItem(1).id)
        assertEquals(20L, ds.getItem(3).id)
        assertEquals(30L, ds.getItem(5).id)
    }

    // ===== get() returns correct UiItem types =====

    @Test
    fun getReturnsHeaderForHeaderPosition() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 5L),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
        )
        val item = ds[0]
        assertTrue(item is UiItem.Header)
        assertEquals(5L, (item as UiItem.Header).value)
    }

    @Test
    fun getReturnsTaskForTaskPosition() {
        val tasks = listOf(
            taskContainer(id = 42, sortGroup = 5L),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
        )
        val item = ds[1]
        assertTrue(item is UiItem.Task)
        assertEquals(42L, (item as UiItem.Task).task.id)
    }

    // ===== getHeaderValue =====

    @Test
    fun getHeaderValueReturnsCorrectValue() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 42L),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
        )
        assertEquals(42L, ds.getHeaderValue(0))
    }

    // ===== subList edge cases =====

    @Test
    fun subListFromZeroToSize() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 0L),
            taskContainer(id = 2, sortGroup = 0L),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
        )
        val sub = ds.subList(0, ds.size)
        assertEquals(ds.size, sub.size)
    }

    @Test
    fun subListEmpty() {
        val ds = SectionedDataSource(
            listOf(taskContainer(id = 1)),
            disableHeaders = true,
        )
        val sub = ds.subList(0, 0)
        assertTrue(sub.isEmpty())
    }

    // ===== getNearestHeader for task after multiple headers =====

    @Test
    fun getNearestHeaderFindsCorrectSectionForLaterTask() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 0L),
            taskContainer(id = 2, sortGroup = 1L),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
        )
        // Layout: H(0), T1, H(1), T2 -> positions 0,1,2,3
        // getNearestHeader(3) should find header at position 2 with value 1L
        assertEquals(1L, ds.getNearestHeader(3))
    }

    // ===== add / removeAt with headers =====

    @Test
    fun addTaskWithHeaders() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 0L),
            taskContainer(id = 2, sortGroup = 0L),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
        )
        // Layout: H(0), T1, T2 -> sectionedPositions 0,1,2
        // Adding at sectionedPosition 2 -> actual position 1 in tasks list
        ds.add(2, taskContainer(id = 99, sortGroup = 0L))
        assertEquals(3, ds.taskCount)
    }

    @Test
    fun removeAtWithHeaders() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 0L),
            taskContainer(id = 2, sortGroup = 0L),
            taskContainer(id = 3, sortGroup = 0L),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
        )
        // Layout: H(0), T1, T2, T3 -> sectionedPositions 0,1,2,3
        val removed = ds.removeAt(2)
        assertEquals(2L, removed.id)
        assertEquals(2, ds.taskCount)
    }

    // ===== sortGroup 0 used as header directly for SORT_DUE =====

    @Test
    fun dueGroupingSortGroupZeroBeforeNonZero() {
        val farFuture = System.currentTimeMillis() + 30 * ONE_DAY
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 0L),
            taskContainer(id = 2, sortGroup = farFuture),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_DUE,
        )
        val sectionValues = ds.getSectionValues()
        // 0L goes through the sortGroup == 0L special case
        assertEquals(2, sectionValues.size)
        assertEquals(0L, sectionValues[0])
    }

    // ===== Collapsed completed section =====

    @Test
    fun collapsedCompletedSection() {
        val tasks = listOf(
            taskContainer(id = 1, sortGroup = 0L, parentComplete = false),
            taskContainer(id = 2, sortGroup = 0L, parentComplete = true),
            taskContainer(id = 3, sortGroup = 0L, parentComplete = true),
        )
        val ds = SectionedDataSource(
            tasks,
            groupMode = SortHelper.SORT_IMPORTANCE,
            completedAtBottom = true,
            collapsed = setOf(SectionedDataSource.HEADER_COMPLETED),
        )
        // Group 0: header + 1 task = 2
        // Completed: collapsed header = 1
        assertEquals(3, ds.size)
        assertEquals(1, ds.taskCount)
    }
}
