package org.tasks.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.Freeze
import org.tasks.TestUtilities.withTZ
import java.util.TimeZone

/**
 * Additional DateTime tests targeting PIT mutation survivors.
 * Covers constructor variants, arithmetic boundaries, time-of-day
 * methods, comparison operators, and UTC conversion edge cases.
 */
class DateTimeExtraTest {

    // =====================================================================
    // Constructor variants
    // =====================================================================

    @Test
    fun defaultConstructorUsesCurrentTime() {
        Freeze.freezeAt(DateTime(2024, 3, 15, 10, 30, 0, 0, DateTime.UTC)) {
            val dt = DateTime(timeZone = DateTime.UTC)
            assertEquals(DateTime(2024, 3, 15, 10, 30, 0, 0, DateTime.UTC).millis, dt.millis)
        }
    }

    @Test
    fun timestampConstructorPreservesMillis() {
        val millis = 1700000000000L
        val dt = DateTime(millis, DateTime.UTC)
        assertEquals(millis, dt.millis)
    }

    @Test
    fun fullComponentConstructorSetsAllFields() {
        val dt = DateTime(2023, 7, 20, 14, 35, 48, 123, DateTime.UTC)
        assertEquals(2023, dt.year)
        assertEquals(7, dt.monthOfYear)
        assertEquals(20, dt.dayOfMonth)
        assertEquals(14, dt.hourOfDay)
        assertEquals(35, dt.minuteOfHour)
        assertEquals(48, dt.secondOfMinute)
    }

    @Test
    fun constructorDefaultsHourMinuteSecondMillisToZero() {
        val dt = DateTime(2023, 7, 20, timeZone = DateTime.UTC)
        assertEquals(0, dt.hourOfDay)
        assertEquals(0, dt.minuteOfHour)
        assertEquals(0, dt.secondOfMinute)
    }

    // =====================================================================
    // startOfDay
    // =====================================================================

    @Test
    fun startOfDayClearsTimeComponents() {
        val dt = DateTime(2023, 6, 15, 14, 35, 48, 123, DateTime.UTC)
        val result = dt.startOfDay()
        assertEquals(0, result.hourOfDay)
        assertEquals(0, result.minuteOfHour)
        assertEquals(0, result.secondOfMinute)
        assertEquals(2023, result.year)
        assertEquals(6, result.monthOfYear)
        assertEquals(15, result.dayOfMonth)
    }

    @Test
    fun startOfDayAtMidnightIsIdempotent() {
        val dt = DateTime(2023, 6, 15, 0, 0, 0, 0, DateTime.UTC)
        assertEquals(dt, dt.startOfDay())
    }

    @Test
    fun startOfDayAtEndOfDayGoesToSameDay() {
        val dt = DateTime(2023, 6, 15, 23, 59, 59, 999, DateTime.UTC)
        val result = dt.startOfDay()
        assertEquals(15, result.dayOfMonth)
        assertEquals(0, result.hourOfDay)
    }

    // =====================================================================
    // startOfMinute
    // =====================================================================

    @Test
    fun startOfMinuteClearsSecondsAndMillis() {
        val dt = DateTime(2023, 6, 15, 14, 35, 48, 123, DateTime.UTC)
        val result = dt.startOfMinute()
        assertEquals(14, result.hourOfDay)
        assertEquals(35, result.minuteOfHour)
        assertEquals(0, result.secondOfMinute)
    }

    @Test
    fun startOfMinuteAtExactMinuteIsIdempotent() {
        val dt = DateTime(2023, 6, 15, 14, 35, 0, 0, DateTime.UTC)
        assertEquals(dt, dt.startOfMinute())
    }

    // =====================================================================
    // noon
    // =====================================================================

    @Test
    fun noonSetsHourTo12() {
        val dt = DateTime(2023, 6, 15, 8, 30, 45, 123, DateTime.UTC)
        val result = dt.noon()
        assertEquals(12, result.hourOfDay)
        assertEquals(0, result.minuteOfHour)
        assertEquals(0, result.secondOfMinute)
    }

    @Test
    fun noonPreservesDate() {
        val dt = DateTime(2023, 12, 31, 23, 59, 59, 999, DateTime.UTC)
        val result = dt.noon()
        assertEquals(2023, result.year)
        assertEquals(12, result.monthOfYear)
        assertEquals(31, result.dayOfMonth)
        assertEquals(12, result.hourOfDay)
    }

    @Test
    fun noonIsDifferentFromStartOfDay() {
        val dt = DateTime(2023, 6, 15, 0, 0, 0, 0, DateTime.UTC)
        assertNotEquals(dt.startOfDay().millis, dt.noon().millis)
    }

    @Test
    fun noonIsDifferentFromEndOfDay() {
        val dt = DateTime(2023, 6, 15, 0, 0, 0, 0, DateTime.UTC)
        assertNotEquals(dt.endOfDay().millis, dt.noon().millis)
    }

    // =====================================================================
    // endOfDay
    // =====================================================================

    @Test
    fun endOfDaySetsTimeTo235959() {
        val dt = DateTime(2023, 6, 15, 8, 0, 0, 0, DateTime.UTC)
        val result = dt.endOfDay()
        assertEquals(23, result.hourOfDay)
        assertEquals(59, result.minuteOfHour)
        assertEquals(59, result.secondOfMinute)
    }

    @Test
    fun endOfDayIsAfterNoon() {
        val dt = DateTime(2023, 6, 15, 0, 0, 0, 0, DateTime.UTC)
        assertTrue(dt.endOfDay().millis > dt.noon().millis)
    }

    // =====================================================================
    // plusDays / minusDays
    // =====================================================================

    @Test
    fun plusDaysIncrementsDay() {
        val dt = DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC)
        val result = dt.plusDays(1)
        assertEquals(16, result.dayOfMonth)
        assertEquals(10, result.hourOfDay) // time preserved
    }

    @Test
    fun plusDaysWrapsMonth() {
        val dt = DateTime(2023, 6, 30, 10, 0, 0, 0, DateTime.UTC)
        val result = dt.plusDays(1)
        assertEquals(7, result.monthOfYear)
        assertEquals(1, result.dayOfMonth)
    }

    @Test
    fun plusDaysWrapsYear() {
        val dt = DateTime(2023, 12, 31, 10, 0, 0, 0, DateTime.UTC)
        val result = dt.plusDays(1)
        assertEquals(2024, result.year)
        assertEquals(1, result.monthOfYear)
        assertEquals(1, result.dayOfMonth)
    }

    @Test
    fun minusDaysDecrementsDay() {
        val dt = DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC)
        val result = dt.minusDays(1)
        assertEquals(14, result.dayOfMonth)
    }

    @Test
    fun minusDaysWrapsMonth() {
        val dt = DateTime(2023, 7, 1, 10, 0, 0, 0, DateTime.UTC)
        val result = dt.minusDays(1)
        assertEquals(6, result.monthOfYear)
        assertEquals(30, result.dayOfMonth)
    }

    @Test
    fun plusDaysZeroReturnsEquivalent() {
        val dt = DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC)
        assertEquals(dt, dt.plusDays(0))
    }

    // =====================================================================
    // plusMonths
    // =====================================================================

    @Test
    fun plusMonthsIncrementsMonth() {
        val dt = DateTime(2023, 1, 15, 10, 0, 0, 0, DateTime.UTC)
        val result = dt.plusMonths(1)
        assertEquals(2, result.monthOfYear)
        assertEquals(15, result.dayOfMonth)
    }

    @Test
    fun plusMonthsWrapsYear() {
        val dt = DateTime(2023, 11, 15, 10, 0, 0, 0, DateTime.UTC)
        val result = dt.plusMonths(3)
        assertEquals(2024, result.year)
        assertEquals(2, result.monthOfYear)
    }

    @Test
    fun plusMonthsEndOfMonthClamp() {
        // Jan 31 + 1 month => Feb has fewer days, Calendar clamps
        val dt = DateTime(2023, 1, 31, 10, 0, 0, 0, DateTime.UTC)
        val result = dt.plusMonths(1)
        assertEquals(2, result.monthOfYear)
        // Feb 2023 has 28 days, so clamped to 28
        assertEquals(28, result.dayOfMonth)
    }

    // =====================================================================
    // withMillisOfDay
    // =====================================================================

    @Test
    fun withMillisOfDayZeroIsStartOfDay() {
        val dt = DateTime(2023, 6, 15, 14, 30, 0, 0, DateTime.UTC)
        val result = dt.withMillisOfDay(0)
        assertEquals(0, result.hourOfDay)
        assertEquals(0, result.minuteOfHour)
        assertEquals(0, result.secondOfMinute)
    }

    @Test
    fun withMillisOfDayNoon() {
        val dt = DateTime(2023, 6, 15, 0, 0, 0, 0, DateTime.UTC)
        val result = dt.withMillisOfDay(12 * 3600000)
        assertEquals(12, result.hourOfDay)
        assertEquals(0, result.minuteOfHour)
    }

    @Test
    fun withMillisOfDayMaxValue() {
        val dt = DateTime(2023, 6, 15, 0, 0, 0, 0, DateTime.UTC)
        val result = dt.withMillisOfDay(DateTime.MAX_MILLIS_PER_DAY)
        assertEquals(23, result.hourOfDay)
        assertEquals(59, result.minuteOfHour)
        assertEquals(59, result.secondOfMinute)
    }

    @Test
    fun withMillisOfDayPreservesDate() {
        val dt = DateTime(2023, 6, 15, 0, 0, 0, 0, DateTime.UTC)
        val result = dt.withMillisOfDay(5 * 3600000 + 30 * 60000)
        assertEquals(2023, result.year)
        assertEquals(6, result.monthOfYear)
        assertEquals(15, result.dayOfMonth)
        assertEquals(5, result.hourOfDay)
        assertEquals(30, result.minuteOfHour)
    }

    // =====================================================================
    // withHourOfDay / withMinuteOfHour
    // =====================================================================

    @Test
    fun withHourOfDayChangesOnlyHour() {
        val dt = DateTime(2023, 6, 15, 10, 30, 45, 0, DateTime.UTC)
        val result = dt.withHourOfDay(20)
        assertEquals(20, result.hourOfDay)
        assertEquals(30, result.minuteOfHour)
        assertEquals(45, result.secondOfMinute)
    }

    @Test
    fun withMinuteOfHourChangesOnlyMinute() {
        val dt = DateTime(2023, 6, 15, 10, 30, 45, 0, DateTime.UTC)
        val result = dt.withMinuteOfHour(0)
        assertEquals(10, result.hourOfDay)
        assertEquals(0, result.minuteOfHour)
        assertEquals(45, result.secondOfMinute)
    }

    // =====================================================================
    // toUTC conversion
    // =====================================================================

    @Test
    fun toUTCFromChicago() {
        withTZ("America/Chicago") {
            val local = DateTime(2023, 6, 15, 12, 0, 0) // CDT = UTC-5
            val utc = local.toUTC()
            assertEquals(DateTime(2023, 6, 15, 17, 0, 0, 0, DateTime.UTC), utc)
        }
    }

    @Test
    fun toUTCFromUTCIsIdempotent() {
        val utc = DateTime(2023, 6, 15, 12, 0, 0, 0, DateTime.UTC)
        val result = utc.toUTC()
        assertEquals(utc, result)
    }

    @Test
    fun toLocalFromUTC() {
        withTZ("America/Chicago") {
            val utc = DateTime(2023, 6, 15, 17, 0, 0, 0, DateTime.UTC)
            val local = utc.toLocal()
            assertEquals(12, local.hourOfDay)
        }
    }

    // =====================================================================
    // Comparison operators
    // =====================================================================

    @Test
    fun isAfterWithLaterTime() {
        val earlier = DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC)
        val later = DateTime(2023, 6, 15, 10, 0, 1, 0, DateTime.UTC)
        assertTrue(later.isAfter(earlier))
    }

    @Test
    fun isAfterWithEqualTimeReturnsFalse() {
        val dt1 = DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC)
        val dt2 = DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC)
        assertFalse(dt1.isAfter(dt2))
    }

    @Test
    fun isAfterWithEarlierTimeReturnsFalse() {
        val earlier = DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC)
        val later = DateTime(2023, 6, 15, 10, 0, 1, 0, DateTime.UTC)
        assertFalse(earlier.isAfter(later))
    }

    @Test
    fun isBeforeWithEarlierTime() {
        val earlier = DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC)
        val later = DateTime(2023, 6, 15, 10, 0, 1, 0, DateTime.UTC)
        assertTrue(earlier.isBefore(later))
    }

    @Test
    fun isBeforeWithEqualTimeReturnsFalse() {
        val dt1 = DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC)
        val dt2 = DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC)
        assertFalse(dt1.isBefore(dt2))
    }

    @Test
    fun isBeforeWithLaterTimeReturnsFalse() {
        val earlier = DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC)
        val later = DateTime(2023, 6, 15, 10, 0, 1, 0, DateTime.UTC)
        assertFalse(later.isBefore(earlier))
    }

    @Test
    fun isAfterNowWhenInFuture() {
        Freeze.freezeAt(DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC)) {
            assertTrue(DateTime(2023, 6, 15, 10, 0, 1, 0, DateTime.UTC).isAfterNow)
        }
    }

    @Test
    fun isAfterNowWhenEqualReturnsFalse() {
        Freeze.freezeAt(DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC)) {
            assertFalse(DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC).isAfterNow)
        }
    }

    @Test
    fun isAfterNowWhenInPastReturnsFalse() {
        Freeze.freezeAt(DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC)) {
            assertFalse(DateTime(2023, 6, 14, 10, 0, 0, 0, DateTime.UTC).isAfterNow)
        }
    }

    @Test
    fun isBeforeNowWhenInPast() {
        Freeze.freezeAt(DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC)) {
            assertTrue(DateTime(2023, 6, 15, 9, 59, 59, 0, DateTime.UTC).isBeforeNow)
        }
    }

    @Test
    fun isBeforeNowWhenEqualReturnsFalse() {
        Freeze.freezeAt(DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC)) {
            assertFalse(DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC).isBeforeNow)
        }
    }

    // =====================================================================
    // equals / hashCode
    // =====================================================================

    @Test
    fun equalDateTimesAreEqual() {
        val dt1 = DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC)
        val dt2 = DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC)
        assertEquals(dt1, dt2)
        assertEquals(dt1.hashCode(), dt2.hashCode())
    }

    @Test
    fun differentMillisAreNotEqual() {
        val dt1 = DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC)
        val dt2 = DateTime(2023, 6, 15, 10, 0, 1, 0, DateTime.UTC)
        assertNotEquals(dt1, dt2)
    }

    @Test
    fun differentTimezonesWithSameMillisAreNotEqual() {
        val millis = DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC).millis
        val utc = DateTime(millis, DateTime.UTC)
        val chicago = DateTime(millis, TimeZone.getTimeZone("America/Chicago"))
        assertNotEquals(utc, chicago)
    }

    @Test
    fun notEqualToNull() {
        val dt = DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC)
        assertFalse(dt.equals(null))
    }

    @Test
    fun notEqualToOtherType() {
        val dt = DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC)
        assertFalse(dt.equals("not a DateTime"))
    }

    // =====================================================================
    // toLocalDate / toLocalDateTime
    // =====================================================================

    @Test
    fun toLocalDateReturnsNullForZero() {
        val dt = DateTime(0L, DateTime.UTC)
        assertNull(dt.toLocalDate())
    }

    @Test
    fun toLocalDateTimeReturnsNullForZero() {
        val dt = DateTime(0L, DateTime.UTC)
        assertNull(dt.toLocalDateTime())
    }

    @Test
    fun toLocalDateReturnsValueForNonZero() {
        withTZ("UTC") {
            val dt = DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC)
            val ld = dt.toLocalDate()!!
            assertEquals(2023, ld.year)
            assertEquals(6, ld.monthValue)
            assertEquals(15, ld.dayOfMonth)
        }
    }

    @Test
    fun toLocalDateTimeReturnsValueForNonZero() {
        withTZ("UTC") {
            val dt = DateTime(2023, 6, 15, 10, 30, 0, 0, DateTime.UTC)
            val ldt = dt.toLocalDateTime()!!
            assertEquals(2023, ldt.year)
            assertEquals(6, ldt.monthValue)
            assertEquals(15, ldt.dayOfMonth)
            assertEquals(10, ldt.hour)
            assertEquals(30, ldt.minute)
        }
    }

    // =====================================================================
    // MAX_MILLIS_PER_DAY constant
    // =====================================================================

    @Test
    fun maxMillisPerDayIsOneLessThan24Hours() {
        assertEquals(24 * 60 * 60 * 1000 - 1, DateTime.MAX_MILLIS_PER_DAY)
    }

    // =====================================================================
    // millisOfDay property
    // =====================================================================

    @Test
    fun millisOfDayAtMidnightIsZero() {
        val dt = DateTime(2023, 6, 15, 0, 0, 0, 0, DateTime.UTC)
        assertEquals(0, dt.millisOfDay)
    }

    @Test
    fun millisOfDayAtNoonIs12Hours() {
        val dt = DateTime(2023, 6, 15, 12, 0, 0, 0, DateTime.UTC)
        assertEquals(12 * 3600000, dt.millisOfDay)
    }

    @Test
    fun millisOfDayIncludesAllComponents() {
        val dt = DateTime(2023, 6, 15, 2, 3, 4, 567, DateTime.UTC)
        val expected = 2 * 3600000 + 3 * 60000 + 4 * 1000 + 567
        assertEquals(expected, dt.millisOfDay)
    }

    // =====================================================================
    // plusSeconds / minusSeconds / plusMillis
    // =====================================================================

    @Test
    fun plusSecondsAddsSeconds() {
        val dt = DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC)
        val result = dt.plusSeconds(90)
        assertEquals(10, result.hourOfDay)
        assertEquals(1, result.minuteOfHour)
        assertEquals(30, result.secondOfMinute)
    }

    @Test
    fun minusSecondsSubtractsSeconds() {
        val dt = DateTime(2023, 6, 15, 10, 1, 30, 0, DateTime.UTC)
        val result = dt.minusSeconds(90)
        assertEquals(10, result.hourOfDay)
        assertEquals(0, result.minuteOfHour)
        assertEquals(0, result.secondOfMinute)
    }

    @Test
    fun plusMillisAddsMilliseconds() {
        val dt = DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC)
        val result = dt.plusMillis(1500)
        assertEquals(1, result.secondOfMinute)
    }

    // =====================================================================
    // offset property
    // =====================================================================

    @Test
    fun utcOffsetIsZero() {
        val dt = DateTime(2023, 6, 15, 10, 0, 0, 0, DateTime.UTC)
        assertEquals(0L, dt.offset)
    }

    @Test
    fun nonUtcOffsetIsNonZero() {
        val chicago = TimeZone.getTimeZone("America/Chicago")
        val dt = DateTime(2023, 6, 15, 10, 0, 0, 0, chicago)
        assertNotEquals(0L, dt.offset)
    }

    // =====================================================================
    // isLastDayOfMonth / numberOfDaysInMonth boundary
    // =====================================================================

    @Test
    fun leapYearFebHas29Days() {
        assertEquals(29, DateTime(2024, 2, 1, 0, 0, 0, 0, DateTime.UTC).numberOfDaysInMonth)
    }

    @Test
    fun nonLeapYearFebHas28Days() {
        assertEquals(28, DateTime(2023, 2, 1, 0, 0, 0, 0, DateTime.UTC).numberOfDaysInMonth)
    }

    @Test
    fun isLastDayOfMonthFeb29LeapYear() {
        assertTrue(DateTime(2024, 2, 29, 0, 0, 0, 0, DateTime.UTC).isLastDayOfMonth)
    }

    @Test
    fun isNotLastDayOfMonthFeb28LeapYear() {
        assertFalse(DateTime(2024, 2, 28, 0, 0, 0, 0, DateTime.UTC).isLastDayOfMonth)
    }
}
