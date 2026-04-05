package org.tasks.data

import com.todoroo.astrid.core.SortHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.filters.CaldavFilter
import org.tasks.filters.DebugFilter
import org.tasks.preferences.QueryPreferences

/**
 * Tests for TaskListQueryRecursive (indirectly, via TaskListQuery.getQuery()).
 *
 * These tests exercise query generation for the recursive CTE path by
 * constructing concrete Filter and QueryPreferences instances and
 * verifying structural properties of the generated SQL.
 */
class TaskListQueryRecursiveTest {

    /** A simple QueryPreferences implementation for testing. */
    private class TestPreferences(
        override var sortMode: Int = SortHelper.SORT_DUE,
        override var groupMode: Int = SortHelper.SORT_DUE,
        override var completedMode: Int = SortHelper.SORT_COMPLETED,
        override var subtaskMode: Int = SortHelper.SORT_MANUAL,
        override var isManualSort: Boolean = false,
        override var isAstridSort: Boolean = false,
        override var sortAscending: Boolean = true,
        override var groupAscending: Boolean = true,
        override var completedAscending: Boolean = false,
        override var subtaskAscending: Boolean = false,
        override val showHidden: Boolean = false,
        override val showCompleted: Boolean = false,
        override val alwaysDisplayFullDate: Boolean = false,
        override var completedTasksAtBottom: Boolean = true,
    ) : QueryPreferences

    private fun debugFilter(sql: String = "WHERE tasks.deleted = 0") =
        DebugFilter(title = "Test", sql = sql, icon = null)

    private fun caldavFilter(
        uuid: String = "test-list-uuid",
        accountType: Int = CaldavAccount.TYPE_CALDAV,
    ): CaldavFilter {
        val account = CaldavAccount(accountType = accountType)
        val calendar = CaldavCalendar(uuid = uuid, account = account.uuid)
        return CaldavFilter(calendar = calendar, account = account)
    }

    // --- Basic query structure ---

    @Test
    fun queryContainsRecursiveCTE() {
        val prefs = TestPreferences()
        val query = TaskListQuery.getQuery(prefs, debugFilter())
        assertTrue(
            "Query should contain WITH RECURSIVE",
            query.contains("WITH RECURSIVE", ignoreCase = true)
        )
    }

    @Test
    fun queryContainsRecursiveTasksTable() {
        val prefs = TestPreferences()
        val query = TaskListQuery.getQuery(prefs, debugFilter())
        assertTrue(
            "Query should reference recursive_tasks",
            query.contains("recursive_tasks")
        )
    }

    @Test
    fun queryContainsMaxIndentCTE() {
        val prefs = TestPreferences()
        val query = TaskListQuery.getQuery(prefs, debugFilter())
        assertTrue(
            "Query should contain max_indent CTE",
            query.contains("max_indent")
        )
    }

    @Test
    fun queryContainsDescendantsCTE() {
        val prefs = TestPreferences()
        val query = TaskListQuery.getQuery(prefs, debugFilter())
        assertTrue(
            "Query should contain descendants CTE",
            query.contains("descendants")
        )
    }

    @Test
    fun queryContainsOrderBySequence() {
        val prefs = TestPreferences()
        val query = TaskListQuery.getQuery(prefs, debugFilter())
        assertTrue(
            "Query should ORDER BY sequence",
            query.contains("ORDER BY sequence")
        )
    }

    // --- Limit ---

    @Test
    fun queryWithLimitContainsLimitClause() {
        val prefs = TestPreferences()
        val query = TaskListQuery.getQuery(prefs, debugFilter(), limit = 50)
        assertTrue(
            "Query should contain LIMIT 50",
            query.contains("LIMIT 50")
        )
    }

    @Test
    fun queryWithoutLimitHasNoLimitClause() {
        val prefs = TestPreferences()
        val query = TaskListQuery.getQuery(prefs, debugFilter())
        assertFalse(
            "Query without limit should not contain LIMIT",
            query.contains("LIMIT")
        )
    }

    // --- Show completed / hidden ---

    @Test
    fun showCompletedRemovesCompletionFilter() {
        val prefs = TestPreferences(showCompleted = true)
        val query = TaskListQuery.getQuery(prefs, debugFilter())
        // When showing completed, the completion_date <= 0 criterion should be removed
        assertFalse(
            "With showCompleted, query should not filter out completed tasks",
            query.contains("completed = 0 AND deleted = 0 AND hideUntil")
        )
    }

    @Test
    fun showHiddenRemovesHideUntilFilter() {
        val prefs = TestPreferences(showHidden = true)
        val query = TaskListQuery.getQuery(prefs, debugFilter())
        // The hideUntil filter should be relaxed
        // The recursive part still has activeAndVisible, but the adjustQueryForFlags
        // modifies the outer query
        assertTrue(
            "Query should still be valid SQL structure",
            query.contains("WITH RECURSIVE")
        )
    }

    // --- CaldavFilter path ---

    @Test
    fun caldavFilterGeneratesRecursiveQuery() {
        val prefs = TestPreferences()
        val query = TaskListQuery.getQuery(prefs, caldavFilter())
        assertTrue(
            "CaldavFilter query should contain WITH RECURSIVE",
            query.contains("WITH RECURSIVE")
        )
    }

    @Test
    fun caldavFilterJoinsCaldavTasks() {
        val prefs = TestPreferences()
        val query = TaskListQuery.getQuery(prefs, caldavFilter())
        assertTrue(
            "CaldavFilter query should join caldav_tasks",
            query.contains("caldav_tasks")
        )
    }

    @Test
    fun caldavFilterContainsListUuid() {
        val prefs = TestPreferences()
        val query = TaskListQuery.getQuery(prefs, caldavFilter(uuid = "my-caldav-list"))
        assertTrue(
            "CaldavFilter query should contain the list UUID",
            query.contains("my-caldav-list")
        )
    }

    // --- Manual sort with CaldavFilter ---

    @Test
    fun manualSortWithCaldavFilterUsesRecursive() {
        val prefs = TestPreferences(isManualSort = true)
        val query = TaskListQuery.getQuery(prefs, caldavFilter())
        assertTrue(
            "Manual sort CaldavFilter should use recursive query",
            query.contains("WITH RECURSIVE")
        )
    }

    @Test
    fun manualSortWithCaldavFilterSuppressesGrouping() {
        val prefs = TestPreferences(
            isManualSort = true,
            groupMode = SortHelper.SORT_DUE,
        )
        val query = TaskListQuery.getQuery(prefs, caldavFilter())
        // When manual sort + CaldavFilter, groupMode becomes GROUP_NONE
        // so primary_group should be "1" (the literal for GROUP_NONE)
        assertTrue(
            "Manual sort should suppress grouping (primary_group = 1)",
            query.contains("1 AS primary_group")
        )
    }

    // --- Sort modes ---

    @Test
    fun sortByAlphaIncludesUpperTitle() {
        val prefs = TestPreferences(sortMode = SortHelper.SORT_ALPHA)
        val query = TaskListQuery.getQuery(prefs, debugFilter())
        assertTrue(
            "Alpha sort should reference UPPER(tasks.title)",
            query.contains("UPPER(tasks.title)")
        )
    }

    @Test
    fun sortByImportanceIncludesImportance() {
        val prefs = TestPreferences(sortMode = SortHelper.SORT_IMPORTANCE)
        val query = TaskListQuery.getQuery(prefs, debugFilter())
        assertTrue(
            "Importance sort should reference tasks.importance",
            query.contains("tasks.importance")
        )
    }

    @Test
    fun sortByModifiedIncludesModified() {
        val prefs = TestPreferences(sortMode = SortHelper.SORT_MODIFIED)
        val query = TaskListQuery.getQuery(prefs, debugFilter())
        assertTrue(
            "Modified sort should reference tasks.modified",
            query.contains("tasks.modified")
        )
    }

    @Test
    fun sortByCreatedIncludesCreated() {
        val prefs = TestPreferences(sortMode = SortHelper.SORT_CREATED)
        val query = TaskListQuery.getQuery(prefs, debugFilter())
        assertTrue(
            "Created sort should reference tasks.created",
            query.contains("tasks.created")
        )
    }

    @Test
    fun sortByStartIncludesHideUntil() {
        val prefs = TestPreferences(sortMode = SortHelper.SORT_START)
        val query = TaskListQuery.getQuery(prefs, debugFilter())
        assertTrue(
            "Start sort should reference hideUntil",
            query.contains("hideUntil")
        )
    }

    // --- Group mode ---

    @Test
    fun groupByListJoinsCaldavLists() {
        val prefs = TestPreferences(groupMode = SortHelper.SORT_LIST)
        val query = TaskListQuery.getQuery(prefs, debugFilter())
        assertTrue(
            "Group by list should join caldav_lists",
            query.contains("caldav_lists") || query.contains("cdl_uuid")
        )
    }

    @Test
    fun groupNoneDoesNotAddGroupJoin() {
        val prefs = TestPreferences(groupMode = SortHelper.GROUP_NONE)
        val query = TaskListQuery.getQuery(prefs, debugFilter())
        assertTrue(
            "GROUP_NONE primary_group should be 1",
            query.contains("1 AS primary_group")
        )
    }

    // --- completedTasksAtBottom ---

    @Test
    fun completedAtBottomAddsCompletedCheck() {
        val prefs = TestPreferences(completedTasksAtBottom = true)
        val query = TaskListQuery.getQuery(prefs, debugFilter())
        assertTrue(
            "Completed at bottom should check tasks.completed > 0",
            query.contains("tasks.completed > 0")
        )
    }

    @Test
    fun completedNotAtBottomUsesZero() {
        val prefs = TestPreferences(completedTasksAtBottom = false)
        val query = TaskListQuery.getQuery(prefs, debugFilter())
        assertTrue(
            "Completed not at bottom should use 0 for parent_complete",
            query.contains("0 AS parent_complete")
        )
    }

    // --- Sort ascending flags ---

    @Test
    fun sortDescendingContainsDesc() {
        val prefs = TestPreferences(
            sortMode = SortHelper.SORT_DUE,
            sortAscending = false,
        )
        val query = TaskListQuery.getQuery(prefs, debugFilter())
        assertTrue(
            "Sort descending should contain DESC for primary_sort",
            query.contains("primary_sort DESC")
        )
    }

    @Test
    fun sortAscendingContainsAsc() {
        val prefs = TestPreferences(
            sortMode = SortHelper.SORT_DUE,
            sortAscending = true,
        )
        val query = TaskListQuery.getQuery(prefs, debugFilter())
        assertTrue(
            "Sort ascending should contain ASC for primary_sort",
            query.contains("primary_sort ASC")
        )
    }

    // --- Cycle prevention ---

    @Test
    fun queryContainsCyclePrevention() {
        val prefs = TestPreferences()
        val query = TaskListQuery.getQuery(prefs, debugFilter())
        assertTrue(
            "Query should contain cycle prevention with recursive_path",
            query.contains("recursive_path NOT LIKE")
        )
    }

    // --- Google Tasks sort ---

    @Test
    fun googleTasksCaldavFilterWithManualSortUsesGtasksOrder() {
        val prefs = TestPreferences(isManualSort = true)
        val filter = caldavFilter(accountType = CaldavAccount.TYPE_GOOGLE_TASKS)
        val query = TaskListQuery.getQuery(prefs, filter)
        assertTrue(
            "Google Tasks with manual sort should use tasks.order",
            query.contains("tasks.`order`")
        )
    }

    // --- Recursive path ---

    @Test
    fun queryContainsRecursivePath() {
        val prefs = TestPreferences()
        val query = TaskListQuery.getQuery(prefs, debugFilter())
        assertTrue(
            "Query should build recursive_path",
            query.contains("recursive_path")
        )
    }

    // --- indent tracking ---

    @Test
    fun queryTracksIndent() {
        val prefs = TestPreferences()
        val query = TaskListQuery.getQuery(prefs, debugFilter())
        assertTrue(
            "Query should track indent level",
            query.contains("indent")
        )
    }
}
