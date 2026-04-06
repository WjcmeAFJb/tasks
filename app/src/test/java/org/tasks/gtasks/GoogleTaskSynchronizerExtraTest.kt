package org.tasks.gtasks

import com.google.api.services.tasks.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.Strings
import org.tasks.data.createDueDate
import java.util.Collections

class GoogleTaskSynchronizerExtraTest {

    // ===== truncate edge cases =====

    @Test
    fun truncateUnicodeString() {
        val unicode = "\u00E9\u00E8\u00EA\u00EB\u00E0" // 5 accented chars
        assertEquals("\u00E9\u00E8\u00EA", GoogleTaskSynchronizer.truncate(unicode, 3))
    }

    @Test
    fun truncateStringWithSpaces() {
        assertEquals("hello ", GoogleTaskSynchronizer.truncate("hello world", 6))
    }

    @Test
    fun truncateMaxIntReturnsOriginal() {
        val str = "test"
        assertEquals(str, GoogleTaskSynchronizer.truncate(str, Int.MAX_VALUE))
    }

    @Test
    fun truncateAtBoundary1024() {
        val str = "a".repeat(1024)
        val result = GoogleTaskSynchronizer.truncate(str, 1024)
        assertEquals(1024, result!!.length)
        assertEquals(str, result)
    }

    @Test
    fun truncateAtBoundary1025() {
        val str = "a".repeat(1025)
        val result = GoogleTaskSynchronizer.truncate(str, 1024)
        assertEquals(1024, result!!.length)
    }

    @Test
    fun truncateAtBoundary8192() {
        val str = "b".repeat(8192)
        val result = GoogleTaskSynchronizer.truncate(str, 8192)
        assertEquals(8192, result!!.length)
        assertEquals(str, result)
    }

    @Test
    fun truncateAtBoundary8193() {
        val str = "b".repeat(8193)
        val result = GoogleTaskSynchronizer.truncate(str, 8192)
        assertEquals(8192, result!!.length)
    }

    // ===== getTruncatedValue edge cases =====

    @Test
    fun getTruncatedValueBothEmpty() {
        assertEquals("", GoogleTaskSynchronizer.getTruncatedValue("", "", 100))
    }

    @Test
    fun getTruncatedValueNewValueExactlyMaxLength() {
        val newValue = "abcde" // length 5
        val currentValue = "abcde_more"
        // newValue.length == maxLength (5), and currentValue starts with newValue
        val result = GoogleTaskSynchronizer.getTruncatedValue(currentValue, newValue, 5)
        assertEquals(currentValue, result)
    }

    @Test
    fun getTruncatedValueNewValueOverMaxCurrentDoesNotStartWith() {
        val newValue = "abcdef" // length 6
        val currentValue = "xyz"
        val result = GoogleTaskSynchronizer.getTruncatedValue(currentValue, newValue, 5)
        assertEquals(newValue, result)
    }

    @Test
    fun getTruncatedValueNewValueShorterThanMax() {
        val newValue = "abc"
        val currentValue = "def"
        val result = GoogleTaskSynchronizer.getTruncatedValue(currentValue, newValue, 10)
        assertEquals(newValue, result)
    }

    @Test
    fun getTruncatedValueBothNullReturnsNull() {
        assertNull(GoogleTaskSynchronizer.getTruncatedValue(null, null, 100))
    }

    @Test
    fun getTruncatedValueNewNullReturnsNull() {
        assertNull(GoogleTaskSynchronizer.getTruncatedValue("existing", null, 100))
    }

    @Test
    fun getTruncatedValuePreservesCurrentWhenNewIsTruncated() {
        // When newValue >= maxLength AND currentValue starts with newValue, return currentValue
        val currentValue = "This is a very long description that was truncated on the server side"
        val newValue = "This is a very long"
        val result = GoogleTaskSynchronizer.getTruncatedValue(currentValue, newValue, 15)
        assertEquals(currentValue, result)
    }

    @Test
    fun getTruncatedValueReturnsNewWhenCurrentDoesNotStartWithNew() {
        val currentValue = "Different text entirely"
        val newValue = "This is a very long"
        val result = GoogleTaskSynchronizer.getTruncatedValue(currentValue, newValue, 15)
        assertEquals(newValue, result)
    }

    // ===== mergeDates =====

    @Test
    fun mergeDatesZeroRemoteDoesNotPreserveExistingTime() {
        val task = org.tasks.data.entity.Task(dueDate = 1000L)
        GoogleTaskSynchronizer.mergeDates(0L, task)
        assertEquals(0L, task.dueDate)
    }

    @Test
    fun mergeDatesNonZeroRemoteWithNoExistingDue() {
        val remoteDueDate = createDueDate(
            org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY,
            1718409600000L
        )
        val task = org.tasks.data.entity.Task(dueDate = 0L)
        GoogleTaskSynchronizer.mergeDates(remoteDueDate, task)
        assertEquals(remoteDueDate, task.dueDate)
    }

    @Test
    fun mergeDatesPreservesTimeFromLocalTask() {
        // Create a task with a specific due date + time
        val specificTime = createDueDate(
            org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY_TIME,
            1718452200000L // 2024-06-15 14:30 UTC
        )
        val task = org.tasks.data.entity.Task(dueDate = specificTime)
        assertTrue(task.hasDueTime())

        // Now merge with a new remote date (different day)
        val remoteDueDate = createDueDate(
            org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY,
            1718841600000L // 2024-06-20 00:00 UTC
        )
        GoogleTaskSynchronizer.mergeDates(remoteDueDate, task)

        // Task should still have a due time
        assertTrue("Should preserve time after merge", task.hasDueTime())
    }

    // ===== PARENTS_FIRST comparator behavior =====

    @Test
    fun parentsFirstComparatorStableForTwoParentless() {
        val t1 = Task()
        val t2 = Task()
        val comparator = parentsFirstComparator()
        assertEquals(0, comparator.compare(t1, t2))
    }

    @Test
    fun parentsFirstComparatorStableForTwoWithParents() {
        val t1 = Task().apply { parent = "p1" }
        val t2 = Task().apply { parent = "p2" }
        val comparator = parentsFirstComparator()
        assertEquals(0, comparator.compare(t1, t2))
    }

    @Test
    fun parentsFirstComparatorParentlessBeforeChild() {
        val parentless = Task()
        val child = Task().apply { parent = "p1" }
        val comparator = parentsFirstComparator()
        assertTrue(comparator.compare(parentless, child) < 0)
    }

    @Test
    fun parentsFirstComparatorChildAfterParentless() {
        val parentless = Task()
        val child = Task().apply { parent = "p1" }
        val comparator = parentsFirstComparator()
        assertTrue(comparator.compare(child, parentless) > 0)
    }

    @Test
    fun parentsFirstSortSeveralTasks() {
        val parent1 = Task().apply { id = "root1" }
        val parent2 = Task().apply { id = "root2" }
        val child1 = Task().apply { id = "child1"; parent = "root1" }
        val child2 = Task().apply { id = "child2"; parent = "root2" }
        val child3 = Task().apply { id = "child3"; parent = "root1" }

        val tasks = mutableListOf(child1, parent1, child3, parent2, child2)
        Collections.sort(tasks, parentsFirstComparator())

        // All parentless tasks should come first
        assertNull(tasks[0].parent)
        assertNull(tasks[1].parent)
        assertNotNull(tasks[2].parent)
        assertNotNull(tasks[3].parent)
        assertNotNull(tasks[4].parent)
    }

    @Test
    fun parentsFirstEmptyParentTreatedAsNull() {
        val emptyParent = Task().apply { parent = "" }
        val withParent = Task().apply { parent = "p1" }
        val comparator = parentsFirstComparator()
        // Empty parent should be treated like no parent
        assertTrue(comparator.compare(emptyParent, withParent) < 0)
    }

    @Test
    fun parentsFirstNullParentTreatedAsNoParent() {
        val nullParent = Task() // parent is null by default
        val withParent = Task().apply { parent = "p1" }
        val comparator = parentsFirstComparator()
        assertTrue(comparator.compare(nullParent, withParent) < 0)
    }

    private fun parentsFirstComparator(): Comparator<Task> {
        return Comparator { o1: Task, o2: Task ->
            if (Strings.isNullOrEmpty(o1.parent)) {
                if (Strings.isNullOrEmpty(o2.parent)) 0 else -1
            } else {
                if (Strings.isNullOrEmpty(o2.parent)) 1 else 0
            }
        }
    }
}
