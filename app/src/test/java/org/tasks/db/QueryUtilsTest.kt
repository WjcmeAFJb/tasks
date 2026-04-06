package org.tasks.db

import org.tasks.data.sql.Functions
import org.tasks.data.entity.Task
import org.junit.Assert.assertEquals
import org.junit.Test

class QueryUtilsTest {
    @Test
    fun replaceHiddenLT() {
        assertEquals("(1)", QueryUtils.showHidden(Task.HIDE_UNTIL.lt(Functions.now()).toString()))
    }

    @Test
    fun replaceHiddenLTE() {
        assertEquals("(1)", QueryUtils.showHidden(Task.HIDE_UNTIL.lte(Functions.now()).toString()))
    }

    @Test
    fun replaceUncompletedEQ() {
        assertEquals("(1)", QueryUtils.showCompleted(Task.COMPLETION_DATE.eq(0).toString()))
    }

    @Test
    fun replaceUncompletedLTE() {
        assertEquals("(1)", QueryUtils.showCompleted(Task.COMPLETION_DATE.lte(0).toString()))
    }

    @Test
    fun showHiddenAndCompletedReplacesBoth() {
        val query = "${Task.HIDE_UNTIL.lte(Functions.now())} AND ${Task.COMPLETION_DATE.eq(0)}"
        val result = QueryUtils.showHiddenAndCompleted(query)
        assertEquals("(1) AND (1)", result)
    }

    @Test
    fun showHiddenDoesNotAffectCompleted() {
        val query = Task.COMPLETION_DATE.eq(0).toString()
        assertEquals(query, QueryUtils.showHidden(query))
    }

    @Test
    fun showCompletedDoesNotAffectHidden() {
        val query = Task.HIDE_UNTIL.lte(Functions.now()).toString()
        assertEquals(query, QueryUtils.showCompleted(query))
    }

    @Test
    fun removeOrderAsc() {
        val query = "SELECT * FROM tasks WHERE 1 order by tasks.title asc"
        assertEquals("SELECT * FROM tasks WHERE 1 ", QueryUtils.removeOrder(query))
    }

    @Test
    fun removeOrderDesc() {
        val query = "SELECT * FROM tasks WHERE 1 order by tasks.dueDate desc"
        assertEquals("SELECT * FROM tasks WHERE 1 ", QueryUtils.removeOrder(query))
    }

    @Test
    fun removeOrderCaseInsensitive() {
        val query = "SELECT * FROM tasks ORDER BY tasks.title ASC"
        assertEquals("SELECT * FROM tasks ", QueryUtils.removeOrder(query))
    }

    @Test
    fun removeOrderMixedCase() {
        val query = "SELECT * FROM tasks Order By tasks.title Asc"
        assertEquals("SELECT * FROM tasks ", QueryUtils.removeOrder(query))
    }

    @Test
    fun removeOrderPreservesQueryWithoutOrder() {
        val query = "SELECT * FROM tasks WHERE 1"
        assertEquals("SELECT * FROM tasks WHERE 1", QueryUtils.removeOrder(query))
    }

    @Test
    fun showHiddenPreservesUnrelatedQuery() {
        val query = "SELECT * FROM tasks WHERE title = 'test'"
        assertEquals(query, QueryUtils.showHidden(query))
    }

    @Test
    fun showCompletedPreservesUnrelatedQuery() {
        val query = "SELECT * FROM tasks WHERE title = 'test'"
        assertEquals(query, QueryUtils.showCompleted(query))
    }

    @Test
    fun showHiddenAndCompletedPreservesUnrelatedQuery() {
        val query = "SELECT * FROM tasks WHERE title = 'test'"
        assertEquals(query, QueryUtils.showHiddenAndCompleted(query))
    }

    // --- Additional tests for better coverage ---

    @Test
    fun showHiddenReplacesMultipleOccurrences() {
        val hidden = Task.HIDE_UNTIL.lte(Functions.now()).toString()
        val query = "$hidden AND $hidden"
        val result = QueryUtils.showHidden(query)
        assertEquals("(1) AND (1)", result)
    }

    @Test
    fun showCompletedReplacesMultipleOccurrences() {
        val completed = Task.COMPLETION_DATE.eq(0).toString()
        val query = "$completed OR $completed"
        val result = QueryUtils.showCompleted(query)
        assertEquals("(1) OR (1)", result)
    }

    @Test
    fun removeOrderWithMultipleColumns() {
        val query = "SELECT * FROM tasks WHERE 1 order by tasks.title, tasks.dueDate asc"
        val result = QueryUtils.removeOrder(query)
        assertEquals("SELECT * FROM tasks WHERE 1 ", result)
    }

    @Test
    fun emptyQueryShowHidden() {
        assertEquals("", QueryUtils.showHidden(""))
    }

    @Test
    fun emptyQueryShowCompleted() {
        assertEquals("", QueryUtils.showCompleted(""))
    }

    @Test
    fun emptyQueryRemoveOrder() {
        assertEquals("", QueryUtils.removeOrder(""))
    }

    @Test
    fun emptyQueryShowHiddenAndCompleted() {
        assertEquals("", QueryUtils.showHiddenAndCompleted(""))
    }

    @Test
    fun removeOrderUpperCase() {
        val query = "SELECT * FROM tasks ORDER BY tasks.title DESC"
        assertEquals("SELECT * FROM tasks ", QueryUtils.removeOrder(query))
    }

    @Test
    fun showHiddenAndCompletedChainedOrder() {
        val hidden = Task.HIDE_UNTIL.lte(Functions.now()).toString()
        val completed = Task.COMPLETION_DATE.eq(0).toString()
        // Verify order doesn't matter
        val query = "$completed AND $hidden"
        val result1 = QueryUtils.showHiddenAndCompleted(query)
        assertEquals("(1) AND (1)", result1)
    }

    @Test
    fun removeOrderPreservesWhereClause() {
        val query = "SELECT * FROM tasks WHERE title LIKE '%test%' order by tasks.title asc"
        val result = QueryUtils.removeOrder(query)
        assertEquals("SELECT * FROM tasks WHERE title LIKE '%test%' ", result)
    }

    @Test
    fun showHiddenWithSurroundingText() {
        val hidden = Task.HIDE_UNTIL.lte(Functions.now()).toString()
        val query = "SELECT * FROM tasks WHERE $hidden AND title = 'foo'"
        val result = QueryUtils.showHidden(query)
        assertEquals("SELECT * FROM tasks WHERE (1) AND title = 'foo'", result)
    }

    @Test
    fun showCompletedWithSurroundingText() {
        val completed = Task.COMPLETION_DATE.eq(0).toString()
        val query = "SELECT * FROM tasks WHERE $completed AND title = 'foo'"
        val result = QueryUtils.showCompleted(query)
        assertEquals("SELECT * FROM tasks WHERE (1) AND title = 'foo'", result)
    }

    @Test
    fun removeOrderDoesNotAffectOrderWordInContent() {
        // "order" appearing as part of data shouldn't be matched by the regex
        // which requires "order by ... (asc|desc)"
        val query = "SELECT * FROM tasks WHERE title = 'my order'"
        assertEquals(query, QueryUtils.removeOrder(query))
    }
}
