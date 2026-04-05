package com.todoroo.astrid.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.Freeze
import org.tasks.TestUtilities.withTZ
import org.tasks.time.DateTime
import org.tasks.time.ONE_DAY

/**
 * Tests targeting PIT mutation survivors in PermaSql.
 * Verifies that each placeholder produces distinct values,
 * that offsets between placeholders are exactly ONE_DAY multiples,
 * and that replacePlaceholdersForNewTask uses noon() not endOfDay().
 */
class PermaSqlMutationTest {

    // =====================================================================
    // Each EOD placeholder produces a DIFFERENT value from the others
    // =====================================================================

    @Test
    fun eodYesterdayIsDifferentFromEodToday() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 12, 0, 0, 0, DateTime.UTC)) {
                val yesterday = parseEodValue(PermaSql.VALUE_EOD_YESTERDAY)
                val today = parseEodValue(PermaSql.VALUE_EOD)
                assertNotEquals(yesterday, today)
                assertTrue(yesterday < today)
            }
        }
    }

    @Test
    fun eodTodayIsDifferentFromEodTomorrow() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 12, 0, 0, 0, DateTime.UTC)) {
                val today = parseEodValue(PermaSql.VALUE_EOD)
                val tomorrow = parseEodValue(PermaSql.VALUE_EOD_TOMORROW)
                assertNotEquals(today, tomorrow)
                assertTrue(today < tomorrow)
            }
        }
    }

    @Test
    fun eodTomorrowIsDifferentFromEodDayAfter() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 12, 0, 0, 0, DateTime.UTC)) {
                val tomorrow = parseEodValue(PermaSql.VALUE_EOD_TOMORROW)
                val dayAfter = parseEodValue(PermaSql.VALUE_EOD_DAY_AFTER)
                assertNotEquals(tomorrow, dayAfter)
                assertTrue(tomorrow < dayAfter)
            }
        }
    }

    @Test
    fun eodDayAfterIsDifferentFromEodNextWeek() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 12, 0, 0, 0, DateTime.UTC)) {
                val dayAfter = parseEodValue(PermaSql.VALUE_EOD_DAY_AFTER)
                val nextWeek = parseEodValue(PermaSql.VALUE_EOD_NEXT_WEEK)
                assertNotEquals(dayAfter, nextWeek)
                assertTrue(dayAfter < nextWeek)
            }
        }
    }

    @Test
    fun eodNextWeekIsDifferentFromEodNextMonth() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 12, 0, 0, 0, DateTime.UTC)) {
                val nextWeek = parseEodValue(PermaSql.VALUE_EOD_NEXT_WEEK)
                val nextMonth = parseEodValue(PermaSql.VALUE_EOD_NEXT_MONTH)
                assertNotEquals(nextWeek, nextMonth)
                assertTrue(nextWeek < nextMonth)
            }
        }
    }

    // =====================================================================
    // EOD offsets are exactly ONE_DAY multiples apart
    // =====================================================================

    @Test
    fun eodYesterdayIsOneDayBeforeToday() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 12, 0, 0, 0, DateTime.UTC)) {
                val yesterday = parseEodValue(PermaSql.VALUE_EOD_YESTERDAY)
                val today = parseEodValue(PermaSql.VALUE_EOD)
                assertEquals(ONE_DAY, today - yesterday)
            }
        }
    }

    @Test
    fun eodTomorrowIsOneDayAfterToday() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 12, 0, 0, 0, DateTime.UTC)) {
                val today = parseEodValue(PermaSql.VALUE_EOD)
                val tomorrow = parseEodValue(PermaSql.VALUE_EOD_TOMORROW)
                assertEquals(ONE_DAY, tomorrow - today)
            }
        }
    }

    @Test
    fun eodDayAfterIsTwoDaysAfterToday() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 12, 0, 0, 0, DateTime.UTC)) {
                val today = parseEodValue(PermaSql.VALUE_EOD)
                val dayAfter = parseEodValue(PermaSql.VALUE_EOD_DAY_AFTER)
                assertEquals(2 * ONE_DAY, dayAfter - today)
            }
        }
    }

    @Test
    fun eodNextWeekIsSevenDaysAfterToday() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 12, 0, 0, 0, DateTime.UTC)) {
                val today = parseEodValue(PermaSql.VALUE_EOD)
                val nextWeek = parseEodValue(PermaSql.VALUE_EOD_NEXT_WEEK)
                assertEquals(7 * ONE_DAY, nextWeek - today)
            }
        }
    }

    @Test
    fun eodNextMonthIsThirtyDaysAfterToday() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 12, 0, 0, 0, DateTime.UTC)) {
                val today = parseEodValue(PermaSql.VALUE_EOD)
                val nextMonth = parseEodValue(PermaSql.VALUE_EOD_NEXT_MONTH)
                assertEquals(30 * ONE_DAY, nextMonth - today)
            }
        }
    }

    // =====================================================================
    // Each NOON placeholder produces DIFFERENT values from each other
    // =====================================================================

    @Test
    fun noonTodayIsDifferentFromNoonTomorrow() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 8, 0, 0, 0, DateTime.UTC)) {
                val today = parseNoonValue(PermaSql.VALUE_NOON)
                val tomorrow = parseNoonValue("NOONT()")
                assertNotEquals(today, tomorrow)
                assertTrue(today < tomorrow)
            }
        }
    }

    @Test
    fun noonYesterdayIsDifferentFromNoonToday() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 8, 0, 0, 0, DateTime.UTC)) {
                val yesterday = parseNoonValue("NOONY()")
                val today = parseNoonValue(PermaSql.VALUE_NOON)
                assertNotEquals(yesterday, today)
                assertTrue(yesterday < today)
            }
        }
    }

    @Test
    fun noonYesterdayIsOneDayBeforeNoonToday() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 8, 0, 0, 0, DateTime.UTC)) {
                val yesterday = parseNoonValue("NOONY()")
                val today = parseNoonValue(PermaSql.VALUE_NOON)
                assertEquals(ONE_DAY, today - yesterday)
            }
        }
    }

    @Test
    fun noonTomorrowIsOneDayAfterNoonToday() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 8, 0, 0, 0, DateTime.UTC)) {
                val today = parseNoonValue(PermaSql.VALUE_NOON)
                val tomorrow = parseNoonValue("NOONT()")
                assertEquals(ONE_DAY, tomorrow - today)
            }
        }
    }

    @Test
    fun noonDayAfterIsTwoDaysAfterNoonToday() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 8, 0, 0, 0, DateTime.UTC)) {
                val today = parseNoonValue(PermaSql.VALUE_NOON)
                val dayAfter = parseNoonValue("NOONTT()")
                assertEquals(2 * ONE_DAY, dayAfter - today)
            }
        }
    }

    @Test
    fun noonNextWeekIsSevenDaysAfterNoonToday() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 8, 0, 0, 0, DateTime.UTC)) {
                val today = parseNoonValue(PermaSql.VALUE_NOON)
                val nextWeek = parseNoonValue("NOONW()")
                assertEquals(7 * ONE_DAY, nextWeek - today)
            }
        }
    }

    @Test
    fun noonNextMonthIsThirtyDaysAfterNoonToday() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 8, 0, 0, 0, DateTime.UTC)) {
                val today = parseNoonValue(PermaSql.VALUE_NOON)
                val nextMonth = parseNoonValue("NOONM()")
                assertEquals(30 * ONE_DAY, nextMonth - today)
            }
        }
    }

    // =====================================================================
    // Noon values are at 12:00, not at end of day
    // =====================================================================

    @Test
    fun noonValueIsAtTwelveOClock() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 8, 0, 0, 0, DateTime.UTC)) {
                val noonTs = parseNoonValue(PermaSql.VALUE_NOON)
                assertEquals(DateTime(2023, 6, 15, 12, 0, 0, 0, DateTime.UTC).millis, noonTs)
            }
        }
    }

    @Test
    fun noonValueIsDifferentFromEodValue() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 8, 0, 0, 0, DateTime.UTC)) {
                val noonTs = parseNoonValue(PermaSql.VALUE_NOON)
                val eodTs = parseEodValue(PermaSql.VALUE_EOD)
                assertNotEquals(noonTs, eodTs)
                assertTrue("Noon should be before end of day", noonTs < eodTs)
            }
        }
    }

    // =====================================================================
    // replacePlaceholdersForNewTask uses noon() not endOfDay() for EOD
    // =====================================================================

    @Test
    fun newTaskEodUsesNoonNotEndOfDay() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 8, 0, 0, 0, DateTime.UTC)) {
                val newTaskResult = PermaSql.replacePlaceholdersForNewTask("x=${PermaSql.VALUE_EOD}")
                val queryResult = PermaSql.replacePlaceholdersForQuery("x=${PermaSql.VALUE_EOD}")
                val newTaskTs = newTaskResult.substringAfter("=").toLong()
                val queryTs = queryResult.substringAfter("=").toLong()
                // newTask should use noon (12:00), query should use endOfDay (23:59:59)
                assertNotEquals(newTaskTs, queryTs)
                assertTrue("New task EOD (noon) should be before query EOD (endOfDay)",
                    newTaskTs < queryTs)
                assertEquals(DateTime(2023, 6, 15, 12, 0, 0, 0, DateTime.UTC).millis, newTaskTs)
            }
        }
    }

    @Test
    fun newTaskEodTomorrowUsesNoonTomorrow() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 8, 0, 0, 0, DateTime.UTC)) {
                val result = PermaSql.replacePlaceholdersForNewTask("x=${PermaSql.VALUE_EOD_TOMORROW}")
                val ts = result.substringAfter("=").toLong()
                // Should be noon today + ONE_DAY
                val expectedNoonTomorrow = DateTime(2023, 6, 15, 12, 0, 0, 0, DateTime.UTC).millis + ONE_DAY
                assertEquals(expectedNoonTomorrow, ts)
            }
        }
    }

    @Test
    fun newTaskEodYesterdayUsesNoonYesterday() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 8, 0, 0, 0, DateTime.UTC)) {
                val result = PermaSql.replacePlaceholdersForNewTask("x=${PermaSql.VALUE_EOD_YESTERDAY}")
                val ts = result.substringAfter("=").toLong()
                // Should be noon today - ONE_DAY
                val expectedNoonYesterday = DateTime(2023, 6, 15, 12, 0, 0, 0, DateTime.UTC).millis - ONE_DAY
                assertEquals(expectedNoonYesterday, ts)
            }
        }
    }

    @Test
    fun newTaskNoonIsUnchangedFromQuery() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 8, 0, 0, 0, DateTime.UTC)) {
                // NOON placeholders should produce identical values in both methods
                val newTaskResult = PermaSql.replacePlaceholdersForNewTask("x=${PermaSql.VALUE_NOON}")
                val queryResult = PermaSql.replacePlaceholdersForQuery("x=${PermaSql.VALUE_NOON}")
                val newTaskTs = newTaskResult.substringAfter("=").toLong()
                val queryTs = queryResult.substringAfter("=").toLong()
                assertEquals(newTaskTs, queryTs)
            }
        }
    }

    @Test
    fun queryEodIsEndOfDayNot12() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 8, 0, 0, 0, DateTime.UTC)) {
                val result = PermaSql.replacePlaceholdersForQuery("x=${PermaSql.VALUE_EOD}")
                val ts = result.substringAfter("=").toLong()
                // Should be 23:59:59, NOT 12:00:00
                assertEquals(DateTime(2023, 6, 15, 23, 59, 59, 0, DateTime.UTC).millis, ts)
            }
        }
    }

    @Test
    fun newTaskEodNextWeekUsesNoonPlusSevenDays() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 8, 0, 0, 0, DateTime.UTC)) {
                val result = PermaSql.replacePlaceholdersForNewTask("x=${PermaSql.VALUE_EOD_NEXT_WEEK}")
                val ts = result.substringAfter("=").toLong()
                val expectedNoonNextWeek = DateTime(2023, 6, 15, 12, 0, 0, 0, DateTime.UTC).millis + 7 * ONE_DAY
                assertEquals(expectedNoonNextWeek, ts)
            }
        }
    }

    @Test
    fun newTaskEodNextMonthUsesNoonPlusThirtyDays() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 8, 0, 0, 0, DateTime.UTC)) {
                val result = PermaSql.replacePlaceholdersForNewTask("x=${PermaSql.VALUE_EOD_NEXT_MONTH}")
                val ts = result.substringAfter("=").toLong()
                val expectedNoonNextMonth = DateTime(2023, 6, 15, 12, 0, 0, 0, DateTime.UTC).millis + 30 * ONE_DAY
                assertEquals(expectedNoonNextMonth, ts)
            }
        }
    }

    @Test
    fun newTaskEodDayAfterUsesNoonPlusTwoDays() {
        withTZ("UTC") {
            Freeze.freezeAt(DateTime(2023, 6, 15, 8, 0, 0, 0, DateTime.UTC)) {
                val result = PermaSql.replacePlaceholdersForNewTask("x=${PermaSql.VALUE_EOD_DAY_AFTER}")
                val ts = result.substringAfter("=").toLong()
                val expectedNoonDayAfter = DateTime(2023, 6, 15, 12, 0, 0, 0, DateTime.UTC).millis + 2 * ONE_DAY
                assertEquals(expectedNoonDayAfter, ts)
            }
        }
    }

    // =====================================================================
    // Helper methods
    // =====================================================================

    private fun parseEodValue(placeholder: String): Long {
        val result = PermaSql.replacePlaceholdersForQuery("x=$placeholder")
        return result.substringAfter("=").toLong()
    }

    private fun parseNoonValue(placeholder: String): Long {
        val result = PermaSql.replacePlaceholdersForQuery("x=$placeholder")
        return result.substringAfter("=").toLong()
    }
}
