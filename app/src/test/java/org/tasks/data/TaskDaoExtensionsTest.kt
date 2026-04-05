package org.tasks.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Task

class TaskDaoExtensionsTest {

    private lateinit var taskDao: TaskDao

    @Before
    fun setUp() {
        taskDao = mock(TaskDao::class.java)
    }

    // --- countCompletedSql ---

    @Test
    fun countCompletedSqlReplacesCompletedClause() = runTest {
        val sql = "WHERE tasks.completed<=0 AND tasks.deleted<=0"
        `when`(taskDao.count(any(String::class.java) ?: "")).thenReturn(5)

        val result = taskDao.countCompletedSql(sql)

        assertEquals(5, result)
    }

    @Test
    fun countCompletedSqlReturnsZeroWhenNoCompletedClause() = runTest {
        val sql = "WHERE tasks.deleted<=0"

        val result = taskDao.countCompletedSql(sql)

        assertEquals(0, result)
        verify(taskDao, never()).count(any(String::class.java) ?: "")
    }

    @Test
    fun countCompletedSqlReplacesCompletedWithGreaterThan() = runTest {
        val sql = "WHERE tasks.completed<=0 ORDER BY dueDate"
        `when`(taskDao.count(any(String::class.java) ?: "")).thenReturn(3)

        val result = taskDao.countCompletedSql(sql)

        assertEquals(3, result)
    }

    @Test
    fun countCompletedSqlHandlesMultipleCompletedClauses() = runTest {
        val sql = "WHERE tasks.completed<=0 AND (tasks.completed<=0 OR other)"
        `when`(taskDao.count(any(String::class.java) ?: "")).thenReturn(7)

        val result = taskDao.countCompletedSql(sql)

        assertEquals(7, result)
    }

    @Test
    fun countCompletedSqlWithEmptyString() = runTest {
        val sql = ""

        val result = taskDao.countCompletedSql(sql)

        assertEquals(0, result)
    }

    @Test
    fun countCompletedSqlWithUnrelatedSql() = runTest {
        val sql = "WHERE tasks.deleted<=0 AND tasks.hideUntil<=12345"

        val result = taskDao.countCompletedSql(sql)

        assertEquals(0, result)
    }

    // --- fetchFiltered ---

    @Test
    fun fetchFilteredReturnsTasksFromContainers() = runTest {
        val task1 = Task(id = 1, title = "Task 1")
        val task2 = Task(id = 2, title = "Task 2")
        val containers = listOf(
            TaskContainer(task = task1),
            TaskContainer(task = task2),
        )
        `when`(taskDao.fetchTasks(any(String::class.java) ?: "")).thenReturn(containers)

        val result = taskDao.fetchFiltered("WHERE tasks.completed<=0")

        assertEquals(2, result.size)
        assertEquals("Task 1", result[0].title)
        assertEquals("Task 2", result[1].title)
    }

    @Test
    fun fetchFilteredReturnsEmptyListWhenNoTasks() = runTest {
        `when`(taskDao.fetchTasks(any(String::class.java) ?: "")).thenReturn(emptyList())

        val result = taskDao.fetchFiltered("WHERE 1=0")

        assertEquals(emptyList<Task>(), result)
    }
}
