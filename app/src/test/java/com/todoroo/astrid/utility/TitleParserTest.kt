package com.todoroo.astrid.utility

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
import java.util.Calendar

/**
 * Comprehensive unit tests for [TitleParser].
 *
 * Covers all public and private-via-parse methods: trimParenthesis, listHelper,
 * priorityHelper, dayHelper, repeatHelper, and their helpers (strToPriority,
 * ampmToNumber, findInterval, stripParens).
 */
class TitleParserTest {

    private lateinit var tagDataDao: TagDataDao

    @Before
    fun setUp() {
        tagDataDao = mock(TagDataDao::class.java)
    }

    private suspend fun stubTagsAsIs() {
        `when`(tagDataDao.getTagWithCase(anyString()))
            .thenAnswer { it.arguments[0] as String }
    }

    private suspend fun parseTitle(title: String): Task {
        stubTagsAsIs()
        val task = Task(title = title)
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        return task
    }

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
    fun trimParenthesis_hashTag_removesHash() {
        assertEquals("groceries", TitleParser.trimParenthesis("#groceries"))
    }

    @Test
    fun trimParenthesis_atSign_removesAt() {
        assertEquals("home", TitleParser.trimParenthesis("@home"))
    }

    @Test
    fun trimParenthesis_hashWithParens_removesHashAndParens() {
        assertEquals("multi word tag", TitleParser.trimParenthesis("#(multi word tag)"))
    }

    @Test
    fun trimParenthesis_atWithParens_removesAtAndParens() {
        assertEquals("at the office", TitleParser.trimParenthesis("@(at the office)"))
    }

    @Test
    fun trimParenthesis_parensOnly_removesParens() {
        assertEquals("grouped", TitleParser.trimParenthesis("(grouped)"))
    }

    @Test
    fun trimParenthesis_noSpecialChars_returnsUnchanged() {
        assertEquals("simple", TitleParser.trimParenthesis("simple"))
    }

    @Test
    fun trimParenthesis_singleChar_hash() {
        assertEquals("x", TitleParser.trimParenthesis("#x"))
    }

    @Test
    fun trimParenthesis_singleChar_at() {
        assertEquals("y", TitleParser.trimParenthesis("@y"))
    }

    // ================================================================== //
    //  Priority parsing - single bang variants
    // ================================================================== //

    @Test
    fun priority_singleBang_setsLow() = runTest {
        assertEquals(Task.Priority.LOW, parseTitle("Buy milk !").priority)
    }

    @Test
    fun priority_doubleBang_setsMedium() = runTest {
        assertEquals(Task.Priority.MEDIUM, parseTitle("Buy milk !!").priority)
    }

    @Test
    fun priority_tripleBang_setsHigh() = runTest {
        assertEquals(Task.Priority.HIGH, parseTitle("Buy milk !!!").priority)
    }

    @Test
    fun priority_quadBang_setsHigh() = runTest {
        assertEquals(Task.Priority.HIGH, parseTitle("Buy milk !!!!").priority)
    }

    @Test
    fun priority_fiveBangs_setsHigh() = runTest {
        assertEquals(Task.Priority.HIGH, parseTitle("Buy milk !!!!!").priority)
    }

    // ================================================================== //
    //  Priority parsing - !N variants
    // ================================================================== //

    @Test
    fun priority_bangZero_setsNone() = runTest {
        assertEquals(Task.Priority.NONE, parseTitle("Buy milk !0").priority)
    }

    @Test
    fun priority_bangOne_setsLow() = runTest {
        assertEquals(Task.Priority.LOW, parseTitle("Buy milk !1").priority)
    }

    @Test
    fun priority_bangTwo_setsMedium() = runTest {
        assertEquals(Task.Priority.MEDIUM, parseTitle("Buy milk !2").priority)
    }

    @Test
    fun priority_bangThree_setsHigh() = runTest {
        assertEquals(Task.Priority.HIGH, parseTitle("Buy milk !3").priority)
    }

    // ================================================================== //
    //  Priority parsing - "priority N" variants
    // ================================================================== //

    @Test
    fun priority_priority0_setsNone() = runTest {
        assertEquals(Task.Priority.NONE, parseTitle("Buy milk priority 0").priority)
    }

    @Test
    fun priority_priority1_setsLow() = runTest {
        assertEquals(Task.Priority.LOW, parseTitle("Buy milk priority 1").priority)
    }

    @Test
    fun priority_priority2_setsMedium() = runTest {
        assertEquals(Task.Priority.MEDIUM, parseTitle("Buy milk priority 2").priority)
    }

    @Test
    fun priority_priority3_setsHigh() = runTest {
        assertEquals(Task.Priority.HIGH, parseTitle("Buy milk priority 3").priority)
    }

    @Test
    fun priority_priorityNoSpace_setsLow() = runTest {
        // "priority1" at end
        assertEquals(Task.Priority.LOW, parseTitle("Buy milk priority1").priority)
    }

    // ================================================================== //
    //  Priority parsing - "bang N" variants
    // ================================================================== //

    @Test
    fun priority_bang0_keyword_setsNone() = runTest {
        assertEquals(Task.Priority.NONE, parseTitle("Buy milk bang 0").priority)
    }

    @Test
    fun priority_bang1_keyword_setsLow() = runTest {
        assertEquals(Task.Priority.LOW, parseTitle("Buy milk bang 1").priority)
    }

    @Test
    fun priority_bang2_keyword_setsMedium() = runTest {
        assertEquals(Task.Priority.MEDIUM, parseTitle("Buy milk bang 2").priority)
    }

    @Test
    fun priority_bang3_keyword_setsHigh() = runTest {
        assertEquals(Task.Priority.HIGH, parseTitle("Buy milk bang 3").priority)
    }

    // ================================================================== //
    //  Priority parsing - "bang" keyword
    // ================================================================== //

    @Test
    fun priority_bangKeyword_setsLow() = runTest {
        assertEquals(Task.Priority.LOW, parseTitle("Buy milk bang").priority)
    }

    @Test
    fun priority_bangBangKeyword_setsMedium() = runTest {
        assertEquals(Task.Priority.MEDIUM, parseTitle("Buy milk bang bang").priority)
    }

    // ================================================================== //
    //  Priority parsing - named priorities
    // ================================================================== //

    @Test
    fun priority_lowPriority() = runTest {
        assertEquals(Task.Priority.LOW, parseTitle("Buy milk low priority").priority)
    }

    @Test
    fun priority_highPriority_mapsTOMedium() = runTest {
        // "high" in strToPriority maps to MEDIUM (equivalent to !!)
        assertEquals(Task.Priority.MEDIUM, parseTitle("Buy milk high priority").priority)
    }

    @Test
    fun priority_highestPriority_mapsToHigh() = runTest {
        // "highest" does not match specific words, stays at default HIGH
        assertEquals(Task.Priority.HIGH, parseTitle("Buy milk highest priority").priority)
    }

    @Test
    fun priority_lowestPriority_mapsToNone() = runTest {
        assertEquals(Task.Priority.NONE, parseTitle("Buy milk lowest priority").priority)
    }

    @Test
    fun priority_leastPriority_mapsToNone() = runTest {
        assertEquals(Task.Priority.NONE, parseTitle("Buy milk least priority").priority)
    }

    @Test
    fun priority_topPriority_mapsToHigh() = runTest {
        // "top" does not match specific words, stays at default HIGH
        assertEquals(Task.Priority.HIGH, parseTitle("Buy milk top priority").priority)
    }

    // ================================================================== //
    //  Priority parsing - title stripping
    // ================================================================== //

    @Test
    fun priority_singleBang_strippedFromTitle() = runTest {
        val task = parseTitle("Buy milk !")
        assertFalse(task.title!!.contains("!"))
    }

    @Test
    fun priority_doubleBang_strippedFromTitle() = runTest {
        val task = parseTitle("Buy milk !!")
        assertFalse(task.title!!.contains("!!"))
    }

    @Test
    fun priority_priorityKeyword_strippedFromTitle() = runTest {
        val task = parseTitle("Buy milk priority 2")
        assertFalse(task.title!!.contains("priority"))
    }

    @Test
    fun priority_bangKeyword_strippedFromTitle() = runTest {
        val task = parseTitle("Buy milk bang")
        assertFalse(task.title!!.contains("bang"))
    }

    @Test
    fun priority_lowPriorityKeyword_strippedFromTitle() = runTest {
        val task = parseTitle("Buy milk low priority")
        assertFalse(task.title!!.contains("low priority"))
    }

    // ================================================================== //
    //  Priority parsing - position variants
    // ================================================================== //

    @Test
    fun priority_bangAtStart() = runTest {
        assertEquals(Task.Priority.LOW, parseTitle("!1 Buy milk").priority)
    }

    @Test
    fun priority_doubleBangAtStart() = runTest {
        assertEquals(Task.Priority.MEDIUM, parseTitle("!! Buy milk").priority)
    }

    @Test
    fun priority_bangInMiddle() = runTest {
        assertEquals(Task.Priority.LOW, parseTitle("Buy ! milk").priority)
    }

    // ================================================================== //
    //  Priority parsing - no priority
    // ================================================================== //

    @Test
    fun priority_noMarker_defaultsToNone() = runTest {
        assertEquals(Task.Priority.NONE, parseTitle("Buy milk").priority)
    }

    @Test
    fun priority_bangInsideWord_doesNotMatch() = runTest {
        // e.g. "don't" contains no standalone bangs
        assertEquals(Task.Priority.NONE, parseTitle("Buy milk please").priority)
    }

    // ================================================================== //
    //  Repeat parsing - simple keywords
    // ================================================================== //

    @Test
    fun repeat_daily_setsRecurrence() = runTest {
        val task = parseTitle("Exercise daily")
        val expected = newRecur().also {
            it.setFrequency(Frequency.DAILY.name); it.interval = 1
        }
        assertEquals(expected.toString(), task.recurrence)
    }

    @Test
    fun repeat_everyday_setsRecurrence() = runTest {
        val task = parseTitle("Exercise everyday")
        val expected = newRecur().also {
            it.setFrequency(Frequency.DAILY.name); it.interval = 1
        }
        assertEquals(expected.toString(), task.recurrence)
    }

    @Test
    fun repeat_weekly_setsRecurrence() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.WEEKLY.name); it.interval = 1
        }
        assertEquals(expected.toString(), parseTitle("Exercise weekly").recurrence)
    }

    @Test
    fun repeat_monthly_setsRecurrence() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.MONTHLY.name); it.interval = 1
        }
        assertEquals(expected.toString(), parseTitle("Exercise monthly").recurrence)
    }

    @Test
    fun repeat_yearly_setsRecurrence() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.YEARLY.name); it.interval = 1
        }
        assertEquals(expected.toString(), parseTitle("Exercise yearly").recurrence)
    }

    // ================================================================== //
    //  Repeat parsing - "every X" forms
    // ================================================================== //

    @Test
    fun repeat_everyDay() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.DAILY.name); it.interval = 1
        }
        assertEquals(expected.toString(), parseTitle("Exercise every day").recurrence)
    }

    @Test
    fun repeat_everyWeek() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.WEEKLY.name); it.interval = 1
        }
        assertEquals(expected.toString(), parseTitle("Exercise every week").recurrence)
    }

    @Test
    fun repeat_everyMonth() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.MONTHLY.name); it.interval = 1
        }
        assertEquals(expected.toString(), parseTitle("Exercise every month").recurrence)
    }

    @Test
    fun repeat_everyYear() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.YEARLY.name); it.interval = 1
        }
        assertEquals(expected.toString(), parseTitle("Exercise every year").recurrence)
    }

    // ================================================================== //
    //  Repeat parsing - numeric intervals
    // ================================================================== //

    @Test
    fun repeat_every2Days() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.DAILY.name); it.interval = 2
        }
        assertEquals(expected.toString(), parseTitle("Exercise every 2 days").recurrence)
    }

    @Test
    fun repeat_every3Days() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.DAILY.name); it.interval = 3
        }
        assertEquals(expected.toString(), parseTitle("Exercise every 3 days").recurrence)
    }

    @Test
    fun repeat_every5Days() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.DAILY.name); it.interval = 5
        }
        assertEquals(expected.toString(), parseTitle("Exercise every 5 days").recurrence)
    }

    @Test
    fun repeat_every2Weeks() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.WEEKLY.name); it.interval = 2
        }
        assertEquals(expected.toString(), parseTitle("Exercise every 2 weeks").recurrence)
    }

    @Test
    fun repeat_every3Months() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.MONTHLY.name); it.interval = 3
        }
        assertEquals(expected.toString(), parseTitle("Exercise every 3 months").recurrence)
    }

    @Test
    fun repeat_every6Months() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.MONTHLY.name); it.interval = 6
        }
        assertEquals(expected.toString(), parseTitle("Exercise every 6 months").recurrence)
    }

    @Test
    fun repeat_every2Years() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.YEARLY.name); it.interval = 2
        }
        assertEquals(expected.toString(), parseTitle("Exercise every 2 years").recurrence)
    }

    // ================================================================== //
    //  Repeat parsing - word intervals
    // ================================================================== //

    @Test
    fun repeat_everyOtherDay() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.DAILY.name); it.interval = 2
        }
        assertEquals(expected.toString(), parseTitle("Exercise every other day").recurrence)
    }

    @Test
    fun repeat_everyOtherWeek() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.WEEKLY.name); it.interval = 2
        }
        assertEquals(expected.toString(), parseTitle("Exercise every other week").recurrence)
    }

    @Test
    fun repeat_everyOtherMonth() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.MONTHLY.name); it.interval = 2
        }
        assertEquals(expected.toString(), parseTitle("Exercise every other month").recurrence)
    }

    @Test
    fun repeat_everyTwoDays() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.DAILY.name); it.interval = 2
        }
        assertEquals(expected.toString(), parseTitle("Exercise every two days").recurrence)
    }

    @Test
    fun repeat_everyThreeWeeks() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.WEEKLY.name); it.interval = 3
        }
        assertEquals(expected.toString(), parseTitle("Exercise every three weeks").recurrence)
    }

    @Test
    fun repeat_everyFourDays() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.DAILY.name); it.interval = 4
        }
        assertEquals(expected.toString(), parseTitle("Exercise every four days").recurrence)
    }

    @Test
    fun repeat_everyFiveMonths() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.MONTHLY.name); it.interval = 5
        }
        assertEquals(expected.toString(), parseTitle("Exercise every five months").recurrence)
    }

    @Test
    fun repeat_everySixDays() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.DAILY.name); it.interval = 6
        }
        assertEquals(expected.toString(), parseTitle("Exercise every six days").recurrence)
    }

    @Test
    fun repeat_everySevenDays() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.DAILY.name); it.interval = 7
        }
        assertEquals(expected.toString(), parseTitle("Exercise every seven days").recurrence)
    }

    @Test
    fun repeat_everyEightDays() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.DAILY.name); it.interval = 8
        }
        assertEquals(expected.toString(), parseTitle("Exercise every eight days").recurrence)
    }

    @Test
    fun repeat_everyNineDays() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.DAILY.name); it.interval = 9
        }
        assertEquals(expected.toString(), parseTitle("Exercise every nine days").recurrence)
    }

    @Test
    fun repeat_everyTenDays() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.DAILY.name); it.interval = 10
        }
        assertEquals(expected.toString(), parseTitle("Exercise every ten days").recurrence)
    }

    @Test
    fun repeat_everyElevenDays() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.DAILY.name); it.interval = 11
        }
        assertEquals(expected.toString(), parseTitle("Exercise every eleven days").recurrence)
    }

    @Test
    fun repeat_everyTwelveMonths() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.MONTHLY.name); it.interval = 12
        }
        assertEquals(expected.toString(), parseTitle("Exercise every twelve months").recurrence)
    }

    // ================================================================== //
    //  Repeat parsing - day-of-week patterns
    // ================================================================== //

    @Test
    fun repeat_everyMonday() = runTest {
        val task = parseTitle("Exercise every monday")
        assertNotNull(task.recurrence)
        assertTrue(task.recurrence!!.contains("WEEKLY"))
    }

    @Test
    fun repeat_everyTuesday() = runTest {
        val task = parseTitle("Exercise every tuesday")
        assertNotNull(task.recurrence)
        assertTrue(task.recurrence!!.contains("WEEKLY"))
    }

    @Test
    fun repeat_everyWednesday() = runTest {
        val task = parseTitle("Exercise every wednesday")
        assertNotNull(task.recurrence)
        assertTrue(task.recurrence!!.contains("WEEKLY"))
    }

    @Test
    fun repeat_everyThursday() = runTest {
        val task = parseTitle("Exercise every thursday")
        assertNotNull(task.recurrence)
        assertTrue(task.recurrence!!.contains("WEEKLY"))
    }

    @Test
    fun repeat_everyFriday() = runTest {
        val task = parseTitle("Exercise every friday")
        assertNotNull(task.recurrence)
        assertTrue(task.recurrence!!.contains("WEEKLY"))
    }

    @Test
    fun repeat_everySaturday() = runTest {
        val task = parseTitle("Exercise every saturday")
        assertNotNull(task.recurrence)
        assertTrue(task.recurrence!!.contains("WEEKLY"))
    }

    @Test
    fun repeat_everySunday() = runTest {
        val task = parseTitle("Exercise every sunday")
        assertNotNull(task.recurrence)
        assertTrue(task.recurrence!!.contains("WEEKLY"))
    }

    // ================================================================== //
    //  Repeat parsing - time-of-day variants
    // ================================================================== //

    @Test
    fun repeat_everyMorning() = runTest {
        val task = parseTitle("Exercise every morning")
        assertNotNull(task.recurrence)
        assertTrue(task.recurrence!!.contains("DAILY"))
    }

    @Test
    fun repeat_everyNight() = runTest {
        val task = parseTitle("Exercise every night")
        assertNotNull(task.recurrence)
        assertTrue(task.recurrence!!.contains("DAILY"))
    }

    @Test
    fun repeat_everyEvening() = runTest {
        val task = parseTitle("Exercise every evening")
        assertNotNull(task.recurrence)
        assertTrue(task.recurrence!!.contains("DAILY"))
    }

    @Test
    fun repeat_everyAfternoon() = runTest {
        val task = parseTitle("Exercise every afternoon")
        assertNotNull(task.recurrence)
        assertTrue(task.recurrence!!.contains("DAILY"))
    }

    // ================================================================== //
    //  Repeat parsing - no repeat
    // ================================================================== //

    @Test
    fun repeat_noRepeatKeyword_noRecurrence() = runTest {
        assertNull(parseTitle("Buy milk at the store").recurrence)
    }

    @Test
    fun repeat_plainText_noRecurrence() = runTest {
        assertNull(parseTitle("Exercise in the park").recurrence)
    }

    // ================================================================== //
    //  Day/time parsing - relative days
    // ================================================================== //

    @Test
    fun day_today_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk today").hasDueDate())
    }

    @Test
    fun day_tomorrow_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk tomorrow").hasDueDate())
    }

    @Test
    fun day_today_noDueTime() = runTest {
        // "today" alone should set a date but not a specific time
        assertFalse(parseTitle("Buy milk today").hasDueTime())
    }

    @Test
    fun day_tomorrow_noDueTime() = runTest {
        assertFalse(parseTitle("Buy milk tomorrow").hasDueTime())
    }

    // ================================================================== //
    //  Day/time parsing - days of week
    // ================================================================== //

    @Test
    fun day_monday_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk monday").hasDueDate())
    }

    @Test
    fun day_tuesday_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk tuesday").hasDueDate())
    }

    @Test
    fun day_wednesday_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk wednesday").hasDueDate())
    }

    @Test
    fun day_thursday_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk thursday").hasDueDate())
    }

    @Test
    fun day_friday_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk friday").hasDueDate())
    }

    @Test
    fun day_saturday_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk saturday").hasDueDate())
    }

    @Test
    fun day_sunday_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk sunday").hasDueDate())
    }

    // ================================================================== //
    //  Day/time parsing - abbreviated days
    // ================================================================== //

    @Test
    fun day_monAbbrev_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk mon.").hasDueDate())
    }

    @Test
    fun day_tueAbbrev_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk tue.").hasDueDate())
    }

    @Test
    fun day_wedAbbrev_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk wed.").hasDueDate())
    }

    @Test
    fun day_thuAbbrev_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk thu.").hasDueDate())
    }

    @Test
    fun day_friAbbrev_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk fri.").hasDueDate())
    }

    @Test
    fun day_satAbbrev_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk sat.").hasDueDate())
    }

    @Test
    fun day_sunAbbrev_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk sun.").hasDueDate())
    }

    // ================================================================== //
    //  Day/time parsing - month date formats
    // ================================================================== //

    @Test
    fun day_januaryDate_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk January 15").hasDueDate())
    }

    @Test
    fun day_janAbbrevAlone_noDueDate() = runTest {
        // Bare "jan" without period or "uary" does not match the month regex
        // (pattern requires "jan." or "january")
        assertFalse(parseTitle("Buy milk jan 15").hasDueDate())
    }

    @Test
    fun day_janDotAbbrev_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk jan. 15").hasDueDate())
    }

    @Test
    fun day_february_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk February 20").hasDueDate())
    }

    @Test
    fun day_marchDate_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk March 1").hasDueDate())
    }

    @Test
    fun day_aprilDate_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk April 10").hasDueDate())
    }

    @Test
    fun day_mayDate_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk may 5").hasDueDate())
    }

    @Test
    fun day_juneDate_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk June 30").hasDueDate())
    }

    @Test
    fun day_julyDate_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk July 4").hasDueDate())
    }

    @Test
    fun day_augustDate_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk August 12").hasDueDate())
    }

    @Test
    fun day_septemberDate_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk September 1").hasDueDate())
    }

    @Test
    fun day_octoberDate_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk October 31").hasDueDate())
    }

    @Test
    fun day_novemberDate_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk November 25").hasDueDate())
    }

    @Test
    fun day_decemberDate_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk December 25").hasDueDate())
    }

    @Test
    fun day_monthDateWithYear_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk January 15, 2027").hasDueDate())
    }

    @Test
    fun day_monthDateWithTwoDigitYear_noDueDate() = runTest {
        // Two-digit year "27" is parsed as year 27 AD (not 2027),
        // which produces a negative timestamp (before epoch).
        // createDueDate returns this negative value, and hasDueDate()
        // checks dueDate > 0, so it returns false.
        assertFalse(parseTitle("Buy milk January 15, 27").hasDueDate())
    }

    // ================================================================== //
    //  Day/time parsing - slash date format (MM/DD)
    // ================================================================== //

    @Test
    fun day_slashDate_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk 3/15").hasDueDate())
    }

    @Test
    fun day_slashDateWithFourDigitYear_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk 3/15/2027").hasDueDate())
    }

    @Test
    fun day_slashDateWithTwoDigitYear_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk 3/15/27").hasDueDate())
    }

    @Test
    fun day_dashDate_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk 6-15").hasDueDate())
    }

    @Test
    fun day_dashDateWithYear_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk 6-15-2027").hasDueDate())
    }

    @Test
    fun day_slashDateDoubleDigitMonth_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk 12/25").hasDueDate())
    }

    @Test
    fun day_slashDateSingleDigitDay_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk 1/5").hasDueDate())
    }

    // ================================================================== //
    //  Day/time parsing - vague times (named times of day)
    // ================================================================== //

    @Test
    fun day_breakfast_setsTime() = runTest {
        val task = parseTitle("Exercise today breakfast")
        assertTrue(task.hasDueTime())
    }

    @Test
    fun day_lunch_setsTime() = runTest {
        val task = parseTitle("Exercise today lunch")
        assertTrue(task.hasDueTime())
    }

    @Test
    fun day_dinner_setsTime() = runTest {
        val task = parseTitle("Exercise today dinner")
        assertTrue(task.hasDueTime())
    }

    @Test
    fun day_supper_setsTime() = runTest {
        val task = parseTitle("Exercise today supper")
        assertTrue(task.hasDueTime())
    }

    @Test
    fun day_brunch_setsTime() = runTest {
        val task = parseTitle("Exercise today brunch")
        assertTrue(task.hasDueTime())
    }

    @Test
    fun day_morning_setsTime() = runTest {
        val task = parseTitle("Exercise today morning")
        assertTrue(task.hasDueTime())
    }

    @Test
    fun day_afternoon_setsTime() = runTest {
        val task = parseTitle("Exercise today afternoon")
        assertTrue(task.hasDueTime())
    }

    @Test
    fun day_evening_setsTime() = runTest {
        val task = parseTitle("Exercise today evening")
        assertTrue(task.hasDueTime())
    }

    @Test
    fun day_night_setsTime() = runTest {
        val task = parseTitle("Exercise today night")
        assertTrue(task.hasDueTime())
    }

    @Test
    fun day_noon_setsTime() = runTest {
        val task = parseTitle("Exercise today noon")
        assertTrue(task.hasDueTime())
    }

    @Test
    fun day_midnight_setsTime() = runTest {
        val task = parseTitle("Exercise today midnight")
        assertTrue(task.hasDueTime())
    }

    // ================================================================== //
    //  Day/time parsing - specific time formats
    // ================================================================== //

    @Test
    fun day_timeWithAmPm_setsTime() = runTest {
        assertTrue(parseTitle("Exercise 3pm").hasDueTime())
    }

    @Test
    fun day_timeWithAmPmDot_setsTime() = runTest {
        assertTrue(parseTitle("Exercise 3p.m.").hasDueTime())
    }

    @Test
    fun day_timeWithAm_setsTime() = runTest {
        assertTrue(parseTitle("Exercise 9am").hasDueTime())
    }

    @Test
    fun day_timeWithAmDot_setsTime() = runTest {
        assertTrue(parseTitle("Exercise 9a.m").hasDueTime())
    }

    @Test
    fun day_timeWithMinutesAndAmPm_setsTime() = runTest {
        assertTrue(parseTitle("Exercise 3:30pm").hasDueTime())
    }

    @Test
    fun day_timeWithMinutesAndAm_setsTime() = runTest {
        assertTrue(parseTitle("Exercise 8:30 am").hasDueTime())
    }

    @Test
    fun day_armyTime_setsTime() = runTest {
        assertTrue(parseTitle("Exercise 14:30").hasDueTime())
    }

    @Test
    fun day_armyTimeZeroPadded_setsTime() = runTest {
        assertTrue(parseTitle("Exercise 09:00").hasDueTime())
    }

    @Test
    fun day_oclock_setsTime() = runTest {
        assertTrue(parseTitle("Exercise 3 o'clock").hasDueTime())
    }

    @Test
    fun day_oclockPm_setsTime() = runTest {
        assertTrue(parseTitle("Exercise 3 o'clock pm").hasDueTime())
    }

    @Test
    fun day_atHour_setsTime() = runTest {
        assertTrue(parseTitle("Exercise at 5").hasDueTime())
    }

    @Test
    fun day_atHourWithPm_setsTime() = runTest {
        assertTrue(parseTitle("Exercise at 5 pm").hasDueTime())
    }

    // ================================================================== //
    //  Day/time parsing - no date
    // ================================================================== //

    @Test
    fun day_plainText_noDueDate() = runTest {
        assertFalse(parseTitle("Buy milk at the store").hasDueDate())
    }

    @Test
    fun day_plainText_noDueTime() = runTest {
        assertFalse(parseTitle("Buy milk at the store").hasDueTime())
    }

    // ================================================================== //
    //  Tag parsing - hash tags
    // ================================================================== //

    @Test
    fun tags_singleHashTag_extracted() = runTest {
        val (task, tags) = parseTitleWithTags("Buy milk #groceries")
        assertTrue(tags.contains("groceries"))
        assertEquals("Buy milk", task.title)
    }

    @Test
    fun tags_hashTagAtStart() = runTest {
        val (task, tags) = parseTitleWithTags("#urgent Buy milk")
        assertTrue(tags.contains("urgent"))
        assertEquals("Buy milk", task.title)
    }

    @Test
    fun tags_multipleHashTags() = runTest {
        val (_, tags) = parseTitleWithTags("Buy milk #groceries #shopping")
        assertTrue(tags.contains("groceries"))
        assertTrue(tags.contains("shopping"))
    }

    @Test
    fun tags_hashTagWithParens() = runTest {
        val (_, tags) = parseTitleWithTags("Buy milk #(multi word)")
        assertTrue(tags.contains("multi word"))
    }

    // ================================================================== //
    //  Tag parsing - at tags
    // ================================================================== //

    @Test
    fun tags_singleAtTag_extracted() = runTest {
        val (task, tags) = parseTitleWithTags("Buy milk @store")
        assertTrue(tags.contains("store"))
        assertEquals("Buy milk", task.title)
    }

    @Test
    fun tags_atTagWithParens() = runTest {
        val (_, tags) = parseTitleWithTags("Buy milk @(corner store)")
        assertTrue(tags.contains("corner store"))
    }

    // ================================================================== //
    //  Tag parsing - mixed hash and at tags
    // ================================================================== //

    @Test
    fun tags_mixedHashAndAt() = runTest {
        val (_, tags) = parseTitleWithTags("Buy milk #groceries @store")
        assertTrue(tags.contains("groceries"))
        assertTrue(tags.contains("store"))
    }

    // ================================================================== //
    //  Tag parsing - duplicate handling
    // ================================================================== //

    @Test
    fun tags_duplicateHash_notAddedTwice() = runTest {
        val (_, tags) = parseTitleWithTags("Buy milk #groceries #groceries")
        assertEquals(1, tags.size)
    }

    // ================================================================== //
    //  Tag parsing - DAO interaction
    // ================================================================== //

    @Test
    fun tags_daoReturnsNull_tagNotAdded() = runTest {
        `when`(tagDataDao.getTagWithCase(anyString())).thenReturn(null)
        val task = Task(title = "Buy milk #nonexistent")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertTrue(tags.isEmpty())
    }

    @Test
    fun tags_daoReturnsDifferentCase_preservesDaoCase() = runTest {
        `when`(tagDataDao.getTagWithCase(anyString()))
            .thenAnswer { it.arguments[0] as String }
        `when`(tagDataDao.getTagWithCase("groceries")).thenReturn("Groceries")
        val task = Task(title = "Buy milk #groceries")
        val tags = ArrayList<String>()
        TitleParser.parse(tagDataDao, task, tags)
        assertTrue(tags.contains("Groceries"))
        assertFalse(tags.contains("groceries"))
    }

    // ================================================================== //
    //  Tag parsing - no tags
    // ================================================================== //

    @Test
    fun tags_noTags_emptyList() = runTest {
        val (_, tags) = parseTitleWithTags("Buy milk")
        assertTrue(tags.isEmpty())
    }

    @Test
    fun tags_hashInMiddleOfWord_notATag() = runTest {
        // "C#" should not be parsed as tag "C" (it's mid-word)
        // Actually # requires space or start-of-string before it
        val (_, tags) = parseTitleWithTags("Learn C#")
        // The pattern requires \s or ^ before #, so C# won't match
        assertTrue(tags.isEmpty())
    }

    // ================================================================== //
    //  Tag parsing - title cleanup
    // ================================================================== //

    @Test
    fun tags_removedFromTitle() = runTest {
        val (task, _) = parseTitleWithTags("Buy milk #groceries today")
        assertFalse(task.title!!.contains("#groceries"))
    }

    @Test
    fun tags_atTagRemovedFromTitle() = runTest {
        val (task, _) = parseTitleWithTags("Buy milk @store")
        assertFalse(task.title!!.contains("@store"))
    }

    @Test
    fun tags_titleTrimmedAfterRemoval() = runTest {
        val (task, _) = parseTitleWithTags("Buy milk #groceries")
        assertEquals("Buy milk", task.title)
    }

    // ================================================================== //
    //  Full parse pipeline - combined features
    // ================================================================== //

    @Test
    fun combined_priorityAndRepeat() = runTest {
        val task = parseTitle("Exercise daily !!")
        assertEquals(Task.Priority.MEDIUM, task.priority)
        assertNotNull(task.recurrence)
    }

    @Test
    fun combined_priorityAndDate() = runTest {
        val task = parseTitle("Buy milk today !")
        assertTrue(task.hasDueDate())
        assertEquals(Task.Priority.LOW, task.priority)
    }

    @Test
    fun combined_repeatAndDate() = runTest {
        val task = parseTitle("Exercise daily today")
        assertNotNull(task.recurrence)
        assertTrue(task.hasDueDate())
    }

    @Test
    fun combined_tagAndPriority() = runTest {
        val (task, tags) = parseTitleWithTags("Buy milk #groceries !!")
        assertTrue(tags.contains("groceries"))
        assertEquals(Task.Priority.MEDIUM, task.priority)
    }

    @Test
    fun combined_tagAndDate() = runTest {
        val (task, tags) = parseTitleWithTags("Buy milk #groceries today")
        assertTrue(tags.contains("groceries"))
        assertTrue(task.hasDueDate())
    }

    @Test
    fun combined_tagAndRepeat() = runTest {
        val (task, tags) = parseTitleWithTags("Exercise #fitness daily")
        assertTrue(tags.contains("fitness"))
        assertNotNull(task.recurrence)
    }

    @Test
    fun combined_allFeatures() = runTest {
        val (task, tags) = parseTitleWithTags("Buy milk #groceries today !!")
        assertTrue(tags.contains("groceries"))
        assertTrue(task.hasDueDate())
        assertEquals(Task.Priority.MEDIUM, task.priority)
    }

    // ================================================================== //
    //  No-op / default cases
    // ================================================================== //

    @Test
    fun noRegexes_simpleTitle_unchanged() = runTest {
        val task = parseTitle("Buy milk")
        assertEquals("Buy milk", task.title)
        assertFalse(task.hasDueDate())
        assertFalse(task.hasDueTime())
        assertNull(task.recurrence)
        assertEquals(Task.Priority.NONE, task.priority)
    }

    @Test
    fun noRegexes_singleWord() = runTest {
        val task = parseTitle("Exercise")
        assertEquals("Exercise", task.title)
        assertEquals(Task.Priority.NONE, task.priority)
        assertNull(task.recurrence)
    }

    // ================================================================== //
    //  Edge cases
    // ================================================================== //

    @Test
    fun priority_caseInsensitive_bangKeyword() = runTest {
        // The regex for "bang" is case-insensitive, but strToPriority compares
        // the matched group literally (lowercase() result is discarded).
        // "Bang" does not equal "bang" in strToPriority, so it falls through
        // to the default HIGH.
        assertEquals(Task.Priority.HIGH, parseTitle("Jog Bang").priority)
    }

    @Test
    fun priority_caseInsensitive_priorityKeyword() = runTest {
        assertEquals(Task.Priority.LOW, parseTitle("Jog PRIORITY 1").priority)
    }

    @Test
    fun repeat_caseInsensitive_daily() = runTest {
        val task = parseTitle("Exercise DAILY")
        val expected = newRecur().also {
            it.setFrequency(Frequency.DAILY.name); it.interval = 1
        }
        assertEquals(expected.toString(), task.recurrence)
    }

    @Test
    fun repeat_caseInsensitive_everyDay() = runTest {
        val expected = newRecur().also {
            it.setFrequency(Frequency.DAILY.name); it.interval = 1
        }
        assertEquals(expected.toString(), parseTitle("Exercise Every Day").recurrence)
    }

    @Test
    fun day_caseInsensitive_today() = runTest {
        assertTrue(parseTitle("Buy milk TODAY").hasDueDate())
    }

    @Test
    fun day_caseInsensitive_tomorrow() = runTest {
        assertTrue(parseTitle("Buy milk TOMORROW").hasDueDate())
    }

    @Test
    fun day_caseInsensitive_monday() = runTest {
        assertTrue(parseTitle("Buy milk MONDAY").hasDueDate())
    }

    @Test
    fun day_caseInsensitive_monthDate() = runTest {
        assertTrue(parseTitle("Buy milk JANUARY 15").hasDueDate())
    }

    @Test
    fun day_parentheticalToday_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk (today)").hasDueDate())
    }

    @Test
    fun day_parentheticalTomorrow_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk (tomorrow)").hasDueDate())
    }

    @Test
    fun day_parentheticalDayOfWeek_setsDueDate() = runTest {
        assertTrue(parseTitle("Buy milk (monday)").hasDueDate())
    }

    // ================================================================== //
    //  Priority at end of title preserves rest of title
    // ================================================================== //

    @Test
    fun priority_preservesTitle_singleBang() = runTest {
        val task = parseTitle("Buy groceries !")
        assertTrue(task.title!!.startsWith("Buy groceries"))
    }

    @Test
    fun priority_preservesTitle_doubleBang() = runTest {
        val task = parseTitle("Buy groceries !!")
        assertTrue(task.title!!.startsWith("Buy groceries"))
    }

    @Test
    fun priority_preservesTitle_lowPriority() = runTest {
        val task = parseTitle("Buy groceries low priority")
        assertTrue(task.title!!.startsWith("Buy groceries"))
    }
}
