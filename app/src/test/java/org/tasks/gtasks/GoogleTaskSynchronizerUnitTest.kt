package org.tasks.gtasks

import com.google.api.services.tasks.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.createDueDate

class GoogleTaskSynchronizerUnitTest {

    // ===== truncate =====

    @Test
    fun truncateNullReturnsNull() {
        assertNull(GoogleTaskSynchronizer.truncate(null, 10))
    }

    @Test
    fun truncateShortStringReturnsOriginal() {
        assertEquals("hello", GoogleTaskSynchronizer.truncate("hello", 10))
    }

    @Test
    fun truncateExactLengthReturnsOriginal() {
        assertEquals("12345", GoogleTaskSynchronizer.truncate("12345", 5))
    }

    @Test
    fun truncateLongStringTruncates() {
        assertEquals("123", GoogleTaskSynchronizer.truncate("12345", 3))
    }

    @Test
    fun truncateEmptyStringReturnsEmpty() {
        assertEquals("", GoogleTaskSynchronizer.truncate("", 10))
    }

    @Test
    fun truncateToZeroReturnsEmpty() {
        assertEquals("", GoogleTaskSynchronizer.truncate("hello", 0))
    }

    @Test
    fun truncateToOneCharacter() {
        assertEquals("h", GoogleTaskSynchronizer.truncate("hello", 1))
    }

    // ===== getTruncatedValue =====

    @Test
    fun getTruncatedValueBothNull() {
        assertNull(GoogleTaskSynchronizer.getTruncatedValue(null, null, 100))
    }

    @Test
    fun getTruncatedValueNewValueNull() {
        assertNull(GoogleTaskSynchronizer.getTruncatedValue("current", null, 100))
    }

    @Test
    fun getTruncatedValueNewValueEmpty() {
        assertEquals("", GoogleTaskSynchronizer.getTruncatedValue("current", "", 100))
    }

    @Test
    fun getTruncatedValueCurrentNullReturnsNewValue() {
        assertEquals("new value", GoogleTaskSynchronizer.getTruncatedValue(null, "new value", 100))
    }

    @Test
    fun getTruncatedValueCurrentEmptyReturnsNewValue() {
        assertEquals("new value", GoogleTaskSynchronizer.getTruncatedValue("", "new value", 100))
    }

    @Test
    fun getTruncatedValueShortNewValueReturnsNewValue() {
        assertEquals("short", GoogleTaskSynchronizer.getTruncatedValue("current longer value", "short", 100))
    }

    @Test
    fun getTruncatedValueNewValueExactMaxLengthAndCurrentStartsWithNew() {
        val newValue = "12345"
        val currentValue = "12345_extended"
        assertEquals(currentValue, GoogleTaskSynchronizer.getTruncatedValue(currentValue, newValue, 5))
    }

    @Test
    fun getTruncatedValueNewValueExactMaxLengthAndCurrentDoesNotStartWithNew() {
        val newValue = "12345"
        val currentValue = "abcde_extended"
        assertEquals(newValue, GoogleTaskSynchronizer.getTruncatedValue(currentValue, newValue, 5))
    }

    @Test
    fun getTruncatedValueNewLongerThanMaxCurrentStartsWith() {
        val newValue = "123456"
        val currentValue = "123456_extended"
        assertEquals(currentValue, GoogleTaskSynchronizer.getTruncatedValue(currentValue, newValue, 5))
    }

    @Test
    fun getTruncatedValueNewLongerThanMaxCurrentDoesNotStartWith() {
        val newValue = "abcdef"
        val currentValue = "xyz_extended"
        assertEquals(newValue, GoogleTaskSynchronizer.getTruncatedValue(currentValue, newValue, 5))
    }

    // ===== mergeDates =====

    @Test
    fun mergeDatesRemoteDueDateZero() {
        val task = org.tasks.data.entity.Task(dueDate = 1000L)
        GoogleTaskSynchronizer.mergeDates(0L, task)
        assertEquals(0L, task.dueDate)
    }

    @Test
    fun mergeDatesNoExistingDueTime() {
        val remoteDueDate = createDueDate(
            org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY,
            1718409600000L
        )
        val task = org.tasks.data.entity.Task(dueDate = 0L)
        GoogleTaskSynchronizer.mergeDates(remoteDueDate, task)
        assertEquals(remoteDueDate, task.dueDate)
    }

    @Test
    fun mergeDatesWithExistingDueTimePreservesTime() {
        val existingDueDate = createDueDate(
            org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY_TIME,
            1718409600000L
        )
        val task = org.tasks.data.entity.Task(dueDate = existingDueDate)
        assertTrue("Task should have due time", task.hasDueTime())

        val remoteDueDate = createDueDate(
            org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY,
            1718496000000L
        )
        GoogleTaskSynchronizer.mergeDates(remoteDueDate, task)
        assertTrue("After merge, task should still have due time", task.hasDueTime())
    }

    @Test
    fun mergeDatesRemoteDueDateNegative() {
        val task = org.tasks.data.entity.Task(dueDate = 1000L)
        GoogleTaskSynchronizer.mergeDates(-1L, task)
        assertEquals(-1L, task.dueDate)
    }

    // ===== PARENTS_FIRST comparator =====

    @Test
    fun parentsFirstBothNoParent() {
        val task1 = Task()
        val task2 = Task()
        val tasks = mutableListOf(task1, task2)
        java.util.Collections.sort(tasks, getParentsFirstComparator())
        assertEquals(task1, tasks[0])
        assertEquals(task2, tasks[1])
    }

    @Test
    fun parentsFirstOnlyFirstHasParent() {
        val task1 = Task().apply { parent = "parent-1" }
        val task2 = Task()
        val tasks = mutableListOf(task1, task2)
        java.util.Collections.sort(tasks, getParentsFirstComparator())
        assertNull(tasks[0].parent)
        assertEquals("parent-1", tasks[1].parent)
    }

    @Test
    fun parentsFirstOnlySecondHasParent() {
        val task1 = Task()
        val task2 = Task().apply { parent = "parent-2" }
        val tasks = mutableListOf(task1, task2)
        java.util.Collections.sort(tasks, getParentsFirstComparator())
        assertNull(tasks[0].parent)
        assertEquals("parent-2", tasks[1].parent)
    }

    @Test
    fun parentsFirstBothHaveParent() {
        val task1 = Task().apply { parent = "parent-1" }
        val task2 = Task().apply { parent = "parent-2" }
        val tasks = mutableListOf(task1, task2)
        java.util.Collections.sort(tasks, getParentsFirstComparator())
        assertEquals("parent-1", tasks[0].parent)
        assertEquals("parent-2", tasks[1].parent)
    }

    @Test
    fun parentsFirstEmptyParentTreatedAsNoParent() {
        val task1 = Task().apply { parent = "" }
        val task2 = Task().apply { parent = "parent-1" }
        val tasks = mutableListOf(task1, task2)
        java.util.Collections.sort(tasks, getParentsFirstComparator())
        assertEquals("", tasks[0].parent)
        assertEquals("parent-1", tasks[1].parent)
    }

    @Test
    fun parentsFirstSortMultipleTasks() {
        val parentTask = Task()
        val child1 = Task().apply { parent = "p1" }
        val child2 = Task().apply { parent = "p1" }
        val root2 = Task()
        val tasks = mutableListOf(child1, parentTask, child2, root2)
        java.util.Collections.sort(tasks, getParentsFirstComparator())
        assertNull(tasks[0].parent)
        assertNull(tasks[1].parent)
        assertEquals("p1", tasks[2].parent)
        assertEquals("p1", tasks[3].parent)
    }

    // ===== truncate for title and description max lengths =====

    @Test
    fun truncateTitleToMaxLength() {
        val longTitle = "a".repeat(1500)
        val result = GoogleTaskSynchronizer.truncate(longTitle, 1024)
        assertEquals(1024, result!!.length)
    }

    @Test
    fun truncateDescriptionToMaxLength() {
        val longDescription = "b".repeat(10000)
        val result = GoogleTaskSynchronizer.truncate(longDescription, 8192)
        assertEquals(8192, result!!.length)
    }

    @Test
    fun truncateTitleUnderMaxLength() {
        val shortTitle = "a".repeat(1024)
        val result = GoogleTaskSynchronizer.truncate(shortTitle, 1024)
        assertEquals(1024, result!!.length)
        assertEquals(shortTitle, result)
    }

    private fun getParentsFirstComparator(): Comparator<Task> {
        return Comparator { o1: Task, o2: Task ->
            if (org.tasks.Strings.isNullOrEmpty(o1.parent)) {
                if (org.tasks.Strings.isNullOrEmpty(o2.parent)) 0 else -1
            } else {
                if (org.tasks.Strings.isNullOrEmpty(o2.parent)) 1 else 0
            }
        }
    }
}
