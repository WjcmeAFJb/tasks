package com.todoroo.astrid.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.preferences.QueryPreferences

class SortHelperTest {

    private fun prefs(
        showCompleted: Boolean = false,
        showHidden: Boolean = false,
        sortAscending: Boolean = true,
    ) = object : QueryPreferences {
        override var sortMode: Int = 0
        override var groupMode: Int = 0
        override var completedMode: Int = 0
        override var subtaskMode: Int = 0
        override var isManualSort: Boolean = false
        override var isAstridSort: Boolean = false
        override var sortAscending: Boolean = sortAscending
        override var groupAscending: Boolean = true
        override var completedAscending: Boolean = true
        override var subtaskAscending: Boolean = true
        override val showHidden: Boolean = showHidden
        override val showCompleted: Boolean = showCompleted
        override val alwaysDisplayFullDate: Boolean = false
        override var completedTasksAtBottom: Boolean = false
    }

    // --- orderSelectForSortTypeRecursive ---

    @Test fun sortAlphaReturnsTitle() =
        assertEquals("UPPER(tasks.title)", SortHelper.orderSelectForSortTypeRecursive(SortHelper.SORT_ALPHA, false))

    @Test fun sortImportanceReturnsImportance() =
        assertEquals("tasks.importance", SortHelper.orderSelectForSortTypeRecursive(SortHelper.SORT_IMPORTANCE, false))

    @Test fun sortModifiedReturnsModified() =
        assertEquals("tasks.modified", SortHelper.orderSelectForSortTypeRecursive(SortHelper.SORT_MODIFIED, false))

    @Test fun sortCreatedReturnsCreated() =
        assertEquals("tasks.created", SortHelper.orderSelectForSortTypeRecursive(SortHelper.SORT_CREATED, false))

    @Test fun sortGtasksReturnsOrder() =
        assertEquals("tasks.`order`", SortHelper.orderSelectForSortTypeRecursive(SortHelper.SORT_GTASKS, false))

    @Test fun sortCompletedReturnsCompleted() =
        assertEquals("tasks.completed", SortHelper.orderSelectForSortTypeRecursive(SortHelper.SORT_COMPLETED, false))

    @Test fun sortGroupNoneReturns1() =
        assertEquals("1", SortHelper.orderSelectForSortTypeRecursive(SortHelper.GROUP_NONE, false))

    @Test fun sortDueGrouping() {
        val result = SortHelper.orderSelectForSortTypeRecursive(SortHelper.SORT_DUE, true)
        assertTrue(result.contains("datetime"))
        assertTrue(result.contains("start of day"))
    }

    @Test fun sortDueNonGrouping() {
        val result = SortHelper.orderSelectForSortTypeRecursive(SortHelper.SORT_DUE, false)
        assertTrue(result.contains("dueDate"))
        assertTrue(result.contains("importance"))
    }

    @Test fun sortStartGrouping() {
        val result = SortHelper.orderSelectForSortTypeRecursive(SortHelper.SORT_START, true)
        assertTrue(result.contains("datetime"))
    }

    @Test fun sortModifiedGrouping() {
        val result = SortHelper.orderSelectForSortTypeRecursive(SortHelper.SORT_MODIFIED, true)
        assertTrue(result.contains("datetime"))
    }

    @Test fun sortListReturnsOrderCase() {
        val result = SortHelper.orderSelectForSortTypeRecursive(SortHelper.SORT_LIST, false)
        assertTrue(result.contains("cdl_order"))
        assertTrue(result.contains("cdl_name"))
    }

    // --- getSortGroup ---

    @Test fun sortGroupDue() = assertEquals("tasks.dueDate", SortHelper.getSortGroup(SortHelper.SORT_DUE))
    @Test fun sortGroupStart() = assertEquals("tasks.hideUntil", SortHelper.getSortGroup(SortHelper.SORT_START))
    @Test fun sortGroupImportance() = assertEquals("tasks.importance", SortHelper.getSortGroup(SortHelper.SORT_IMPORTANCE))
    @Test fun sortGroupModified() = assertEquals("tasks.modified", SortHelper.getSortGroup(SortHelper.SORT_MODIFIED))
    @Test fun sortGroupCreated() = assertEquals("tasks.created", SortHelper.getSortGroup(SortHelper.SORT_CREATED))
    @Test fun sortGroupList() = assertEquals("cdl_id", SortHelper.getSortGroup(SortHelper.SORT_LIST))
    @Test fun sortGroupAlphaReturnsNull() = assertNull(SortHelper.getSortGroup(SortHelper.SORT_ALPHA))
    @Test fun sortGroupAutoReturnsNull() = assertNull(SortHelper.getSortGroup(SortHelper.SORT_AUTO))
    @Test fun sortGroupManualReturnsNull() = assertNull(SortHelper.getSortGroup(SortHelper.SORT_MANUAL))

    // --- adjustQueryForFlagsAndSort ---

    @Test fun addsOrderByWhenMissing() {
        val result = SortHelper.adjustQueryForFlagsAndSort(prefs(), "SELECT *", SortHelper.SORT_ALPHA)
        assertTrue(result.contains("ORDER BY"))
    }

    @Test fun doesNotAddOrderByWhenPresent() {
        val sql = "SELECT * ORDER BY id"
        val result = SortHelper.adjustQueryForFlagsAndSort(prefs(), sql, SortHelper.SORT_ALPHA)
        // Should not duplicate ORDER BY
        assertEquals(1, result.split("ORDER BY").size - 1)
    }

    @Test fun nullSqlTreatedAsEmpty() {
        val result = SortHelper.adjustQueryForFlagsAndSort(prefs(), null, SortHelper.SORT_ALPHA)
        assertNotNull(result)
        assertTrue(result.contains("ORDER BY"))
    }

    @Test fun showCompletedPassesThroughSql() {
        val result = SortHelper.adjustQueryForFlags(prefs(showCompleted = true), "WHERE x=1")
        assertNotNull(result)
    }

    @Test fun showHiddenPassesThroughSql() {
        val result = SortHelper.adjustQueryForFlags(prefs(showHidden = true), "WHERE x=1")
        assertNotNull(result)
    }

    // --- orderForGroupTypeRecursive ---

    @Test fun groupAscendingReturnsAsc() {
        val order = SortHelper.orderForGroupTypeRecursive(SortHelper.SORT_DUE, true)
        assertTrue(order.toString().contains("ASC"))
    }

    @Test fun groupDescendingReturnsDesc() {
        val order = SortHelper.orderForGroupTypeRecursive(SortHelper.SORT_DUE, false)
        assertTrue(order.toString().contains("DESC"))
    }

    // --- orderForSortTypeRecursive ---

    @Test fun sortRecursiveAscending() {
        val order = SortHelper.orderForSortTypeRecursive(SortHelper.SORT_DUE, true, SortHelper.SORT_ALPHA, true)
        val str = order.toString()
        assertTrue(str.contains("primary_sort"))
        assertTrue(str.contains("secondary_sort"))
    }

    @Test fun sortRecursiveAlphaNoSortTitle() {
        val order = SortHelper.orderForSortTypeRecursive(SortHelper.SORT_ALPHA, true, SortHelper.SORT_DUE, true)
        val str = order.toString()
        // SORT_ALPHA should NOT add sort_title (it already sorts by title)
        assertFalse(str.contains("sort_title"))
    }

    @Test fun sortRecursiveNonAlphaAddsSortTitle() {
        val order = SortHelper.orderForSortTypeRecursive(SortHelper.SORT_DUE, true, SortHelper.SORT_ALPHA, true)
        val str = order.toString()
        assertTrue(str.contains("sort_title"))
    }

    @Test fun sortRecursiveGtasksAlwaysAsc() {
        val order = SortHelper.orderForSortTypeRecursive(SortHelper.SORT_GTASKS, false, SortHelper.SORT_DUE, true)
        val str = order.toString()
        // SORT_GTASKS primary always ASC regardless of primaryAscending
        assertTrue(str.startsWith("primary_sort ASC"))
    }

    @Test fun sortRecursiveCaldavAlwaysAsc() {
        val order = SortHelper.orderForSortTypeRecursive(SortHelper.SORT_CALDAV, false, SortHelper.SORT_DUE, true)
        val str = order.toString()
        assertTrue(str.startsWith("primary_sort ASC"))
    }

    // --- constants ---

    @Test fun sortConstants() {
        assertEquals(-1, SortHelper.GROUP_NONE)
        assertEquals(0, SortHelper.SORT_AUTO)
        assertEquals(1, SortHelper.SORT_ALPHA)
        assertEquals(2, SortHelper.SORT_DUE)
        assertEquals(3, SortHelper.SORT_IMPORTANCE)
        assertEquals(6, SortHelper.SORT_GTASKS)
        assertEquals(7, SortHelper.SORT_CALDAV)
        assertEquals(11, SortHelper.SORT_MANUAL)
    }

    private fun assertFalse(value: Boolean) = assertEquals(false, value)
}
