package com.todoroo.astrid.utility

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.Task

class TitleParserUnitTest {

    private lateinit var tagDataDao: TagDataDao

    @Before
    fun setUp() {
        tagDataDao = mock(TagDataDao::class.java)
    }

    // --- priority extraction ---

    @Test
    fun singleBangSetsPriorityLow() = runTest {
        val task = Task(title = "Buy milk !")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertEquals(Task.Priority.LOW, task.priority)
    }

    @Test
    fun doubleBangSetsPriorityMedium() = runTest {
        val task = Task(title = "Buy milk !!")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertEquals(Task.Priority.MEDIUM, task.priority)
    }

    @Test
    fun tripleBangSetsPriorityHigh() = runTest {
        val task = Task(title = "Buy milk !!!")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertEquals(Task.Priority.HIGH, task.priority)
    }

    @Test
    fun bangOneSetsPriorityLow() = runTest {
        val task = Task(title = "Buy milk !1")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertEquals(Task.Priority.LOW, task.priority)
    }

    @Test
    fun bangTwoSetsPriorityMedium() = runTest {
        val task = Task(title = "Buy milk !2")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertEquals(Task.Priority.MEDIUM, task.priority)
    }

    @Test
    fun bangThreeSetsPriorityHigh() = runTest {
        val task = Task(title = "Buy milk !3")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertEquals(Task.Priority.HIGH, task.priority)
    }

    @Test
    fun bangZeroSetsPriorityNone() = runTest {
        val task = Task(title = "Buy milk !0")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertEquals(Task.Priority.NONE, task.priority)
    }

    @Test
    fun priorityBangRemovedFromTitle() = runTest {
        val task = Task(title = "Buy milk !!")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertEquals("Buy milk", task.title)
    }

    @Test
    fun priorityWordHighSetsCorrectPriority() = runTest {
        val task = Task(title = "Buy milk priority 2")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertEquals(Task.Priority.MEDIUM, task.priority)
    }

    @Test
    fun priorityWordLowSetsCorrectPriority() = runTest {
        val task = Task(title = "Buy milk priority 1")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertEquals(Task.Priority.LOW, task.priority)
    }

    @Test
    fun bangKeywordSetsLowPriority() = runTest {
        val task = Task(title = "Buy milk bang")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertEquals(Task.Priority.LOW, task.priority)
    }

    @Test
    fun highPriorityKeyword() = runTest {
        val task = Task(title = "Buy milk high priority")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertEquals(Task.Priority.HIGH, task.priority)
    }

    @Test
    fun lowPriorityKeyword() = runTest {
        val task = Task(title = "Buy milk low priority")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertEquals(Task.Priority.LOW, task.priority)
    }

    @Test
    fun leastPriorityKeyword() = runTest {
        val task = Task(title = "Buy milk least priority")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertEquals(Task.Priority.NONE, task.priority)
    }

    @Test
    fun noPriorityKeepsDefault() = runTest {
        val task = Task(title = "Buy milk")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertEquals(Task.Priority.NONE, task.priority)
    }

    // --- tag extraction ---

    @Test
    fun hashtagExtractsTag() = runTest {
        val task = Task(title = "Buy milk #groceries")
        val tags = ArrayList<String>()
        `when`(tagDataDao.getTagWithCase("groceries")).thenReturn("Groceries")
        TitleParser.parse(tagDataDao, task, tags)
        assertTrue(tags.contains("Groceries"))
    }

    @Test
    fun atSignExtractsTag() = runTest {
        val task = Task(title = "Call Bob @work")
        val tags = ArrayList<String>()
        `when`(tagDataDao.getTagWithCase("work")).thenReturn("Work")
        TitleParser.parse(tagDataDao, task, tags)
        assertTrue(tags.contains("Work"))
    }

    @Test
    fun unknownTagNotAdded() = runTest {
        val task = Task(title = "Buy milk #nonexistent")
        val tags = ArrayList<String>()
        `when`(tagDataDao.getTagWithCase("nonexistent")).thenReturn(null)
        TitleParser.parse(tagDataDao, task, tags)
        assertTrue(tags.isEmpty())
    }

    @Test
    fun hashtagRemovedFromTitle() = runTest {
        val task = Task(title = "Buy milk #groceries")
        val tags = ArrayList<String>()
        `when`(tagDataDao.getTagWithCase("groceries")).thenReturn("Groceries")
        TitleParser.parse(tagDataDao, task, tags)
        assertEquals("Buy milk", task.title)
    }

    @Test
    fun multipleHashtags() = runTest {
        val task = Task(title = "Buy milk #groceries #food")
        val tags = ArrayList<String>()
        `when`(tagDataDao.getTagWithCase("groceries")).thenReturn("Groceries")
        `when`(tagDataDao.getTagWithCase("food")).thenReturn("Food")
        TitleParser.parse(tagDataDao, task, tags)
        assertTrue(tags.contains("Groceries"))
        assertTrue(tags.contains("Food"))
    }

    @Test
    fun parenthesisTagExtracted() = runTest {
        val task = Task(title = "Buy milk #(my tag)")
        val tags = ArrayList<String>()
        `when`(tagDataDao.getTagWithCase("my tag")).thenReturn("My Tag")
        TitleParser.parse(tagDataDao, task, tags)
        assertTrue(tags.contains("My Tag"))
    }

    @Test
    fun duplicateTagNotAddedTwice() = runTest {
        val task = Task(title = "Buy milk #work @work")
        val tags = ArrayList<String>()
        `when`(tagDataDao.getTagWithCase("work")).thenReturn("Work")
        TitleParser.parse(tagDataDao, task, tags)
        assertEquals(1, tags.count { it == "Work" })
    }

    // --- date extraction ---

    @Test
    fun todaySetsDate() = runTest {
        val task = Task(title = "Buy milk today")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertTrue(task.hasDueDate())
    }

    @Test
    fun tomorrowSetsDate() = runTest {
        val task = Task(title = "Buy milk tomorrow")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertTrue(task.hasDueDate())
    }

    @Test
    fun dayOfWeekSetsDate() = runTest {
        val task = Task(title = "Buy milk monday")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertTrue(task.hasDueDate())
    }

    @Test
    fun noDateKeepsDueDateZero() = runTest {
        val task = Task(title = "Buy milk")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertEquals(0L, task.dueDate)
    }

    // --- repeat extraction ---

    @Test
    fun dailyRepeatExtracted() = runTest {
        val task = Task(title = "Take medicine daily")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertNotNull(task.recurrence)
        assertTrue(task.recurrence!!.contains("DAILY"))
    }

    @Test
    fun weeklyRepeatExtracted() = runTest {
        val task = Task(title = "Review weekly")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertNotNull(task.recurrence)
        assertTrue(task.recurrence!!.contains("WEEKLY"))
    }

    @Test
    fun monthlyRepeatExtracted() = runTest {
        val task = Task(title = "Pay rent monthly")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertNotNull(task.recurrence)
        assertTrue(task.recurrence!!.contains("MONTHLY"))
    }

    @Test
    fun yearlyRepeatExtracted() = runTest {
        val task = Task(title = "Renew license yearly")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertNotNull(task.recurrence)
        assertTrue(task.recurrence!!.contains("YEARLY"))
    }

    @Test
    fun everyDayRepeatExtracted() = runTest {
        val task = Task(title = "Take medicine every day")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertNotNull(task.recurrence)
        assertTrue(task.recurrence!!.contains("DAILY"))
    }

    @Test
    fun everyThreeDaysRepeatExtracted() = runTest {
        val task = Task(title = "Take medicine every three days")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertNotNull(task.recurrence)
        assertTrue(task.recurrence!!.contains("DAILY"))
        assertTrue(task.recurrence!!.contains("INTERVAL=3"))
    }

    @Test
    fun noRepeatKeepsRecurrenceNull() = runTest {
        val task = Task(title = "Buy milk")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertNull(task.recurrence)
    }

    // --- trimParenthesis ---

    @Test
    fun trimParenthesisRemovesParens() {
        assertEquals("tag name", TitleParser.trimParenthesis("(tag name)"))
    }

    @Test
    fun trimParenthesisRemovesHash() {
        assertEquals("tag", TitleParser.trimParenthesis("#tag"))
    }

    @Test
    fun trimParenthesisRemovesAt() {
        assertEquals("context", TitleParser.trimParenthesis("@context"))
    }

    @Test
    fun trimParenthesisNonParenthesisReturnsUnchanged() {
        assertEquals("plain", TitleParser.trimParenthesis("plain"))
    }

    // --- combined parsing ---

    @Test
    fun combinedPriorityAndTag() = runTest {
        val task = Task(title = "Buy milk #groceries !!")
        val tags = ArrayList<String>()
        `when`(tagDataDao.getTagWithCase("groceries")).thenReturn("Groceries")
        TitleParser.parse(tagDataDao, task, tags)
        assertTrue(tags.contains("Groceries"))
        assertEquals(Task.Priority.MEDIUM, task.priority)
    }

    @Test
    fun combinedDateAndPriority() = runTest {
        val task = Task(title = "Buy milk today !")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertTrue(task.hasDueDate())
        assertEquals(Task.Priority.LOW, task.priority)
    }
}
