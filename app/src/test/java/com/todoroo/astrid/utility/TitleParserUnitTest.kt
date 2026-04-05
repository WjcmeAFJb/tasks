package com.todoroo.astrid.utility

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import net.fortuna.ical4j.model.Recur.Frequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.tasks.data.dao.TagDataDao
import org.tasks.data.entity.Task
import org.tasks.repeats.RecurrenceUtils.newRecur

/**
 * JVM unit tests for [TitleParser].
 *
 * Exercises the pure-logic helpers (priority parsing, repeat parsing,
 * day/time parsing, tag extraction, and string utilities) without
 * requiring an Android device or emulator.
 */
class TitleParserUnitTest {

    private lateinit var tagDataDao: TagDataDao

    @Before
    fun setUp() {
        tagDataDao = mock(TagDataDao::class.java)
    }

    /** Set up default tag-as-is stub. Must be called inside a coroutine. */
    private suspend fun stubTagsAsIs() {
        `when`(tagDataDao.getTagWithCase(anyString()))
            .thenAnswer { it.arguments[0] as String }
    }

    /** Convenience: run the full parse pipeline. */
    private suspend fun parseTitle(title: String): Task {
        stubTagsAsIs()
        val task = Task(title = title)
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        return task
    }

    /** Convenience: run parse and also return collected tags. */
    private suspend fun parseTitleWithTags(title: String): Pair<Task, ArrayList<String>> {
        stubTagsAsIs()
        val task = Task(title = title)
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        return Pair(task, tags)
    }

    // ================================================================== //
    //  trimParenthesis
    // ================================================================== //

    @Test
    fun trimParenthesis_hashWithParens() {
        assertEquals("cool tag", TitleParser.trimParenthesis("#(cool tag)"))
    }

    @Test
    fun trimParenthesis_hashWithoutParens() {
        assertEquals("tag", TitleParser.trimParenthesis("#tag"))
    }

    @Test
    fun trimParenthesis_atWithParens() {
        assertEquals("context", TitleParser.trimParenthesis("@(context)"))
    }

    @Test
    fun trimParenthesis_atWithoutParens() {
        assertEquals("work", TitleParser.trimParenthesis("@work"))
    }

    @Test
    fun trimParenthesis_plainParens() {
        assertEquals("hello", TitleParser.trimParenthesis("(hello)"))
    }

    @Test
    fun trimParenthesis_noPrefixNoParens() {
        assertEquals("plain", TitleParser.trimParenthesis("plain"))
    }

    // ================================================================== //
    //  Priority parsing
    // ================================================================== //

    @Test
    fun priority_singleBang_setsLow() = runTest {
        assertEquals(Task.Priority.LOW, parseTitle("Jog !").priority)
    }

    @Test
    fun priority_doubleBang_setsMedium() = runTest {
        assertEquals(Task.Priority.MEDIUM, parseTitle("Jog !!").priority)
    }

    @Test
    fun priority_tripleBang_setsHigh() = runTest {
        assertEquals(Task.Priority.HIGH, parseTitle("Jog !!!").priority)
    }

    @Test
    fun priority_manyBangs_setsHigh() = runTest {
        assertEquals(Task.Priority.HIGH, parseTitle("Jog !!!!!!!").priority)
    }

    @Test
    fun priority_bangOne_setsLow() = runTest {
        assertEquals(Task.Priority.LOW, parseTitle("Jog !1").priority)
    }

    @Test
    fun priority_bangTwo_setsMedium() = runTest {
        assertEquals(Task.Priority.MEDIUM, parseTitle("Jog !2").priority)
    }

    @Test
    fun priority_bangThree_setsHigh() = runTest {
        assertEquals(Task.Priority.HIGH, parseTitle("Jog !3").priority)
    }

    @Test
    fun priority_bangZero_setsNone() = runTest {
        assertEquals(Task.Priority.NONE, parseTitle("Jog !0").priority)
    }

    @Test
    fun priority_priority1_atEnd() = runTest {
        assertEquals(Task.Priority.LOW, parseTitle("Jog priority 1").priority)
    }

    @Test
    fun priority_priority2_atEnd() = runTest {
        assertEquals(Task.Priority.MEDIUM, parseTitle("Jog priority 2").priority)
    }

    @Test
    fun priority_priority3_atEnd() = runTest {
        assertEquals(Task.Priority.HIGH, parseTitle("Jog priority 3").priority)
    }

    @Test
    fun priority_lowPriority() = runTest {
        assertEquals(Task.Priority.LOW, parseTitle("Jog low priority").priority)
    }

    @Test
    fun priority_highPriority_isMedium() = runTest {
        // "high" maps to MEDIUM in strToPriority (equivalent to !!)
        assertEquals(Task.Priority.MEDIUM, parseTitle("Jog high priority").priority)
    }

    @Test
    fun priority_highestPriority_isHigh() = runTest {
        // "highest" does not match any word, so stays at default HIGH
        assertEquals(Task.Priority.HIGH, parseTitle("Jog highest priority").priority)
    }

    @Test
    fun priority_lowestPriority_isNone() = runTest {
        assertEquals(Task.Priority.NONE, parseTitle("Jog lowest priority").priority)
    }

    @Test
    fun priority_leastPriority_isNone() = runTest {
        assertEquals(Task.Priority.NONE, parseTitle("Jog least priority").priority)
    }

    @Test
    fun priority_topPriority_isHigh() = runTest {
        // "top" does not match any word, so stays at default HIGH
        assertEquals(Task.Priority.HIGH, parseTitle("Jog top priority").priority)
    }

    @Test
    fun priority_bangKeyword_setsLow() = runTest {
        assertEquals(Task.Priority.LOW, parseTitle("Jog bang").priority)
    }

    @Test
    fun priority_bangBangKeyword_setsMedium() = runTest {
        assertEquals(Task.Priority.MEDIUM, parseTitle("Jog bang bang").priority)
    }

    @Test
    fun priority_bang0_setsNone() = runTest {
        assertEquals(Task.Priority.NONE, parseTitle("Jog bang 0").priority)
    }

    @Test
    fun priority_bang1_setsLow() = runTest {
        assertEquals(Task.Priority.LOW, parseTitle("Jog bang 1").priority)
    }

    @Test
    fun priority_bang2_setsMedium() = runTest {
        assertEquals(Task.Priority.MEDIUM, parseTitle("Jog bang 2").priority)
    }

    @Test
    fun priority_bang3_setsHigh() = runTest {
        assertEquals(Task.Priority.HIGH, parseTitle("Jog bang 3").priority)
    }

    @Test
    fun priority_stripped_fromTitle() = runTest {
        val task = parseTitle("Jog !!")
        assertFalse("Priority marker should be stripped", task.title!!.contains("!!"))
    }

    @Test
    fun priority_noMarker_defaultsToNone() = runTest {
        assertEquals(Task.Priority.NONE, parseTitle("Jog around the block").priority)
    }

    @Test
    fun priority_bangAtBeginning() = runTest {
        assertEquals(Task.Priority.LOW, parseTitle("!1 jog").priority)
    }

    @Test
    fun priority_doubleBangAtBeginning() = runTest {
        assertEquals(Task.Priority.MEDIUM, parseTitle("!! jog").priority)
    }

    // ================================================================== //
    //  Repeat parsing
    // ================================================================== //

    @Test
    fun repeat_daily() = runTest {
        val task = parseTitle("Jog daily")
        val expected = newRecur().also {
            it.setFrequency(Frequency.DAILY.name); it.interval = 1
        }
        assertEquals(expected.toString(), task.recurrence)
    }

    @Test
    fun repeat_everyday() = runTest {
        val task = parseTitle("Jog everyday")
        val expected = newRecur().also {
            it.setFrequency(Frequency.DAILY.name); it.interval = 1
        }
        assertEquals(expected.toString(), task.recurrence)
    }

    @Test
    fun repeat_weekly() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.WEEKLY.name); it.interval = 1
        }
        assertEquals(expected.toString(), parseTitle("Jog weekly").recurrence)
    }

    @Test
    fun repeat_monthly() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.MONTHLY.name); it.interval = 1
        }
        assertEquals(expected.toString(), parseTitle("Jog monthly").recurrence)
    }

    @Test
    fun repeat_yearly() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.YEARLY.name); it.interval = 1
        }
        assertEquals(expected.toString(), parseTitle("Jog yearly").recurrence)
    }

    @Test
    fun repeat_everyDay() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.DAILY.name); it.interval = 1
        }
        assertEquals(expected.toString(), parseTitle("Jog every day").recurrence)
    }

    @Test
    fun repeat_everyWeek() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.WEEKLY.name); it.interval = 1
        }
        assertEquals(expected.toString(), parseTitle("Jog every week").recurrence)
    }

    @Test
    fun repeat_everyMonth() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.MONTHLY.name); it.interval = 1
        }
        assertEquals(expected.toString(), parseTitle("Jog every month").recurrence)
    }

    @Test
    fun repeat_everyYear() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.YEARLY.name); it.interval = 1
        }
        assertEquals(expected.toString(), parseTitle("Jog every year").recurrence)
    }

    @Test
    fun repeat_every3Days() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.DAILY.name); it.interval = 3
        }
        assertEquals(expected.toString(), parseTitle("Jog every 3 days").recurrence)
    }

    @Test
    fun repeat_every2Weeks() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.WEEKLY.name); it.interval = 2
        }
        assertEquals(expected.toString(), parseTitle("Jog every 2 weeks").recurrence)
    }

    @Test
    fun repeat_every6Months() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.MONTHLY.name); it.interval = 6
        }
        assertEquals(expected.toString(), parseTitle("Jog every 6 months").recurrence)
    }

    @Test
    fun repeat_everyOtherDay() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.DAILY.name); it.interval = 2
        }
        assertEquals(expected.toString(), parseTitle("Jog every other day").recurrence)
    }

    @Test
    fun repeat_everyThreeWeeks() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.WEEKLY.name); it.interval = 3
        }
        assertEquals(expected.toString(), parseTitle("Jog every three weeks").recurrence)
    }

    @Test
    fun repeat_everyTwelveMonths() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.MONTHLY.name); it.interval = 12
        }
        assertEquals(expected.toString(), parseTitle("Jog every twelve months").recurrence)
    }

    @Test
    fun repeat_everyMorning() = runTest {
        val task = parseTitle("Jog every morning")
        assertNotNull(task.recurrence)
        assertTrue(task.recurrence!!.contains("DAILY"))
    }

    @Test
    fun repeat_everyNight() = runTest {
        val task = parseTitle("Jog every night")
        assertNotNull(task.recurrence)
        assertTrue(task.recurrence!!.contains("DAILY"))
    }

    @Test
    fun repeat_everyAfternoon() = runTest {
        val task = parseTitle("Jog every afternoon")
        assertNotNull(task.recurrence)
        assertTrue(task.recurrence!!.contains("DAILY"))
    }

    @Test
    fun repeat_everyMonday() = runTest {
        val task = parseTitle("Jog every monday")
        assertNotNull(task.recurrence)
        assertTrue(task.recurrence!!.contains("WEEKLY"))
    }

    @Test
    fun repeat_everyFriday() = runTest {
        val task = parseTitle("Jog every friday")
        assertNotNull(task.recurrence)
        assertTrue(task.recurrence!!.contains("WEEKLY"))
    }

    @Test
    fun repeat_noRepeat_noRecurrence() = runTest {
        assertNull(parseTitle("Jog around the block").recurrence)
    }

    // ================================================================== //
    //  Day/time parsing
    // ================================================================== //

    @Test
    fun day_today_setsDueDate() = runTest {
        assertTrue(parseTitle("Jog today").hasDueDate())
    }

    @Test
    fun day_tomorrow_setsDueDate() = runTest {
        assertTrue(parseTitle("Jog tomorrow").hasDueDate())
    }

    @Test
    fun day_monday_setsDueDate() = runTest {
        assertTrue(parseTitle("Jog monday").hasDueDate())
    }

    @Test
    fun day_abbreviatedDay_setsDueDate() = runTest {
        assertTrue(parseTitle("Jog fri.").hasDueDate())
    }

    @Test
    fun day_monthDate_setsDueDate() = runTest {
        assertTrue(parseTitle("Jog on January 15").hasDueDate())
    }

    @Test
    fun day_slashDate_setsDueDate() = runTest {
        assertTrue(parseTitle("Jog on 3/15").hasDueDate())
    }

    @Test
    fun day_slashDateWithYear_setsDueDate() = runTest {
        assertTrue(parseTitle("Jog on 3/15/2025").hasDueDate())
    }

    @Test
    fun day_noDateKeyword_noDueDate() = runTest {
        assertFalse(parseTitle("Jog around the block").hasDueDate())
    }

    @Test
    fun day_breakfast_setsTime() = runTest {
        assertTrue(parseTitle("Jog today breakfast").hasDueTime())
    }

    @Test
    fun day_lunch_setsTime() = runTest {
        assertTrue(parseTitle("Jog today lunch").hasDueTime())
    }

    @Test
    fun day_dinner_setsTime() = runTest {
        assertTrue(parseTitle("Jog today dinner").hasDueTime())
    }

    @Test
    fun day_noon_setsTime() = runTest {
        assertTrue(parseTitle("Jog today noon").hasDueTime())
    }

    @Test
    fun day_midnight_setsTime() = runTest {
        assertTrue(parseTitle("Jog today midnight").hasDueTime())
    }

    @Test
    fun day_morning_setsTime() = runTest {
        assertTrue(parseTitle("Jog today morning").hasDueTime())
    }

    @Test
    fun day_evening_setsTime() = runTest {
        assertTrue(parseTitle("Jog today evening").hasDueTime())
    }

    @Test
    fun day_ampm_setsTime() = runTest {
        assertTrue(parseTitle("Jog 8:30 PM").hasDueTime())
    }

    @Test
    fun day_armyTime_setsTime() = runTest {
        assertTrue(parseTitle("Jog 14:30").hasDueTime())
    }

    @Test
    fun day_oclock_setsTime() = runTest {
        assertTrue(parseTitle("Jog 3 o'clock PM").hasDueTime())
    }

    @Test
    fun day_atHour_setsTime() = runTest {
        assertTrue(parseTitle("Jog at 5 PM").hasDueTime())
    }

    @Test
    fun day_dashDate_setsDueDate() = runTest {
        assertTrue(parseTitle("Jog on 6-15").hasDueDate())
    }

    @Test
    fun day_twoDigitYear_setsDueDate() = runTest {
        assertTrue(parseTitle("Jog on 6/15/25").hasDueDate())
    }

    // ================================================================== //
    //  Tag parsing (via listHelper)
    // ================================================================== //

    @Test
    fun tags_hashTag() = runTest {
        val (task, tags) = parseTitleWithTags("Jog #fitness")
        assertTrue(tags.contains("fitness"))
        assertEquals("Jog", task.title)
    }

    @Test
    fun tags_atTag() = runTest {
        val (task, tags) = parseTitleWithTags("Jog @gym")
        assertTrue(tags.contains("gym"))
        assertEquals("Jog", task.title)
    }

    @Test
    fun tags_hashWithParenthesis() = runTest {
        val (_, tags) = parseTitleWithTags("Jog #(cool tag)")
        assertTrue(tags.contains("cool tag"))
    }

    @Test
    fun tags_atWithParenthesis() = runTest {
        val (_, tags) = parseTitleWithTags("Jog @(at work)")
        assertTrue(tags.contains("at work"))
    }

    @Test
    fun tags_multipleTags() = runTest {
        val (_, tags) = parseTitleWithTags("Jog #fitness #outdoor")
        assertTrue(tags.contains("fitness"))
        assertTrue(tags.contains("outdoor"))
    }

    @Test
    fun tags_noTags_emptyList() = runTest {
        val (_, tags) = parseTitleWithTags("Jog around the block")
        assertTrue(tags.isEmpty())
    }

    @Test
    fun tags_strippedFromTitle() = runTest {
        val (task, _) = parseTitleWithTags("Jog #fitness today")
        assertFalse(task.title!!.contains("#fitness"))
    }

    @Test
    fun tags_duplicateTagNotAddedTwice() = runTest {
        val (_, tags) = parseTitleWithTags("Jog #fitness #fitness")
        assertEquals(1, tags.size)
    }

    @Test
    fun tags_casePreserved_whenDaoReturnsTag() = runTest {
        `when`(tagDataDao.getTagWithCase(anyString()))
            .thenAnswer { it.arguments[0] as String }
        `when`(tagDataDao.getTagWithCase("fitness")).thenReturn("Fitness")
        val task = Task(title = "Jog #fitness")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertTrue(tags.contains("Fitness"))
    }

    @Test
    fun tags_notAdded_whenDaoReturnsNull() = runTest {
        `when`(tagDataDao.getTagWithCase(anyString())).thenReturn(null)
        val task = Task(title = "Jog #nonexistent")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertTrue(tags.isEmpty())
    }

    // ================================================================== //
    //  Combined parsing
    // ================================================================== //

    @Test
    fun combined_priorityAndRepeat() = runTest {
        val task = parseTitle("Jog daily !!")
        assertEquals(Task.Priority.MEDIUM, task.priority)
        assertNotNull(task.recurrence)
    }

    @Test
    fun combined_repeatAndDate() = runTest {
        val task = parseTitle("Jog daily today")
        assertNotNull(task.recurrence)
        assertTrue(task.hasDueDate())
    }

    @Test
    fun noRegexes_defaultTask() = runTest {
        val task = parseTitle("Jog")
        assertFalse(task.hasDueTime())
        assertFalse(task.hasDueDate())
        assertNull(task.recurrence)
        assertEquals(Task.Priority.NONE, task.priority)
    }

    @Test
    fun combined_tagAndPriority() = runTest {
        val (task, tags) = parseTitleWithTags("Buy milk #groceries !!")
        assertTrue(tags.contains("groceries"))
        assertEquals(Task.Priority.MEDIUM, task.priority)
    }

    @Test
    fun combined_dateAndPriority() = runTest {
        val task = parseTitle("Buy milk today !")
        assertTrue(task.hasDueDate())
        assertEquals(Task.Priority.LOW, task.priority)
    }
}
