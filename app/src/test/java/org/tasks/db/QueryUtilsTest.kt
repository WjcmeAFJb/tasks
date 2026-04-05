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
}