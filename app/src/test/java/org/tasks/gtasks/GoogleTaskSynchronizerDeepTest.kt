package org.tasks.gtasks

import com.google.api.services.tasks.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.createDueDate

class GoogleTaskSynchronizerDeepTest {

    // ===== truncate: comprehensive =====

    @Test fun truncateNull() { assertNull(GoogleTaskSynchronizer.truncate(null, 10)) }
    @Test fun truncateEmpty() { assertEquals("", GoogleTaskSynchronizer.truncate("", 10)) }
    @Test fun truncateShort() { assertEquals("abc", GoogleTaskSynchronizer.truncate("abc", 10)) }
    @Test fun truncateExact() { assertEquals("abcde", GoogleTaskSynchronizer.truncate("abcde", 5)) }
    @Test fun truncateLong() { assertEquals("abc", GoogleTaskSynchronizer.truncate("abcdef", 3)) }
    @Test fun truncateTitleMax() { assertEquals(1024, GoogleTaskSynchronizer.truncate("a".repeat(2000), 1024)!!.length) }
    @Test fun truncateDescMax() { assertEquals(8192, GoogleTaskSynchronizer.truncate("b".repeat(10000), 8192)!!.length) }
    @Test fun truncateToZero() { assertEquals("", GoogleTaskSynchronizer.truncate("hello", 0)) }
    @Test fun truncateToOne() { assertEquals("h", GoogleTaskSynchronizer.truncate("hello", 1)) }
    @Test fun truncateUnicode() { assertEquals("\u00E9\u00E8", GoogleTaskSynchronizer.truncate("\u00E9\u00E8\u00EA", 2)) }
    @Test fun truncateWithSpaces() { assertEquals("hello ", GoogleTaskSynchronizer.truncate("hello world", 6)) }
    @Test fun truncateAtBoundary1024() { val s = "a".repeat(1024); assertEquals(s, GoogleTaskSynchronizer.truncate(s, 1024)) }
    @Test fun truncateAtBoundary1025() { assertEquals(1024, GoogleTaskSynchronizer.truncate("a".repeat(1025), 1024)!!.length) }
    @Test fun truncateAtBoundary8192() { val s = "b".repeat(8192); assertEquals(s, GoogleTaskSynchronizer.truncate(s, 8192)) }
    @Test fun truncateAtBoundary8193() { assertEquals(8192, GoogleTaskSynchronizer.truncate("b".repeat(8193), 8192)!!.length) }
    @Test fun truncateMaxIntReturnsOriginal() { val s = "test"; assertEquals(s, GoogleTaskSynchronizer.truncate(s, Int.MAX_VALUE)) }

    // ===== getTruncatedValue: comprehensive =====

    @Test fun getTruncBothNull() { assertNull(GoogleTaskSynchronizer.getTruncatedValue(null, null, 100)) }
    @Test fun getTruncNewNull() { assertNull(GoogleTaskSynchronizer.getTruncatedValue("c", null, 100)) }
    @Test fun getTruncNewEmpty() { assertEquals("", GoogleTaskSynchronizer.getTruncatedValue("c", "", 100)) }
    @Test fun getTruncCurrNullNewNotNull() { assertEquals("n", GoogleTaskSynchronizer.getTruncatedValue(null, "n", 100)) }
    @Test fun getTruncCurrEmptyNewNotNull() { assertEquals("n", GoogleTaskSynchronizer.getTruncatedValue("", "n", 100)) }
    @Test fun getTruncShortNew() { assertEquals("s", GoogleTaskSynchronizer.getTruncatedValue("e", "s", 100)) }
    @Test fun getTruncExactMaxStartsWith() { assertEquals("12345_ext", GoogleTaskSynchronizer.getTruncatedValue("12345_ext", "12345", 5)) }
    @Test fun getTruncExactMaxNotStartsWith() { assertEquals("12345", GoogleTaskSynchronizer.getTruncatedValue("abcde_e", "12345", 5)) }
    @Test fun getTruncOverMaxStartsWith() { assertEquals("123456_e", GoogleTaskSynchronizer.getTruncatedValue("123456_e", "123456", 5)) }
    @Test fun getTruncOverMaxNotStartsWith() { assertEquals("abcdef", GoogleTaskSynchronizer.getTruncatedValue("xyz_e", "abcdef", 5)) }
    @Test fun getTruncBothEmpty() { assertEquals("", GoogleTaskSynchronizer.getTruncatedValue("", "", 100)) }
    @Test fun getTruncNewExactMaxCurrentStartsWith() { assertEquals("abcde_more", GoogleTaskSynchronizer.getTruncatedValue("abcde_more", "abcde", 5)) }
    @Test fun getTruncNewShorterThanMax() { assertEquals("abc", GoogleTaskSynchronizer.getTruncatedValue("def", "abc", 10)) }
    @Test fun getTruncBothNullReturnsNull() { assertNull(GoogleTaskSynchronizer.getTruncatedValue(null, null, 100)) }
    @Test fun getTruncNewNullReturnsNull() { assertNull(GoogleTaskSynchronizer.getTruncatedValue("existing", null, 100)) }
    @Test fun getTruncPreservesCurrentWhenTruncated() {
        val c = "This is a very long description"
        val n = "This is a very long"
        assertEquals(c, GoogleTaskSynchronizer.getTruncatedValue(c, n, 15))
    }
    @Test fun getTruncReturnsNewWhenNoMatch() {
        assertEquals("This is a very long", GoogleTaskSynchronizer.getTruncatedValue("Different text", "This is a very long", 15))
    }

    // ===== mergeDates: comprehensive =====

    @Test fun mergeDatesZero() {
        val t = org.tasks.data.entity.Task(dueDate = 1000L)
        GoogleTaskSynchronizer.mergeDates(0L, t)
        assertEquals(0L, t.dueDate)
    }

    @Test fun mergeDatesNoExistingTime() {
        val rd = createDueDate(org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY, 1718409600000L)
        val t = org.tasks.data.entity.Task(dueDate = 0L)
        GoogleTaskSynchronizer.mergeDates(rd, t)
        assertEquals(rd, t.dueDate)
    }

    @Test fun mergeDatesWithExistingTime() {
        val ed = createDueDate(org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY_TIME, 1718409600000L)
        val t = org.tasks.data.entity.Task(dueDate = ed)
        assertTrue(t.hasDueTime())
        val rd = createDueDate(org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY, 1718496000000L)
        GoogleTaskSynchronizer.mergeDates(rd, t)
        assertTrue(t.hasDueTime())
    }

    @Test fun mergeDatesNegative() {
        val t = org.tasks.data.entity.Task(dueDate = 1000L)
        GoogleTaskSynchronizer.mergeDates(-1L, t)
        assertEquals(-1L, t.dueDate)
    }

    @Test fun mergeDatesPositiveRemoteNoExisting() {
        val rd = createDueDate(org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY, 1718409600000L)
        val t = org.tasks.data.entity.Task()
        GoogleTaskSynchronizer.mergeDates(rd, t)
        assertTrue(t.hasDueDate())
    }

    @Test fun mergeDatesZeroRemoteNoExistingDue() {
        val t = org.tasks.data.entity.Task(dueDate = 0L)
        GoogleTaskSynchronizer.mergeDates(0L, t)
        assertEquals(0L, t.dueDate)
    }

    @Test fun mergeDatesNonZeroRemoteWithDayOnly() {
        val rd = createDueDate(org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY, 1718409600000L)
        val existingDayOnly = createDueDate(org.tasks.data.entity.Task.URGENCY_SPECIFIC_DAY, 1718323200000L)
        val t = org.tasks.data.entity.Task(dueDate = existingDayOnly)
        GoogleTaskSynchronizer.mergeDates(rd, t)
        assertEquals(rd, t.dueDate)
    }

    // ===== PARENTS_FIRST comparator =====

    @Test fun parentsFirstBothNoParent() {
        val tasks = mutableListOf(Task(), Task())
        java.util.Collections.sort(tasks, pfc())
        assertNull(tasks[0].parent)
    }

    @Test fun parentsFirstParentlessBeforeChild() {
        val tasks = mutableListOf(Task().apply { parent = "p" }, Task())
        java.util.Collections.sort(tasks, pfc())
        assertNull(tasks[0].parent)
    }

    @Test fun parentsFirstBothHaveParents() {
        val tasks = mutableListOf(Task().apply { parent = "a" }, Task().apply { parent = "b" })
        java.util.Collections.sort(tasks, pfc())
        assertEquals("a", tasks[0].parent)
    }

    @Test fun parentsFirstEmptyIsNoParent() {
        val tasks = mutableListOf(Task().apply { parent = "p" }, Task().apply { parent = "" })
        java.util.Collections.sort(tasks, pfc())
        assertEquals("", tasks[0].parent)
    }

    @Test fun parentsFirstMixed() {
        val tasks = mutableListOf(Task().apply { parent = "r" }, Task(), Task().apply { parent = "r" }, Task())
        java.util.Collections.sort(tasks, pfc())
        assertNull(tasks[0].parent)
        assertNull(tasks[1].parent)
        assertNotNull(tasks[2].parent)
        assertNotNull(tasks[3].parent)
    }

    @Test fun parentsFirstFiveTasks() {
        val p1 = Task().apply { id = "root1" }
        val p2 = Task().apply { id = "root2" }
        val c1 = Task().apply { id = "c1"; parent = "root1" }
        val c2 = Task().apply { id = "c2"; parent = "root2" }
        val c3 = Task().apply { id = "c3"; parent = "root1" }
        val tasks = mutableListOf(c1, p1, c3, p2, c2)
        java.util.Collections.sort(tasks, pfc())
        assertNull(tasks[0].parent)
        assertNull(tasks[1].parent)
        assertNotNull(tasks[2].parent)
        assertNotNull(tasks[3].parent)
        assertNotNull(tasks[4].parent)
    }

    @Test fun parentsFirstNullParentTreatedAsNoParent() {
        val tasks = mutableListOf(Task().apply { parent = "p1" }, Task())
        java.util.Collections.sort(tasks, pfc())
        assertNull(tasks[0].parent)
        assertEquals("p1", tasks[1].parent)
    }

    private fun pfc(): Comparator<Task> = Comparator { o1, o2 ->
        if (org.tasks.Strings.isNullOrEmpty(o1.parent)) { if (org.tasks.Strings.isNullOrEmpty(o2.parent)) 0 else -1 }
        else { if (org.tasks.Strings.isNullOrEmpty(o2.parent)) 1 else 0 }
    }
}
